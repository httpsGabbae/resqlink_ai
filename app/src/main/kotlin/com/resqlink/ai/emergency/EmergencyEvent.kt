package com.resqlink.ai.emergency

import com.resqlink.ai.domain.model.LocationSnapshot
import java.util.UUID

enum class EmergencyTriggerSource(val displayName: String) {
    VOICE_KEYWORD("Voice AI Hotword"),
    MANUAL_SOS_BUTTON("One-Tap SOS Button"),
    SLIDE_TO_SOS("Slide-to-SOS Slider"),
    SAFETY_JOURNEY_DEADLINE("Safety Journey Missed Check-In"),
    EXPERIMENTAL_MOTION_IMPACT("Motion Sensor Impact (Experimental)"),
    SIMULATION_TEST("Safe Emergency Simulation")
}

enum class IncidentLifecycleState(val userLabel: String) {
    IDLE("Idle Standby"),
    SUSPECTED("Distress Suspected"),
    AWAITING_CONFIRMATION("Awaiting User Confirmation (10s)"),
    CONFIRMED("Emergency Confirmed by User"),
    CONTACT_ALERTING("Dispatching Trusted Contact Alerts"),
    ACTIVE_ASSIST("Emergency Voice Assist Active"),
    RESOLVED("Incident Safely Resolved"),
    CANCELLED("Incident Aborted / Cancelled"),
    FAILED("Operation Failed")
}

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
    fun toRecord(): com.resqlink.ai.incidents.IncidentRecord {
        return com.resqlink.ai.incidents.IncidentRecord(
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
            timeline = this.corroboratingSignals.map { signal ->
                com.resqlink.ai.incidents.IncidentTimelineEntry(
                    timestampMillis = this.startTimeMillis,
                    stage = this.state.name,
                    message = signal
                )
            },
            notes = this.notes
        )
    }
}
