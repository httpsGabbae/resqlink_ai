package com.resqlink.ai.safetyjourney

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.resqlink.ai.MainActivity
import com.resqlink.ai.data.local.HotlineDatabaseHelper
import com.resqlink.ai.data.location.LocationProvider
import com.resqlink.ai.data.telephony.SmsDispatcher
import com.resqlink.ai.emergency.EmergencyEventManager
import com.resqlink.ai.emergency.EmergencyTriggerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * # JourneyAlarmReceiver
 *
 * BroadcastReceiver responsible for waking the application when a Safety Journey deadline arrives.
 *
 * Ordinary Human Overview:
 * When you walk home at night and set a 20-minute safety timer, this receiver is the digital safety net:
 * 1. **At the Deadline**: It fires a loud, vibrating heads-up notification asking: "Are you safe?"
 * 2. **Notification Actions**: Directly from the lock screen, you can tap:
 *    - "I'M SAFE" -> Cancels alarms and completes the trip safely.
 *    - "+15 MINS" -> Extends your journey if stuck in traffic or a bus delay.
 *    - "EMERGENCY SOS" -> Immediately launches full emergency assistance.
 * 3. **Grace Period Expiration**: If you do not tap anything and the grace period runs out,
 *    it automatically retrieves your last known GPS coordinates and sends an urgent SMS
 *    to your designated family/trusted contacts.
 *
 * Safety-Critical Architecture Guarantee:
 * A missed check-in NEVER automatically calls 911 or public emergency dispatch.
 * It alerts trusted family contacts ONLY, preventing accidental false 911 dispatch.
 */
class JourneyAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECK_IN_REMINDER = "com.resqlink.ai.action.JOURNEY_CHECK_IN_REMINDER"
        const val ACTION_GRACE_EXPIRED = "com.resqlink.ai.action.JOURNEY_GRACE_EXPIRED"
        const val ACTION_CHECK_IN_SAFE = "com.resqlink.ai.action.JOURNEY_CHECK_IN_SAFE"
        const val ACTION_EXTEND_15M = "com.resqlink.ai.action.JOURNEY_EXTEND_15M"
        const val ACTION_EMERGENCY_SOS = "com.resqlink.ai.action.JOURNEY_EMERGENCY_SOS"

        const val EXTRA_JOURNEY_ID = "extra_journey_id"
        private const val JOURNEY_NOTIFICATION_ID = 9021
        private const val NOTIFICATION_CHANNEL_ID = "safety_journey_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Safety Journey Check-in"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val journeyRepository = SafetyJourneyRepository.getInstance(context)
        val journeyScheduler = JourneyScheduler(context)
        val currentActiveJourney = journeyRepository.activeJourney.value

        when (intent.action) {
            ACTION_CHECK_IN_REMINDER -> {
                if (currentActiveJourney != null && currentActiveJourney.status == JourneyStatus.ACTIVE) {
                    val updatedJourney = currentActiveJourney.copy(status = JourneyStatus.GRACE_PERIOD)
                    journeyRepository.saveActiveJourney(updatedJourney)

                    // Post high-priority check-in reminder on lock screen
                    displayCheckInNotification(context, updatedJourney)

                    // Program grace-period countdown alarm
                    journeyScheduler.scheduleGraceAlarm(updatedJourney)
                }
            }

            ACTION_GRACE_EXPIRED -> {
                if (currentActiveJourney != null && currentActiveJourney.status == JourneyStatus.GRACE_PERIOD) {
                    // Grace window elapsed without user response -> Escalate to trusted ICE contacts
                    cancelNotification(context)

                    val escalatedJourney = currentActiveJourney.copy(
                        status = JourneyStatus.ESCALATED_CONTACTS,
                        escalationReason = "Safety journey deadline exceeded without user check-in."
                    )
                    journeyRepository.archiveJourney(escalatedJourney)

                    escalateToTrustedContacts(context, escalatedJourney)
                }
            }

            ACTION_CHECK_IN_SAFE -> {
                cancelNotification(context)
                journeyScheduler.cancelAllAlarms()
                if (currentActiveJourney != null) {
                    val resolvedJourney = currentActiveJourney.copy(
                        status = JourneyStatus.RESOLVED_SAFE,
                        completedTimeMillis = System.currentTimeMillis()
                    )
                    journeyRepository.archiveJourney(resolvedJourney)
                }
            }

            ACTION_EXTEND_15M -> {
                cancelNotification(context)
                journeyScheduler.extendJourney(15)
            }

            ACTION_EMERGENCY_SOS -> {
                cancelNotification(context)
                journeyScheduler.cancelAllAlarms()
                EmergencyEventManager.getInstance().registerTrigger(
                    source = EmergencyTriggerSource.MANUAL_SOS_BUTTON,
                    detectedPhrase = "Safety Journey SOS button pressed"
                )
            }
        }
    }

    /**
     * Builds and displays a high-priority, ongoing notification with action buttons.
     */
    private fun displayCheckInNotification(context: Context, journey: SafetyJourney) {
        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders and urgent check-ins for active safety sessions"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            systemNotificationManager.createNotificationChannel(notificationChannel)
        }

        // Tap content intent -> opens MainActivity
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: I'm Safe
        val safePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, JourneyAlarmReceiver::class.java).apply {
                action = ACTION_CHECK_IN_SAFE
                putExtra(EXTRA_JOURNEY_ID, journey.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Extend 15 Minutes
        val extendPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, JourneyAlarmReceiver::class.java).apply {
                action = ACTION_EXTEND_15M
                putExtra(EXTRA_JOURNEY_ID, journey.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Trigger SOS
        val emergencyPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, JourneyAlarmReceiver::class.java).apply {
                action = ACTION_EMERGENCY_SOS
                putExtra(EXTRA_JOURNEY_ID, journey.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val checkInNotification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Are you safe? Check-in Overdue")
            .setContentText("Destination: ${journey.destination}. Tap 'I'm Safe' or journey will escalate in ${journey.gracePeriodMinutes} mins.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "I'M SAFE", safePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+15 MINS", extendPendingIntent)
            .addAction(android.R.drawable.ic_delete, "EMERGENCY SOS", emergencyPendingIntent)
            .build()

        systemNotificationManager.notify(JOURNEY_NOTIFICATION_ID, checkInNotification)
    }

    private fun cancelNotification(context: Context) {
        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemNotificationManager.cancel(JOURNEY_NOTIFICATION_ID)
    }

    /**
     * Executes background SMS notification to authorized family contacts with last known GPS location.
     */
    private fun escalateToTrustedContacts(context: Context, journey: SafetyJourney) {
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            val locationProvider = LocationProvider(context)
            val smsDispatcher = SmsDispatcher(context)
            val databaseHelper = HotlineDatabaseHelper(context)
            val authorizedContacts = databaseHelper.getTrustedContacts().filter { it.isAuthorizedForSafetyCheck }

            val currentLocationSnapshot = locationProvider.getCurrentOrCachedLocation()

            // Record incident in central event manager
            val (incidentRecord, _) = EmergencyEventManager.getInstance().registerTrigger(
                source = EmergencyTriggerSource.SAFETY_JOURNEY_DEADLINE,
                detectedPhrase = "Missed check-in for destination: ${journey.destination}",
                location = currentLocationSnapshot
            )

            // Send emergency SMS alerts to family contacts (never auto-call 911)
            var successfullySentSmsCount = 0
            if (authorizedContacts.isNotEmpty()) {
                successfullySentSmsCount = smsDispatcher.dispatchUnresponsiveAlerts(
                    contacts = authorizedContacts,
                    location = currentLocationSnapshot,
                    nearestHotline = null,
                    userName = "Safety Journey User (${journey.destination})"
                )
            }

            EmergencyEventManager.getInstance().recordContactAlertResult(
                incidentId = incidentRecord.incidentId,
                attempted = authorizedContacts.size,
                sent = successfullySentSmsCount
            )
        }
    }
}
