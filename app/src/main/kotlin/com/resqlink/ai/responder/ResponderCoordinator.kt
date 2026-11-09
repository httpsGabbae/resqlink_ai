package com.resqlink.ai.responder

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.emergency.EmergencyEventManager
import com.resqlink.ai.emergency.EmergencyIncident
import com.resqlink.ai.emergency.IncidentLifecycleState
import com.resqlink.ai.rescuecard.RescueCardRepository
import com.resqlink.ai.rescuecard.RescueProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coordinator for Responder Mode. Exposes safety-critical incident and medical information
 * to first responders, medical personnel, and Good Samaritans.
 * Public emergency calls are strictly user/responder-initiated.
 */
class ResponderCoordinator(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val emergencyEventManager = EmergencyEventManager.getInstance()
    private val rescueCardRepository = RescueCardRepository.getInstance(context)

    private val _responderState = MutableStateFlow(buildState(null, rescueCardRepository.profile.value))
    val responderState: StateFlow<ResponderState> = _responderState.asStateFlow()

    init {
        scope.launch {
            emergencyEventManager.currentIncident.collect { incident ->
                refreshState(incident, rescueCardRepository.profile.value)
            }
        }
        scope.launch {
            rescueCardRepository.profile.collect { profile ->
                refreshState(emergencyEventManager.currentIncident.value, profile)
            }
        }
    }

    private fun refreshState(incident: EmergencyIncident?, profile: RescueProfile) {
        _responderState.value = buildState(incident, profile)
    }

    private fun buildState(incident: EmergencyIncident?, profile: RescueProfile): ResponderState {
        val now = System.currentTimeMillis()
        val startTime = incident?.startTimeMillis ?: 0L
        val elapsedSec = if (startTime > 0) (now - startTime) / 1000L else 0L

        val loc = incident?.location
        val locAgeSec = loc?.let { (now - it.timestamp) / 1000L }
        val isStale = locAgeSec != null && locAgeSec > 300 // Older than 5 minutes

        val alertMsg = when {
            incident == null -> "Standby - No active emergency"
            incident.contactAlertsSent > 0 -> "Dispatched: ${incident.contactAlertsSent} contacts notified via SMS"
            incident.contactAlertsAttempted > 0 -> "Alerts in progress (${incident.contactAlertsAttempted} attempted)"
            incident.state == IncidentLifecycleState.AWAITING_CONFIRMATION -> "Awaiting user countdown confirmation"
            else -> "No contact alerts dispatched yet"
        }

        return ResponderState(
            activeIncidentId = incident?.incidentId,
            lifecycleState = incident?.state ?: IncidentLifecycleState.IDLE,
            triggerSourceDescription = incident?.triggerSource?.displayName ?: "Standby",
            incidentStartTimeMillis = startTime,
            elapsedSeconds = elapsedSec,
            currentLocation = loc,
            locationAgeSeconds = locAgeSec,
            isLocationStale = isStale,
            sanitizedRescueProfile = profile,
            emergencySummaryText = profile.toSanitizedEmergencySummary(),
            qrPayload = profile.toQrPayload(),
            contactAlertsSentCount = incident?.contactAlertsSent ?: 0,
            contactAlertsAttemptedCount = incident?.contactAlertsAttempted ?: 0,
            contactAlertStatusMessage = alertMsg,
            primaryContactName = profile.primaryContactName.ifBlank { null },
            primaryContactNumber = profile.primaryContactPhone.ifBlank { null }
        )
    }

    /**
     * User/responder initiated dial action. Never dials automatically without human interaction.
     */
    fun openDialer(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Dialer unavailable
        }
    }
}
