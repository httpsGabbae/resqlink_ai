package com.resqlink.ai.safetyjourney

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.UUID

class JourneyScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val repo = SafetyJourneyRepository.getInstance(context)

    fun startJourney(
        destination: String,
        durationMinutes: Int,
        gracePeriodMinutes: Int = 5,
        notes: String = ""
    ): SafetyJourney {
        val now = System.currentTimeMillis()
        val deadline = now + (durationMinutes * 60 * 1000L)
        val graceDeadline = deadline + (gracePeriodMinutes * 60 * 1000L)

        val journey = SafetyJourney(
            id = "JRN-${UUID.randomUUID().toString().take(8).uppercase()}",
            destination = destination.ifBlank { "Destination" },
            notes = notes,
            startTimeMillis = now,
            targetDurationMinutes = durationMinutes,
            deadlineMillis = deadline,
            gracePeriodMinutes = gracePeriodMinutes,
            graceDeadlineMillis = graceDeadline,
            status = JourneyStatus.ACTIVE
        )

        repo.saveActiveJourney(journey)
        scheduleCheckInAlarm(journey)
        return journey
    }

    fun scheduleCheckInAlarm(journey: SafetyJourney) {
        val intent = Intent(context, JourneyAlarmReceiver::class.java).apply {
            action = JourneyAlarmReceiver.ACTION_CHECK_IN_REMINDER
            putExtra(JourneyAlarmReceiver.EXTRA_JOURNEY_ID, journey.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmExact(journey.deadlineMillis, pendingIntent)
    }

    fun scheduleGraceAlarm(journey: SafetyJourney) {
        val intent = Intent(context, JourneyAlarmReceiver::class.java).apply {
            action = JourneyAlarmReceiver.ACTION_GRACE_EXPIRED
            putExtra(JourneyAlarmReceiver.EXTRA_JOURNEY_ID, journey.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmExact(journey.graceDeadlineMillis, pendingIntent)
    }

    fun extendJourney(additionalMinutes: Int = 15): SafetyJourney? {
        val current = repo.activeJourney.value ?: return null
        val now = System.currentTimeMillis()
        val baseDeadline = if (current.deadlineMillis > now) current.deadlineMillis else now
        val newDeadline = baseDeadline + (additionalMinutes * 60 * 1000L)
        val newGrace = newDeadline + (current.gracePeriodMinutes * 60 * 1000L)

        val updated = current.copy(
            deadlineMillis = newDeadline,
            graceDeadlineMillis = newGrace,
            status = JourneyStatus.ACTIVE,
            extensionCount = current.extensionCount + 1
        )

        repo.saveActiveJourney(updated)
        cancelGraceAlarm()
        scheduleCheckInAlarm(updated)
        return updated
    }

    fun cancelAllAlarms() {
        cancelCheckInAlarm()
        cancelGraceAlarm()
    }

    private fun cancelCheckInAlarm() {
        val intent = Intent(context, JourneyAlarmReceiver::class.java).apply {
            action = JourneyAlarmReceiver.ACTION_CHECK_IN_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun cancelGraceAlarm() {
        val intent = Intent(context, JourneyAlarmReceiver::class.java).apply {
            action = JourneyAlarmReceiver.ACTION_GRACE_EXPIRED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun setAlarmExact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (alarmManager == null) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // If exact alarm permission is missing on Android 12+, use window or inexact fallback
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
