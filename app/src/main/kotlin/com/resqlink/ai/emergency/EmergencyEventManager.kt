package com.resqlink.ai.emergency

import com.resqlink.ai.domain.model.LocationSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Coordinates emergency event states, prevents duplicate simultaneous dispatch loops,
 * and maintains safety policy guarantees (e.g. no automatic 911 calls on timeouts).
 */
class EmergencyEventManager private constructor() {

    companion object {
        @Volatile
        private var instance: EmergencyEventManager? = null

        fun getInstance(): EmergencyEventManager {
            return instance ?: synchronized(this) {
                instance ?: EmergencyEventManager().also { instance = it }
            }
        }
    }

    private val _currentIncident = MutableStateFlow<EmergencyIncident?>(null)
    val currentIncident: StateFlow<EmergencyIncident?> = _currentIncident.asStateFlow()

    private val _incidentHistory = MutableStateFlow<List<EmergencyIncident>>(emptyList())
    val incidentHistory: StateFlow<List<EmergencyIncident>> = _incidentHistory.asStateFlow()

    /**
     * Registers a new emergency trigger or corroborates an existing active incident.
     * Returns a Pair of the incident and a Boolean indicating whether this was a brand-new incident (true)
     * or an existing incident that was corroborated without duplicate dispatch (false).
     */
    @Synchronized
    fun registerTrigger(
        source: EmergencyTriggerSource,
        detectedPhrase: String? = null,
        location: LocationSnapshot? = null,
        isSimulated: Boolean = false
    ): Pair<EmergencyIncident, Boolean> {
        val active = _currentIncident.value
        val isOngoing = active != null && (
                active.state == IncidentLifecycleState.SUSPECTED ||
                active.state == IncidentLifecycleState.AWAITING_CONFIRMATION ||
                active.state == IncidentLifecycleState.CONFIRMED ||
                active.state == IncidentLifecycleState.CONTACT_ALERTING ||
                active.state == IncidentLifecycleState.ACTIVE_ASSIST
        )

        if (isOngoing && active != null) {
            // Deduplicate: Don't spin up another dispatch cycle. Log as corroborating signal!
            val signal = "[${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}] Corroborating trigger from ${source.displayName}" +
                    (if (!detectedPhrase.isNullOrBlank()) ": \"$detectedPhrase\"" else "")
            val updated = active.copy(
                corroboratingSignals = active.corroboratingSignals + signal,
                location = location ?: active.location
            )
            _currentIncident.value = updated
            return Pair(updated, false)
        }

        // New Incident Creation
        val newIncidentId = generateIncidentId(isSimulated)
        val initialIncident = EmergencyIncident(
            incidentId = newIncidentId,
            triggerSource = source,
            initialDetectedPhrase = detectedPhrase,
            state = if (source == EmergencyTriggerSource.MANUAL_SOS_BUTTON || source == EmergencyTriggerSource.SLIDE_TO_SOS) {
                IncidentLifecycleState.CONFIRMED
            } else {
                IncidentLifecycleState.AWAITING_CONFIRMATION
            },
            startTimeMillis = System.currentTimeMillis(),
            location = location,
            isSimulated = isSimulated,
            corroboratingSignals = listOf(
                "Triggered by ${source.displayName}" + if (!detectedPhrase.isNullOrBlank()) ": \"$detectedPhrase\"" else ""
            )
        )

        _currentIncident.value = initialIncident
        return Pair(initialIncident, true)
    }

    @Synchronized
    fun transitionState(
        incidentId: String,
        newState: IncidentLifecycleState,
        notes: String? = null
    ): EmergencyIncident? {
        val current = _currentIncident.value
        if (current == null || current.incidentId != incidentId) return null

        val updated = current.copy(
            state = newState,
            notes = if (notes != null) "${current.notes ?: ""}\n$notes".trim() else current.notes,
            resolvedTimeMillis = if (newState == IncidentLifecycleState.RESOLVED || newState == IncidentLifecycleState.CANCELLED) {
                System.currentTimeMillis()
            } else current.resolvedTimeMillis
        )

        _currentIncident.value = updated

        if (newState == IncidentLifecycleState.RESOLVED || newState == IncidentLifecycleState.CANCELLED) {
            archiveIncident(updated)
        }

        return updated
    }

    @Synchronized
    fun recordContactAlertResult(
        incidentId: String,
        attempted: Int,
        sent: Int
    ) {
        val current = _currentIncident.value
        if (current == null || current.incidentId != incidentId) return
        _currentIncident.value = current.copy(
            contactAlertsAttempted = attempted,
            contactAlertsSent = sent,
            state = IncidentLifecycleState.CONTACT_ALERTING
        )
    }

    @Synchronized
    fun recordCallInitiated(
        incidentId: String,
        isPublicEmergency: Boolean,
        callTargetName: String
    ) {
        val current = _currentIncident.value
        if (current == null || current.incidentId != incidentId) return
        val signal = "Call placed to: $callTargetName (Public 911: $isPublicEmergency)"
        _currentIncident.value = current.copy(
            publicEmergencyCallInitiated = isPublicEmergency,
            corroboratingSignals = current.corroboratingSignals + signal,
            state = IncidentLifecycleState.ACTIVE_ASSIST
        )
    }

    @Synchronized
    fun cancelIncident(incidentId: String, reason: String = "User Aborted") {
        transitionState(incidentId, IncidentLifecycleState.CANCELLED, "Cancelled: $reason")
    }

    @Synchronized
    fun resolveIncident(incidentId: String, reason: String = "Resolved") {
        transitionState(incidentId, IncidentLifecycleState.RESOLVED, "Resolution: $reason")
    }

    @Synchronized
    fun clearActiveIncident() {
        val current = _currentIncident.value
        if (current != null) {
            archiveIncident(current.copy(resolvedTimeMillis = System.currentTimeMillis()))
        }
        _currentIncident.value = null
    }

    private fun archiveIncident(incident: EmergencyIncident) {
        val list = _incidentHistory.value.toMutableList()
        val existingIndex = list.indexOfFirst { it.incidentId == incident.incidentId }
        if (existingIndex >= 0) {
            list[existingIndex] = incident
        } else {
            list.add(0, incident)
        }
        // Limit local memory retention to last 50 incidents
        if (list.size > 50) {
            list.removeAt(list.size - 1)
        }
        _incidentHistory.value = list
    }

    private fun generateIncidentId(isSimulated: Boolean): String {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val shortUuid = UUID.randomUUID().toString().substring(0, 6).uppercase(Locale.US)
        val prefix = if (isSimulated) "SIM" else "INC"
        return "$prefix-$dateStr-$shortUuid"
    }
}
