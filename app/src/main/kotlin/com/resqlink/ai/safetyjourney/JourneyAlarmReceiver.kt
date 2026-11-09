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
import com.resqlink.ai.R
import com.resqlink.ai.data.local.HotlineDatabaseHelper
import com.resqlink.ai.data.location.LocationProvider
import com.resqlink.ai.data.telephony.SmsDispatcher
import com.resqlink.ai.emergency.EmergencyEventManager
import com.resqlink.ai.emergency.EmergencyTriggerSource
import com.resqlink.ai.emergency.IncidentLifecycleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles Safety Journey timer alarms and user interactive actions from notification buttons.
 * Guarantees that missed check-in escalations notify trusted contacts ONLY and never auto-dial 911.
 */
class JourneyAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECK_IN_REMINDER = "com.resqlink.ai.action.JOURNEY_CHECK_IN_REMINDER"
        const val ACTION_GRACE_EXPIRED = "com.resqlink.ai.action.JOURNEY_GRACE_EXPIRED"
        const val ACTION_CHECK_IN_SAFE = "com.resqlink.ai.action.JOURNEY_CHECK_IN_SAFE"
        const val ACTION_EXTEND_15M = "com.resqlink.ai.action.JOURNEY_EXTEND_15M"
        const val ACTION_EMERGENCY_SOS = "com.resqlink.ai.action.JOURNEY_EMERGENCY_SOS"

        const val EXTRA_JOURNEY_ID = "extra_journey_id"
        private const val NOTIFICATION_ID = 9021
        private const val CHANNEL_ID = "safety_journey_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val repo = SafetyJourneyRepository.getInstance(context)
        val scheduler = JourneyScheduler(context)
        val currentJourney = repo.activeJourney.value

        when (intent.action) {
            ACTION_CHECK_IN_REMINDER -> {
                if (currentJourney != null && currentJourney.status == JourneyStatus.ACTIVE) {
                    val updated = currentJourney.copy(status = JourneyStatus.GRACE_PERIOD)
                    repo.saveActiveJourney(updated)

                    // Post high-priority check-in reminder
                    showCheckInNotification(context, updated)

                    // Schedule grace-period escalation alarm
                    scheduler.scheduleGraceAlarm(updated)
                }
            }

            ACTION_GRACE_EXPIRED -> {
                if (currentJourney != null && currentJourney.status == JourneyStatus.GRACE_PERIOD) {
                    // Grace period expired without check-in -> ESCALATE TO TRUSTED CONTACTS
                    dismissNotification(context)

                    val escalated = currentJourney.copy(
                        status = JourneyStatus.ESCALATED_CONTACTS,
                        escalationReason = "Safety journey deadline exceeded without user check-in."
                    )
                    repo.archiveJourney(escalated)

                    performContactEscalation(context, escalated)
                }
            }

            ACTION_CHECK_IN_SAFE -> {
                dismissNotification(context)
                scheduler.cancelAllAlarms()
                if (currentJourney != null) {
                    val resolved = currentJourney.copy(
                        status = JourneyStatus.RESOLVED_SAFE,
                        completedTimeMillis = System.currentTimeMillis()
                    )
                    repo.archiveJourney(resolved)
                }
            }

            ACTION_EXTEND_15M -> {
                dismissNotification(context)
                scheduler.extendJourney(15)
            }

            ACTION_EMERGENCY_SOS -> {
                dismissNotification(context)
                scheduler.cancelAllAlarms()
                EmergencyEventManager.getInstance().registerTrigger(
                    source = EmergencyTriggerSource.MANUAL_SOS_BUTTON,
                    detectedPhrase = "Safety Journey SOS button pressed"
                )
            }
        }
    }

    private fun showCheckInNotification(context: Context, journey: SafetyJourney) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Safety Journey Check-in",
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
            notificationManager.createNotificationChannel(channel)
        }

        // Tap content intent -> opens MainActivity
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Safe Action
        val safeIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, JourneyAlarmReceiver::class.java).apply {
                action = ACTION_CHECK_IN_SAFE
                putExtra(EXTRA_JOURNEY_ID, journey.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Extend Action
        val extendIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, JourneyAlarmReceiver::class.java).apply {
                action = ACTION_EXTEND_15M
                putExtra(EXTRA_JOURNEY_ID, journey.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Emergency SOS Action
        val emergencyIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, JourneyAlarmReceiver::class.java).apply {
                action = ACTION_EMERGENCY_SOS
                putExtra(EXTRA_JOURNEY_ID, journey.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Are you safe? Check-in Overdue")
            .setContentText("Destination: ${journey.destination}. Tap 'I'm Safe' or journey will escalate in ${journey.gracePeriodMinutes} mins.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.checkbox_on_background, "I'M SAFE", safeIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "+15 MINS", extendIntent)
            .addAction(android.R.drawable.ic_delete, "EMERGENCY SOS", emergencyIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun dismissNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun performContactEscalation(context: Context, journey: SafetyJourney) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val locationProvider = LocationProvider(context)
            val smsDispatcher = SmsDispatcher(context)
            val dbHelper = HotlineDatabaseHelper(context)
            val contacts = dbHelper.getTrustedContacts().filter { it.isAuthorizedForSafetyCheck }

            val location = locationProvider.getCurrentOrCachedLocation()

            // Register incident in event manager
            val (incident, _) = EmergencyEventManager.getInstance().registerTrigger(
                source = EmergencyTriggerSource.SAFETY_JOURNEY_DEADLINE,
                detectedPhrase = "Missed check-in for destination: ${journey.destination}",
                location = location
            )

            // Send SMS to trusted contacts ONLY (Never call 911)
            var sent = 0
            if (contacts.isNotEmpty()) {
                sent = smsDispatcher.dispatchUnresponsiveAlerts(
                    contacts = contacts,
                    location = location,
                    nearestHotline = null,
                    userName = "Safety Journey User (${journey.destination})"
                )
            }

            EmergencyEventManager.getInstance().recordContactAlertResult(
                incidentId = incident.incidentId,
                attempted = contacts.size,
                sent = sent
            )
        }
    }
}
