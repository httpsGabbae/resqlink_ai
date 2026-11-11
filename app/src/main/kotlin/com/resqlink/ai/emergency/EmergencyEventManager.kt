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
 * # EmergencyEventManager
 *
 * The central coordination brain of the ResQLink AI emergency engine.
 *
 * Ordinary Human Overview:
 * When danger occurs, multiple sensors might fire at the same time:
 * for example, a car crash triggers the accelerometer AND the driver shouts "Tulong!".
 * If not managed properly, an app might try to send 10 text messages at once and crash.
 *
 * [EmergencyEventManager] solves this by acting as the **single source of truth**:
 * 1. **Deduplication**: If an incident is already ongoing, any new trigger is logged as a
 *    "corroborating signal" (evidence) instead of creating duplicate panic loops.
 * 2. **State Transition Machine**: Moves the incident cleanly from Suspected -> Confirmation ->
 *    Alerting -> Active -> Resolved.
 * 3. **Audit Trail**: Archives every incident into an in-memory chronological history list.
 */
class EmergencyEventManager private constructor() {

    companion object {
        @Volatile
        private var singletonInstance: EmergencyEventManager? = null

        /**
         * Returns the global singleton instance using double-checked locking for thread safety.
         */
        fun getInstance(): EmergencyEventManager {
            return singletonInstance ?: synchronized(this) {
                singletonInstance ?: EmergencyEventManager().also { singletonInstance = it }
            }
        }
    }

    private val _currentIncident = MutableStateFlow<EmergencyIncident?>(null)
    val currentIncident: StateFlow<EmergencyIncident?> = _currentIncident.asStateFlow()

    private val _incidentHistory = MutableStateFlow<List<EmergencyIncident>>(emptyList())
    val incidentHistory: StateFlow<List<EmergencyIncident>> = _incidentHistory.asStateFlow()

