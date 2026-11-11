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

/**
 * # GuardianForegroundService
 *
 * Android Foreground Service managing persistent, hands-free offline microphone monitoring.
 *
 * Ordinary Human Overview:
 * When you walk through a dark alley or return home late at night, you can toggle "Guardian Mode" ON.
 *
 * This background service:
 * 1. Shows a persistent Android notification indicating the phone is actively protecting you.
 * 2. Holds a foreground microphone lock (`FOREGROUND_SERVICE_TYPE_MICROPHONE`) so Android OS
 *    does not terminate audio recording when the screen turns off.
 * 3. Runs a two-stage detection pipeline:
 *    - Continuous Tagalog/English speech recognition ([GuardianSpeechRecognizer]).
 *    - Offline acoustic VAD + TFLite neural keyword spotter ([AudioStreamRecorder]).
 * 4. When distress is heard ("Resqlink Tulong!", "Tulong!", "Help!"), it immediately launches
 *    a high-priority full-screen alarm notification and starts a 10-second countdown to alert family.
 */
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

        var isRunning: Boolean = false
            private set

        /**
         * Dismisses the active full-screen distress alert popup notification.
         */
        fun dismissEmergencyDistressPopup(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(EMERGENCY_POPUP_NOTIFICATION_ID)
            } catch (ignored: Exception) {}
        }
    }

    private val serviceCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var voiceActivityDetector: VadDetector
    private lateinit var keywordSpotter: KeywordSpotter
    private lateinit var audioStreamRecorder: AudioStreamRecorder
    private var guardianSpeechRecognizer: GuardianSpeechRecognizer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        voiceActivityDetector = VadDetector()
        keywordSpotter = KeywordSpotter(this)
        audioStreamRecorder = AudioStreamRecorder(voiceActivityDetector, keywordSpotter)

        guardianSpeechRecognizer = GuardianSpeechRecognizer(this) { detectedKeyword, detectedPhrase ->
            handleDistressTrigger(detectedKeyword, detectedPhrase)
        }

        // Observe acoustic fallback detection events from the TFLite neural network
        serviceCoroutineScope.launch {
            audioStreamRecorder.detectionEvents.collect { detectionResult ->
                handleDistressTrigger(detectionResult.keyword, detectionResult.keyword.label)
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
        val foregroundStatusNotification = buildForegroundServiceNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                foregroundStatusNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, foregroundStatusNotification)
        }

        guardianSpeechRecognizer?.startListening()
        audioStreamRecorder.startMonitoring(serviceCoroutineScope)
    }

    private fun stopGuardianMode() {
        isRunning = false
        dismissEmergencyDistressPopup(this)
        guardianSpeechRecognizer?.stopListening()
        audioStreamRecorder.stopMonitoring()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /**
     * Responds to detected voice distress keywords or hands-free cancel commands.
     */
    private fun handleDistressTrigger(keyword: com.resqlink.ai.ai.DistressKeyword, phrase: String) {
        if (keyword == com.resqlink.ai.ai.DistressKeyword.CANCEL) {
            dismissEmergencyDistressPopup(this)
            val cancelIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_EMERGENCY_DETECTED, true)
                putExtra(EXTRA_KEYWORD_LABEL, "Cancel")
            }
            startActivity(cancelIntent)
            return
        }

        if (keyword.isEmergency) {
            val detectedPhraseText = phrase.ifBlank { keyword.label }
            displayEmergencyDistressPopup(detectedPhraseText)
            launchEmergencyLockscreenActivity(detectedPhraseText)
        }
    }

    private fun buildForegroundServiceNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenAppIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopServiceIntent = Intent(this, GuardianForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopServiceIntent = PendingIntent.getService(
            this,
            1,
            stopServiceIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Guardian Mode Active")
            .setContentText("Listening offline for wake keyword \"Resqlink Tulong!\"")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpenAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn Off", pendingStopServiceIntent)
            .build()
    }

    /**
     * High-Priority Heads-Up Pop-Up Notification with vibration, alarm sound,
     * full-screen intent, and actionable CANCEL & CALL NOW controls.
     */
    private fun displayEmergencyDistressPopup(phrase: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_EMERGENCY_DETECTED, true)
            putExtra(EXTRA_KEYWORD_LABEL, phrase)
        }
        val pendingOpenAppIntent = PendingIntent.getActivity(
            this,
            1001,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelEmergencyIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_EMERGENCY_DETECTED, true)
            putExtra(EXTRA_KEYWORD_LABEL, "Cancel")
        }
        val pendingCancelIntent = PendingIntent.getActivity(
            this,
            1002,
            cancelEmergencyIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val immediateConfirmIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_EMERGENCY_DETECTED, true)
            putExtra(EXTRA_CONFIRM_IMMEDIATELY, true)
        }
        val pendingConfirmIntent = PendingIntent.getActivity(
            this,
            1003,
            immediateConfirmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmVibrationPattern = longArrayOf(0, 800, 400, 800, 400)
        val alarmSoundRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val popupDistressNotification = NotificationCompat.Builder(this, EMERGENCY_POPUP_CHANNEL_ID)
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
            .setVibrate(alarmVibrationPattern)
            .setSound(alarmSoundRingtoneUri)
            .setContentIntent(pendingOpenAppIntent)
            .setFullScreenIntent(pendingOpenAppIntent, true)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "CANCEL", pendingCancelIntent)
            .addAction(android.R.drawable.ic_menu_call, "CALL NOW", pendingConfirmIntent)
            .build()

        popupDistressNotification.flags = popupDistressNotification.flags or Notification.FLAG_INSISTENT

        val systemNotificationManager = getSystemService(NotificationManager::class.java)
        systemNotificationManager.notify(EMERGENCY_POPUP_NOTIFICATION_ID, popupDistressNotification)
    }

    private fun launchEmergencyLockscreenActivity(keywordLabel: String) {
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
            val systemNotificationManager = getSystemService(NotificationManager::class.java)

            // 1. Ongoing Guardian Mode Service Channel (Low importance)
            val serviceNotificationChannel = NotificationChannel(
                CHANNEL_ID,
                "ResQLink Guardian Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent indicator that offline voice distress detection is active."
            }
            systemNotificationManager.createNotificationChannel(serviceNotificationChannel)

            // 2. High-Priority Emergency Pop-Up Alert Channel (Heads-Up display)
            val popupDistressNotificationChannel = NotificationChannel(
                EMERGENCY_POPUP_CHANNEL_ID,
                "Emergency Distress Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority pop-up alert notifications when voice distress keywords are detected."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(alarmSoundUri, audioAttributes)
            }
            systemNotificationManager.createNotificationChannel(popupDistressNotificationChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGuardianMode()
        guardianSpeechRecognizer?.destroy()
        guardianSpeechRecognizer = null
        keywordSpotter.close()
        serviceCoroutineScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
