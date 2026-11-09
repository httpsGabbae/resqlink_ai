package com.resqlink.ai.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.resqlink.ai.MainActivity
import com.resqlink.ai.ai.AudioStreamRecorder
import com.resqlink.ai.ai.KeywordSpotter
import com.resqlink.ai.ai.VadDetector
import kotlinx.coroutines.*

class GuardianForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "guardian_mode_channel"
        const val EMERGENCY_POPUP_CHANNEL_ID = "emergency_distress_alerts"
        const val NOTIFICATION_ID = 9110
        const val EMERGENCY_POPUP_NOTIFICATION_ID = 9911

        const val ACTION_START = "ACTION_START_GUARDIAN"
        const val ACTION_STOP = "ACTION_STOP_GUARDIAN"
        const val EXTRA_EMERGENCY_DETECTED = "EXTRA_EMERGENCY_DETECTED"
        const val EXTRA_KEYWORD_LABEL = "EXTRA_KEYWORD_LABEL"
        const val EXTRA_CONFIRM_IMMEDIATELY = "EXTRA_CONFIRM_IMMEDIATELY"

        var isRunning = false
            private set

        fun dismissEmergencyDistressPopup(context: Context) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(EMERGENCY_POPUP_NOTIFICATION_ID)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var vadDetector: VadDetector
    private lateinit var keywordSpotter: KeywordSpotter
    private lateinit var audioRecorder: AudioStreamRecorder
    private var speechRecognizer: GuardianSpeechRecognizer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        vadDetector = VadDetector()
        keywordSpotter = KeywordSpotter(this)
        audioRecorder = AudioStreamRecorder(vadDetector, keywordSpotter)

        speechRecognizer = GuardianSpeechRecognizer(this) { keyword, phrase ->
            handleDistressDetected(keyword, phrase)
        }

        // Observe acoustic fallback detection events
        serviceScope.launch {
            audioRecorder.detectionEvents.collect { result ->
                handleDistressDetected(result.keyword, result.keyword.label)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopGuardianMode()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startGuardianMode()
            }
        }
        return START_STICKY
    }

    private fun startGuardianMode() {
        isRunning = true
        val notification = buildForegroundNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        speechRecognizer?.startListening()
        audioRecorder.startMonitoring(serviceScope)
    }

    private fun stopGuardianMode() {
        isRunning = false
        dismissEmergencyDistressPopup(this)
        speechRecognizer?.stopListening()
        audioRecorder.stopMonitoring()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun handleDistressDetected(keyword: com.resqlink.ai.ai.DistressKeyword, phrase: String) {
        if (keyword == com.resqlink.ai.ai.DistressKeyword.CANCEL) {
            dismissEmergencyDistressPopup(this)
            val cancelIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_EMERGENCY_DETECTED, true)
                putExtra(EXTRA_KEYWORD_LABEL, "Cancel")
            }
            startActivity(cancelIntent)
            return
        }

        if (keyword.isEmergency) {
            val detectedPhrase = phrase.ifBlank { keyword.label }
            showEmergencyDistressPopup(detectedPhrase)
            triggerEmergencyLockscreenAlert(detectedPhrase)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, GuardianForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Guardian Mode Active")
            .setContentText("Listening offline for wake keyword \"Resqlink Tulong!\"")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn Off", pendingStop)
            .build()
    }

    /**
     * High-Priority Heads-Up Pop-Up Notification with vibration, alarm sound,
     * full-screen intent, and actionable CANCEL & CALL NOW controls.
     */
    private fun showEmergencyDistressPopup(phrase: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_EMERGENCY_DETECTED, true)
            putExtra(EXTRA_KEYWORD_LABEL, phrase)
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            1001,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_EMERGENCY_DETECTED, true)
            putExtra(EXTRA_KEYWORD_LABEL, "Cancel")
        }
        val pendingCancel = PendingIntent.getActivity(
            this,
            1002,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val confirmIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_EMERGENCY_DETECTED, true)
            putExtra(EXTRA_CONFIRM_IMMEDIATELY, true)
        }
        val pendingConfirm = PendingIntent.getActivity(
            this,
            1003,
            confirmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val popupNotification = NotificationCompat.Builder(this, EMERGENCY_POPUP_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🚨 EMERGENCY DISTRESS DETECTED")
            .setContentText("Keyword: \"$phrase\" • 10s Countdown to Dispatch")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("🚨 EMERGENCY DISTRESS DETECTED")
                    .bigText("Voice Keyword: \"$phrase\"\n10-second emergency countdown in progress. Immediate dispatch to emergency contacts & hotline.")
            )
            .setSubText("ResQLink 5km Defense")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(vibrationPattern)
            .setSound(alarmSound)
            .setContentIntent(pendingOpenApp)
            .setFullScreenIntent(pendingOpenApp, true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "CANCEL", pendingCancel)
            .addAction(android.R.drawable.ic_menu_call, "CALL NOW", pendingConfirm)
            .build()

        popupNotification.flags = popupNotification.flags or Notification.FLAG_INSISTENT

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(EMERGENCY_POPUP_NOTIFICATION_ID, popupNotification)
    }

    private fun triggerEmergencyLockscreenAlert(keywordLabel: String) {
        val emergencyIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_EMERGENCY_DETECTED, true)
            putExtra(EXTRA_KEYWORD_LABEL, keywordLabel)
        }
        startActivity(emergencyIntent)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // 1. Ongoing Guardian Mode Service Channel (Low importance)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "ResQLink Guardian Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent indicator that offline voice distress detection is active."
            }
            manager.createNotificationChannel(serviceChannel)

            // 2. High-Priority Emergency Pop-Up Alert Channel (Heads-Up display)
            val popupChannel = NotificationChannel(
                EMERGENCY_POPUP_CHANNEL_ID,
                "Emergency Distress Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority pop-up alert notifications when voice distress keywords are detected."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(alarmSound, audioAttributes)
            }
            manager.createNotificationChannel(popupChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGuardianMode()
        speechRecognizer?.destroy()
        speechRecognizer = null
        keywordSpotter.close()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