    /**
     * Registers a new emergency trigger or corroborates an existing active incident.
     *
     * @param source What caused this trigger (Voice Keyword, SOS Button, Fall Motion, etc.).
     * @param detectedPhrase The words detected, if any.
     * @param location The GPS coordinates captured at the moment of trigger.
     * @param isSimulated True if running an educational test simulation.
     * @return A Pair containing:
     *   1. The [EmergencyIncident] record.
     *   2. A Boolean: true if this is a brand-new emergency; false if it was merged into an ongoing incident.
     */
    @Synchronized
    fun registerTrigger(
        source: EmergencyTriggerSource,
        detectedPhrase: String? = null,
        location: LocationSnapshot? = null,
        isSimulated: Boolean = false
    ): Pair<EmergencyIncident, Boolean> {
        val activeIncident = _currentIncident.value
        val isIncidentOngoing = activeIncident != null && (
                activeIncident.state == IncidentLifecycleState.SUSPECTED ||
                activeIncident.state == IncidentLifecycleState.AWAITING_CONFIRMATION ||
                activeIncident.state == IncidentLifecycleState.CONFIRMED ||
                activeIncident.state == IncidentLifecycleState.CONTACT_ALERTING ||
                activeIncident.state == IncidentLifecycleState.ACTIVE_ASSIST
        )

        // Deduplication Guard: If an emergency is already active, corroborate without duplicate dispatch
        if (isIncidentOngoing && activeIncident != null) {
            val timestampString = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val corroboratingSignal = "[$timestampString] Corroborating trigger from ${source.displayName}" +
                    (if (!detectedPhrase.isNullOrBlank()) ": \"$detectedPhrase\"" else "")

            val updatedIncident = activeIncident.copy(
                corroboratingSignals = activeIncident.corroboratingSignals + corroboratingSignal,
                location = location ?: activeIncident.location
            )
            _currentIncident.value = updatedIncident
            return Pair(updatedIncident, false)
        }

        // New Incident Creation: Generate unique ID and initial lifecycle state
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

    /**
     * Transitions an active incident to a new lifecycle state.
     */
    @Synchronized
    fun transitionState(
        incidentId: String,
        newState: IncidentLifecycleState,
        notes: String? = null
    ): EmergencyIncident? {
        val currentIncidentState = _currentIncident.value
        if (currentIncidentState == null || currentIncidentState.incidentId != incidentId) return null

        val updatedIncident = currentIncidentState.copy(
            state = newState,
            notes = if (notes != null) "${currentIncidentState.notes ?: ""}\n$notes".trim() else currentIncidentState.notes,
            resolvedTimeMillis = if (newState == IncidentLifecycleState.RESOLVED || newState == IncidentLifecycleState.CANCELLED) {
                System.currentTimeMillis()
            } else currentIncidentState.resolvedTimeMillis
        )

        _currentIncident.value = updatedIncident

        if (newState == IncidentLifecycleState.RESOLVED || newState == IncidentLifecycleState.CANCELLED) {
            archiveIncident(updatedIncident)
        }

        return updatedIncident
    }

    /**
     * Records the outcome of sending emergency SMS alerts to trusted contacts.
     */
    @Synchronized
    fun recordContactAlertResult(
        incidentId: String,
        attempted: Int,
        sent: Int
    ) {
        val currentIncidentState = _currentIncident.value
        if (currentIncidentState == null || currentIncidentState.incidentId != incidentId) return

        _currentIncident.value = currentIncidentState.copy(
            contactAlertsAttempted = attempted,
            contactAlertsSent = sent,
            state = IncidentLifecycleState.CONTACT_ALERTING
        )
    }

    /**
     * Records that an outgoing cellular emergency phone call was placed.
     */
    @Synchronized
    fun recordCallInitiated(
        incidentId: String,
        isPublicEmergency: Boolean,
        callTargetName: String
    ) {
        val currentIncidentState = _currentIncident.value
        if (currentIncidentState == null || currentIncidentState.incidentId != incidentId) return

        val callSignalLog = "Call placed to: $callTargetName (Public 911: $isPublicEmergency)"
        _currentIncident.value = currentIncidentState.copy(
            publicEmergencyCallInitiated = isPublicEmergency,
            corroboratingSignals = currentIncidentState.corroboratingSignals + callSignalLog,
            state = IncidentLifecycleState.ACTIVE_ASSIST
        )
    }

    /**
     * Aborts an active incident (e.g. user canceled during the 10-second countdown).
     */
    @Synchronized
    fun cancelIncident(incidentId: String, reason: String = "User Aborted") {
        transitionState(incidentId, IncidentLifecycleState.CANCELLED, "Cancelled: $reason")
    }

    /**
     * Marks an incident safely resolved once assistance has concluded.
     */
    @Synchronized
    fun resolveIncident(incidentId: String, reason: String = "Resolved") {
        transitionState(incidentId, IncidentLifecycleState.RESOLVED, "Resolution: $reason")
    }

    /**
     * Clears the active incident slot and archives it to historical memory.
     */
    @Synchronized
    fun clearActiveIncident() {
        val currentIncidentState = _currentIncident.value
        if (currentIncidentState != null) {
            archiveIncident(currentIncidentState.copy(resolvedTimeMillis = System.currentTimeMillis()))
        }
        _currentIncident.value = null
    }

    /**
     * Stores the incident in local memory history (keeps the most recent 50 incidents).
     */
    private fun archiveIncident(incident: EmergencyIncident) {
        val historyList = _incidentHistory.value.toMutableList()
        val existingIndex = historyList.indexOfFirst { it.incidentId == incident.incidentId }
        if (existingIndex >= 0) {
            historyList[existingIndex] = incident
        } else {
            historyList.add(0, incident)
        }

        // Limit memory retention to the last 50 incidents to prevent memory leaks
        if (historyList.size > 50) {
            historyList.removeAt(historyList.size - 1)
        }
        _incidentHistory.value = historyList
    }

    /**
     * Generates a human-readable incident tracking ID (e.g. "INC-20261111-A1B2C3").
     */
    private fun generateIncidentId(isSimulated: Boolean): String {
        val formattedDateString = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val randomUniqueCode = UUID.randomUUID().toString().substring(0, 6).uppercase(Locale.US)
        val prefix = if (isSimulated) "SIM" else "INC"
        return "$prefix-$formattedDateString-$randomUniqueCode"
    }
}
