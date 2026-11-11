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
 * # ResponderCoordinator
 *
 * Coordinates and formats real-time emergency telemetry for first responders and rescuers.
 *
 * Ordinary Human Overview:
 * When someone discovers an accident victim, every second counts.
 * This class observes the active emergency incident and the user's Rescue Card, combining them
 * into a single clean [ResponderState] that powers the Responder Assistance screen.
 *
 * Safety Invariant:
 * Public emergency calls are strictly user- or responder-initiated via [openDialer].
 * The application never silently dials 911 on its own.
 */
class ResponderCoordinator(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val emergencyEventManager = EmergencyEventManager.getInstance()
    private val rescueCardRepository = RescueCardRepository.getInstance(context)

    private val _responderState = MutableStateFlow(
        buildResponderState(
            activeIncident = null,
            rescueProfile = rescueCardRepository.profile.value
        )
    )
    val responderState: StateFlow<ResponderState> = _responderState.asStateFlow()

    init {
        // Observe changes to the current emergency incident
        coroutineScope.launch {
            emergencyEventManager.currentIncident.collect { activeIncident ->
                refreshResponderState(activeIncident, rescueCardRepository.profile.value)
            }
        }
        // Observe changes to the user's medical profile
        coroutineScope.launch {
            rescueCardRepository.profile.collect { updatedProfile ->
                refreshResponderState(emergencyEventManager.currentIncident.value, updatedProfile)
            }
        }
    }

    private fun refreshResponderState(activeIncident: EmergencyIncident?, rescueProfile: RescueProfile) {
        _responderState.value = buildResponderState(activeIncident, rescueProfile)
    }

    /**
     * Synthesizes incident telemetry, location age, and medical records into a clean UI state.
     */
    private fun buildResponderState(
        activeIncident: EmergencyIncident?,
        rescueProfile: RescueProfile
    ): ResponderState {
        val currentTimestamp = System.currentTimeMillis()
        val incidentStartTimeMillis = activeIncident?.startTimeMillis ?: 0L
        val elapsedSeconds = if (incidentStartTimeMillis > 0) (currentTimestamp - incidentStartTimeMillis) / 1000L else 0L

        val incidentLocation = activeIncident?.location
        val locationAgeSeconds = incidentLocation?.let { (currentTimestamp - it.timestamp) / 1000L }
        val isLocationStale = locationAgeSeconds != null && locationAgeSeconds > 300 // Flag stale if older than 5 minutes

        val alertStatusMessage = when {
            activeIncident == null -> "Standby - No active emergency"
            activeIncident.contactAlertsSent > 0 -> "Dispatched: ${activeIncident.contactAlertsSent} contacts notified via SMS"
            activeIncident.contactAlertsAttempted > 0 -> "Alerts in progress (${activeIncident.contactAlertsAttempted} attempted)"
            activeIncident.state == IncidentLifecycleState.AWAITING_CONFIRMATION -> "Awaiting user countdown confirmation"
            else -> "No contact alerts dispatched yet"
        }

        return ResponderState(
            activeIncidentId = activeIncident?.incidentId,
            lifecycleState = activeIncident?.state ?: IncidentLifecycleState.IDLE,
            triggerSourceDescription = activeIncident?.triggerSource?.displayName ?: "Standby",
            incidentStartTimeMillis = incidentStartTimeMillis,
            elapsedSeconds = elapsedSeconds,
            currentLocation = incidentLocation,
            locationAgeSeconds = locationAgeSeconds,
            isLocationStale = isLocationStale,
            sanitizedRescueProfile = rescueProfile,
            emergencySummaryText = rescueProfile.toSanitizedEmergencySummary(),
            qrPayload = rescueProfile.toQrPayload(),
            contactAlertsSentCount = activeIncident?.contactAlertsSent ?: 0,
            contactAlertsAttemptedCount = activeIncident?.contactAlertsAttempted ?: 0,
            contactAlertStatusMessage = alertStatusMessage,
            primaryContactName = rescueProfile.primaryContactName.ifBlank { null },
            primaryContactNumber = rescueProfile.primaryContactPhone.ifBlank { null }
        )
    }

    /**
     * Opens the Android system dialer pad with the requested emergency number.
     * Requires explicit physical touch by the human rescuer.
     */
    fun openDialer(phoneNumber: String) {
        try {
            val dialerIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phoneNumber")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialerIntent)
        } catch (launchException: Exception) {
            // Android dialer activity unavailable
        }
    }
}
