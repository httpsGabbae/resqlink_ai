package com.resqlink.ai.domain.model

sealed interface EmergencyState {
    data object Idle : EmergencyState

    data class GuardianActive(
        val isMonitoring: Boolean = true,
        val detectedPhrase: String? = null,
        val confidence: Float = 0f
    ) : EmergencyState

    data class ConfirmationPending(
        val detectedPhrase: String,
        val remainingSeconds: Int = 10,
        val location: LocationSnapshot? = null,
        val nearestHotline: Hotline? = null,
        val callPriority: CallPriority = CallPriority.LOCAL_HOTLINE,
        val targetName: String = "Local Emergency Dispatch",
        val targetNumber: String = "122"
    ) : EmergencyState

    data class ManualSosGracePeriod(
        val remainingSeconds: Int = 3,
        val location: LocationSnapshot? = null,
        val nearestHotline: Hotline? = null,
        val callPriority: CallPriority = CallPriority.LOCAL_HOTLINE,
        val targetName: String = "Local Emergency Dispatch",
        val targetNumber: String = "122"
    ) : EmergencyState

    data class EmergencyConfirmed(
        val reason: String,
        val callPriority: CallPriority = CallPriority.LOCAL_HOTLINE,
        val targetName: String = "Local Emergency Dispatch",
        val targetNumber: String = "122",
        val dialedHotline: Hotline? = null,
        val smsSentCount: Int = 0,
        val location: LocationSnapshot? = null
    ) : EmergencyState

    data class TimeoutEscalation(
        val smsSentCount: Int,
        val location: LocationSnapshot?,
        val nearestHotline: Hotline?
    ) : EmergencyState

    data class VoiceAssistActive(
        val targetName: String,
        val targetNumber: String,
        val liveTranscript: String,
        val cycleCount: Int = 1,
        val isHandoverComplete: Boolean = false,
        val detectedUserWords: String? = null,
        val location: LocationSnapshot? = null,
        val lastDispatcherQuestion: String? = null,
        val lastAiAnswer: String? = null
    ) : EmergencyState
}

