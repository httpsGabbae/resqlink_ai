package com.resqlink.ai.responder

import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.emergency.EmergencyIncident
import com.resqlink.ai.emergency.IncidentLifecycleState
import com.resqlink.ai.rescuecard.RescueProfile

/**
 * # ResponderHotline
 *
 * An emergency telephone number displayed on the Responder Assistance screen.
 *
 * @property number The telephone short-code or landline (e.g. "911", "143").
 * @property name The official institution name (e.g. "Philippine Red Cross").
 * @property description A brief explanation of the agency's primary capabilities.
 */
data class ResponderHotline(
    val number: String,
    val name: String,
    val description: String
)

/**
 * # ResponderState
 *
 * The high-contrast, privacy-sanitized emergency information displayed when a first responder
 * (paramedic, police officer, or Good Samaritan) picks up the phone during an active crisis.
 *
 * Ordinary Human Overview:
 * If an unconscious person is found on the road, a rescuer can look at the phone screen to see:
 * 1. How long ago the incident happened (elapsed minutes and seconds).
 * 2. Crucial medical data (blood type, life-threatening allergies, emergency instructions).
 * 3. 1-tap call buttons for the user's family and official national hotlines.
 * 4. An offline QR code that paramedics can scan to import medical history into their tablet.
 */
data class ResponderState(
    val activeIncidentId: String? = null,
    val lifecycleState: IncidentLifecycleState = IncidentLifecycleState.IDLE,
    val triggerSourceDescription: String = "No Active Emergency",
    val incidentStartTimeMillis: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val currentLocation: LocationSnapshot? = null,
    val locationAgeSeconds: Long? = null,
    val isLocationStale: Boolean = false,
    val sanitizedRescueProfile: RescueProfile? = null,
    val emergencySummaryText: String = "",
    val qrPayload: String = "",
    val contactAlertsSentCount: Int = 0,
    val contactAlertsAttemptedCount: Int = 0,
    val contactAlertStatusMessage: String = "No contacts alerted yet",
    val primaryContactNumber: String? = null,
    val primaryContactName: String? = null,
    val officialHotlines: List<ResponderHotline> = listOf(
        ResponderHotline("911", "National Emergency Hotline", "All-hazard emergency dispatch"),
        ResponderHotline("117", "Philippine National Police (PNP)", "Direct police emergency response"),
        ResponderHotline("143", "Philippine Red Cross", "Ambulance, blood, rescue & relief"),
        ResponderHotline("165-02", "DPWH Emergency Road Patrol", "Road obstructions, disasters & hazards")
    )
)
