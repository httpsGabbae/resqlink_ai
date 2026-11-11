package com.resqlink.ai.emergency

import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.incidents.IncidentRecord
import com.resqlink.ai.incidents.IncidentTimelineEntry

/**
 * # EmergencyTriggerSource
 *
 * Identifies the exact origin of what triggered an emergency event in ResQLink.
 */
enum class EmergencyTriggerSource(val displayName: String) {
    /** Spoken distress phrase ("Tulong!", "Help!") detected by microphone analysis. */
    VOICE_KEYWORD("Voice AI Hotword"),

    /** The large red 1-tap SOS button was pressed on the home screen. */
    MANUAL_SOS_BUTTON("One-Tap SOS Button"),

    /** The user dragged the interactive Slide-to-SOS slider. */
    SLIDE_TO_SOS("Slide-to-SOS Slider"),

    /** The user set a Safety Journey timer (e.g. 15 min commute) and failed to check in safely. */
    SAFETY_JOURNEY_DEADLINE("Safety Journey Missed Check-In"),

    /** Accelerometer detected a heavy impact (>2.8g) followed by prolonged physical stillness. */
    EXPERIMENTAL_MOTION_IMPACT("Motion Sensor Impact (Experimental)"),

    /** Safe educational emergency test simulation triggered in Readiness Center. */
    SIMULATION_TEST("Safe Emergency Simulation")
}

/**
 * # IncidentLifecycleState
 *
 * The progressive chronological lifecycle stages of an emergency incident.
 */
enum class IncidentLifecycleState(val userLabel: String) {
    /** App is idle and monitoring for potential emergencies. */
    IDLE("Idle Standby"),

    /** AI or hardware sensors suspect that an emergency event has occurred. */
    SUSPECTED("Distress Suspected"),

    /** 10-second countdown is displayed on screen awaiting user confirmation or cancellation. */
    AWAITING_CONFIRMATION("Awaiting User Confirmation (10s)"),

    /** Emergency confirmed either by user action or by countdown expiration. */
    CONFIRMED("Emergency Confirmed by User"),

    /** Cellular radio is actively transmitting emergency SMS alerts with GPS coordinates. */
    CONTACT_ALERTING("Dispatching Trusted Contact Alerts"),

    /** Phone is in an active phone call with emergency services, broadcasting automated speech. */
    ACTIVE_ASSIST("Emergency Voice Assist Active"),

    /** The emergency has ended and the user or first responders confirmed safety. */
    RESOLVED("Incident Safely Resolved"),

    /** The pending alert was aborted or canceled by the user during the countdown. */
    CANCELLED("Incident Aborted / Cancelled"),

    /** A platform error occurred during transmission (e.g. airplane mode or cellular radio failure). */
    FAILED("Operation Failed")
}

/**
 * # EmergencyIncident
 *
 * Encapsulates the complete real-time runtime state of an active emergency incident.
 *
 * @property incidentId Unique tracking identifier (e.g. "INC-A1B2C3D4").
 * @property triggerSource The input that initiated this incident (Voice, Button, Motion, etc.).
 * @property initialDetectedPhrase The phrase heard by speech recognition, if applicable.
 * @property state Current operational lifecycle state of the incident.
 * @property startTimeMillis Epoch time in milliseconds when the incident began.
 * @property resolvedTimeMillis Epoch time in milliseconds when the incident was marked resolved.
 * @property location GPS coordinates attached to the incident.
 * @property isSimulated True if running in test simulation mode (never sends real SMS or calls).
 * @property contactAlertsAttempted Total number of SMS transmissions attempted.
 * @property contactAlertsSent Total number of SMS transmissions successfully dispatched.
 * @property publicEmergencyCallInitiated True if a cellular phone call was placed.
 * @property corroboratingSignals Sequence of chronological event logs attached to this incident.
 * @property notes Additional telemetry or context notes.
 */
data class EmergencyIncident(
    val incidentId: String,
    val triggerSource: EmergencyTriggerSource,
    val initialDetectedPhrase: String? = null,
    val state: IncidentLifecycleState = IncidentLifecycleState.SUSPECTED,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val resolvedTimeMillis: Long? = null,
    val location: LocationSnapshot? = null,
    val isSimulated: Boolean = false,
    val contactAlertsAttempted: Int = 0,
    val contactAlertsSent: Int = 0,
    val publicEmergencyCallInitiated: Boolean = false,
    val corroboratingSignals: List<String> = emptyList(),
    val notes: String? = null
) {
    /**
     * Converts this active runtime incident into a persistent [IncidentRecord]
     * for saving to the local SQLite database.
     */
    fun toRecord(): IncidentRecord {
        return IncidentRecord(
            incidentId = this.incidentId,
            triggerSource = this.triggerSource.displayName,
            initialDetectedPhrase = this.initialDetectedPhrase,
            finalState = this.state.name,
            startTimeMillis = this.startTimeMillis,
            resolvedTimeMillis = this.resolvedTimeMillis,
            latitude = this.location?.latitude,
            longitude = this.location?.longitude,
            locationAccuracyMeters = this.location?.accuracyMeters,
            isSimulated = this.isSimulated,
            contactsAttempted = this.contactAlertsAttempted,
            contactsSent = this.contactAlertsSent,
            emergencyCallInitiated = this.publicEmergencyCallInitiated,
            timeline = this.corroboratingSignals.map { signalDescription ->
                IncidentTimelineEntry(
                    timestampMillis = this.startTimeMillis,
                    stage = this.state.name,
                    message = signalDescription
                )
            },
            notes = this.notes
        )
    }
}
