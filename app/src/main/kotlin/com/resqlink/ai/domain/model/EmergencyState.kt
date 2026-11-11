package com.resqlink.ai.domain.model

/**
 * # EmergencyState
 *
 * This sealed interface defines every possible state that the ResQLink AI emergency
 * engine can be in. A sealed interface acts like an exclusive enum with superpowers:
 * the app can only ever be in EXACTLY ONE of these states at any given moment.
 *
 * Ordinary Human Overview:
 * - [Idle]: Everything is calm. The app is waiting for user action or background monitoring.
 * - [GuardianActive]: The background listener is quietly running, waiting for distress keywords.
 * - [ConfirmationPending]: A distress keyword ("Tulong", "Help") or fall was detected. A 10-second
 *   safety countdown is displayed on screen so the user can abort if it was a false alarm.
 * - [ManualSosGracePeriod]: The user manually slid the SOS button. A short 3-second grace countdown
 *   runs in case it was tapped by accident.
 * - [EmergencyConfirmed]: The emergency is confirmed! Outgoing SMS alerts with live GPS coordinates
 *   have been dispatched, and the phone is placing an emergency call.
 * - [TimeoutEscalation]: The 10-second countdown finished with zero response from the user. An
 *   unverified distress SMS with GPS coordinates is sent to trusted contacts.
 * - [VoiceAssistActive]: The phone is connected to an emergency operator on speakerphone, and ResQLink's
 *   on-device speech synthesizer is broadcasting vital information (name, blood type, GPS coordinates).
 */
sealed interface EmergencyState {

    /**
     * Standby state. No ongoing emergency, no active countdowns.
     */
    data object Idle : EmergencyState

    /**
     * The background safety guardian is actively monitoring the device sensors (microphone or accelerometer).
     *
     * @property isMonitoring True when the background service is actively running.
     * @property detectedPhrase The most recent voice snippet or keyword detected, if any.
     * @property confidence A value between 0.0 and 1.0 showing how confident the AI model was in the detection.
     */
    data class GuardianActive(
        val isMonitoring: Boolean = true,
        val detectedPhrase: String? = null,
        val confidence: Float = 0f
    ) : EmergencyState

    /**
     * A possible emergency was detected automatically by sensors or voice.
     * A 10-second countdown is displayed on screen with an audible prompt.
     * If the user taps "I Am Safe", the state resets to [Idle].
     * If the timer runs out without a response, it escalates automatically.
     *
     * @property detectedPhrase The distress word heard by the microphone (e.g. "Tulong!").
     * @property remainingSeconds The number of seconds left before escalation (defaults to 10 seconds).
     * @property location The last known GPS location snapshot of the user.
     * @property nearestHotline The closest verified emergency station within 5km.
     * @property callPriority Which phone number to dial first (Local Hotline, Universal 911, or Family).
     * @property targetName The human-readable name of the station or contact being dialed.
     * @property targetNumber The actual phone number that will be called (e.g. "911", "122").
     */
    data class ConfirmationPending(
        val detectedPhrase: String,
        val remainingSeconds: Int = 10,
        val location: LocationSnapshot? = null,
        val nearestHotline: Hotline? = null,
        val callPriority: CallPriority = CallPriority.LOCAL_HOTLINE,
        val targetName: String = "Local Emergency Dispatch",
        val targetNumber: String = "122"
    ) : EmergencyState

    /**
     * The user manually dragged the SOS slider.
     * A 3-second grace countdown is displayed so the user can easily abort if triggered by mistake.
     *
     * @property remainingSeconds Countdown timer before placing the call (defaults to 3 seconds).
     * @property location The user's live GPS coordinates.
     * @property nearestHotline The nearest local emergency facility.
     * @property callPriority The user's chosen calling priority.
     * @property targetName The name of the emergency destination (e.g. "Universal 911").
     * @property targetNumber The phone number to dial.
     */
    data class ManualSosGracePeriod(
        val remainingSeconds: Int = 3,
        val location: LocationSnapshot? = null,
        val nearestHotline: Hotline? = null,
        val callPriority: CallPriority = CallPriority.LOCAL_HOTLINE,
        val targetName: String = "Local Emergency Dispatch",
        val targetNumber: String = "122"
    ) : EmergencyState

    /**
     * Emergency is fully confirmed!
     * 1. Emergency SMS messages containing live Google Maps GPS coordinates are sent to trusted contacts.
     * 2. The Android phone dialer is initiated to connect the user to emergency services.
     *
     * @property reason The origin of the alert (e.g. "Manual SOS Trigger" or "Voice Distress Confirmation").
     * @property callPriority How the outgoing call was routed.
     * @property targetName Name of the service dialed.
     * @property targetNumber Phone number dialed.
     * @property dialedHotline The hotline object dialed, if applicable.
     * @property smsSentCount The number of trusted contacts who received an emergency SMS.
     * @property location The exact GPS location where the emergency was triggered.
     */
    data class EmergencyConfirmed(
        val reason: String,
        val callPriority: CallPriority = CallPriority.LOCAL_HOTLINE,
        val targetName: String = "Local Emergency Dispatch",
        val targetNumber: String = "122",
        val dialedHotline: Hotline? = null,
        val smsSentCount: Int = 0,
        val location: LocationSnapshot? = null
    ) : EmergencyState

    /**
     * Escalation state triggered when the user failed to answer the 10-second countdown.
     * Per safety regulations, ResQLink NEVER silently auto-dials 911 for an unverified timeout
     * to avoid swamping emergency operators. Instead, an unverified text alert is sent to trusted contacts.
     *
     * @property smsSentCount Total SMS alerts successfully handed to the cellular radio.
     * @property location The user's GPS position when the timer expired.
     * @property nearestHotline The nearest station identified for reference.
     */
    data class TimeoutEscalation(
        val smsSentCount: Int,
        val location: LocationSnapshot?,
        val nearestHotline: Hotline?
    ) : EmergencyState

    /**
     * In-Call Voice Assist mode.
     * If the user is injured, incapacitated, or unable to speak after a crash, ResQLink AI
     * acts as a voice proxy on speakerphone to relay critical telemetry to the human dispatcher.
     *
     * @property targetName Name of the connected dispatch center.
     * @property targetNumber Phone number currently connected.
     * @property liveTranscript Real-time text of what the AI is speaking aloud to the dispatcher.
     * @property cycleCount How many times the emergency audio loop has repeated (1 to 3 cycles).
     * @property isHandoverComplete True when the user wakes up and speaks, returning control to the human.
     * @property detectedUserWords The first words spoken by the user that triggered the handover.
     * @property location GPS position attached to the telemetry report.
     * @property lastDispatcherQuestion The question asked by the emergency operator (e.g. "Where are you?").
     * @property lastAiAnswer The factual answer formulated by the on-device AI model.
     */
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
