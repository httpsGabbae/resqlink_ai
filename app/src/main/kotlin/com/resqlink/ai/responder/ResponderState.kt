package com.resqlink.ai.responder

import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.emergency.EmergencyIncident
import com.resqlink.ai.emergency.IncidentLifecycleState
import com.resqlink.ai.rescuecard.RescueProfile

/**
 * UI State exposed to First Responders or Good Samaritans assisting the user.
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

data class ResponderHotline(
    val number: String,
    val name: String,
    val description: String
)
