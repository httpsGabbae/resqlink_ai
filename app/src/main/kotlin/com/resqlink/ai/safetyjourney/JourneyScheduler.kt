package com.resqlink.ai.safetyjourney

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.UUID

/**
 * # JourneyScheduler
 *
 * Coordinates real-time hardware alarms and countdown deadlines for the "Safe Travel" feature.
 *
 * Ordinary Human Overview:
 * When someone travels late at night, takes a taxi, or walks home through unfamiliar territory,
 * they can activate a "Safety Journey" in ResQLink AI (e.g., 30 minutes to get home + 5-minute grace period).
 *
 * This scheduler interfaces directly with Android's system `AlarmManager` using `setExactAndAllowWhileIdle`.
 * Even if the phone enters deep sleep (Doze mode) in a pocket or bag:
 * 1. An exact hardware alarm wakes the CPU at the deadline to ask the user: "Did you arrive safely?"
 * 2. If the user does not respond within the grace period, a secondary alarm triggers automatic
 *    SMS emergency escalation to trusted family contacts with the traveler's last known GPS coordinates.
 */
class JourneyScheduler(private val context: Context) {

    private val systemAlarmManager: AlarmManager? = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val journeyRepository: SafetyJourneyRepository = SafetyJourneyRepository.getInstance(context)

    companion object {
        private const val CHECK_IN_REQUEST_CODE = 1001
        private const val GRACE_EXPIRED_REQUEST_CODE = 1002
        private const val MILLISECONDS_PER_MINUTE = 60 * 1000L
    }

    /**
     * Initiates a new monitored travel journey, sets target and grace deadlines,
     * stores the state to disk, and programs the initial check-in alarm.
     *
     * @param destination Plain-text travel destination (e.g., "Home", "Office", "Ayala Mall")
     * @param durationMinutes Expected travel duration in minutes
     * @param gracePeriodMinutes Overtime window before family alerting occurs (default 5 min)
     * @param notes Optional journey notes (e.g., "Taking Taxi Plate ABC-123")
     */
    fun startJourney(
        destination: String,
        durationMinutes: Int,
        gracePeriodMinutes: Int = 5,
        notes: String = ""
    ): SafetyJourney {
        val currentTimeMillis = System.currentTimeMillis()
        val checkInDeadlineMillis = currentTimeMillis + (durationMinutes * MILLISECONDS_PER_MINUTE)
        val graceDeadlineMillis = checkInDeadlineMillis + (gracePeriodMinutes * MILLISECONDS_PER_MINUTE)

        val newJourney = SafetyJourney(
            id = "JRN-${UUID.randomUUID().toString().take(8).uppercase()}",
            destination = destination.ifBlank { "Destination" },
            notes = notes,
            startTimeMillis = currentTimeMillis,
            targetDurationMinutes = durationMinutes,
            deadlineMillis = checkInDeadlineMillis,
            gracePeriodMinutes = gracePeriodMinutes,
            graceDeadlineMillis = graceDeadlineMillis,
            status = JourneyStatus.ACTIVE
        )

        journeyRepository.saveActiveJourney(newJourney)
        scheduleCheckInAlarm(newJourney)
        return newJourney
    }

    /**
     * Schedules the primary check-in alarm that reminds the traveler to verify their safety.
     */
    fun scheduleCheckInAlarm(journey: SafetyJourney) {
        val reminderIntent = Intent(context, JourneyAlarmReceiver::class.java).apply {
            action = JourneyAlarmReceiver.ACTION_CHECK_IN_REMINDER
            putExtra(JourneyAlarmReceiver.EXTRA_JOURNEY_ID, journey.id)
        }
        val pendingCheckInIntent = PendingIntent.getBroadcast(
            context,
            CHECK_IN_REQUEST_CODE,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactHardwareAlarm(journey.deadlineMillis, pendingCheckInIntent)
    }

    /**
     * Schedules the grace period expiration alarm that triggers contact escalation
     * if the traveler failed to respond to the check-in prompt.
     */
    fun scheduleGraceAlarm(journey: SafetyJourney) {
        val graceIntent = Intent(context, JourneyAlarmReceiver::class.java).apply {
            action = JourneyAlarmReceiver.ACTION_GRACE_EXPIRED
            putExtra(JourneyAlarmReceiver.EXTRA_JOURNEY_ID, journey.id)
        }
        val pendingGraceIntent = PendingIntent.getBroadcast(
            context,
            GRACE_EXPIRED_REQUEST_CODE,
            graceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactHardwareAlarm(journey.graceDeadlineMillis, pendingGraceIntent)
    }

    /**
     * Extends an active journey's deadline (for example, if the user encounters traffic or delays).
     *
     * @param additionalMinutes Minutes to append to the journey deadline
     */
    fun extendJourney(additionalMinutes: Int = 15): SafetyJourney? {
        val currentJourney = journeyRepository.activeJourney.value ?: return null
        val currentTimeMillis = System.currentTimeMillis()
        val baseDeadlineMillis = if (currentJourney.deadlineMillis > currentTimeMillis) currentJourney.deadlineMillis else currentTimeMillis
        val extendedDeadlineMillis = baseDeadlineMillis + (additionalMinutes * MILLISECONDS_PER_MINUTE)
        val extendedGraceMillis = extendedDeadlineMillis + (currentJourney.gracePeriodMinutes * MILLISECONDS_PER_MINUTE)

        val updatedJourney = currentJourney.copy(
            deadlineMillis = extendedDeadlineMillis,
            graceDeadlineMillis = extendedGraceMillis,
            status = JourneyStatus.ACTIVE,
            extensionCount = currentJourney.extensionCount + 1
        )

        journeyRepository.saveActiveJourney(updatedJourney)
        cancelGraceAlarm()
        scheduleCheckInAlarm(updatedJourney)
        return updatedJourney
    }

    /**
     * Cancels both the primary check-in alarm and any pending grace-period escalation alarms.
     */
    fun cancelAllAlarms() {
        cancelCheckInAlarm()
        cancelGraceAlarm()
    }

    private fun cancelCheckInAlarm() {
        val reminderIntent = Intent(context, JourneyAlarmReceiver::class.java).apply {
            action = JourneyAlarmReceiver.ACTION_CHECK_IN_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            CHECK_IN_REQUEST_CODE,
            reminderIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && systemAlarmManager != null) {
            systemAlarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun cancelGraceAlarm() {
        val graceIntent = Intent(context, JourneyAlarmReceiver::class.java).apply {
            action = JourneyAlarmReceiver.ACTION_GRACE_EXPIRED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            GRACE_EXPIRED_REQUEST_CODE,
            graceIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && systemAlarmManager != null) {
            systemAlarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Sets a high-priority, exact hardware alarm that will wake the device CPU from low-power Doze sleep.
     */
    private fun setExactHardwareAlarm(triggerTimestampMillis: Long, pendingIntent: PendingIntent) {
        val alarmManager = systemAlarmManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Wakes up CPU even during Android Doze mode
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimestampMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimestampMillis,
                    pendingIntent
                )
            }
        } catch (securityException: SecurityException) {
            // Fallback for Android 12+ (API 31+) if SCHEDULE_EXACT_ALARM permission is revoked in system settings
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimestampMillis,
                pendingIntent
            )
        }
    }
}
