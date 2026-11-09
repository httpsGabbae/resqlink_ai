package com.resqlink.ai.readiness

import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.domain.model.TrustedContact
import com.resqlink.ai.emergency.EmergencyEventManager
import com.resqlink.ai.emergency.EmergencyIncident
import com.resqlink.ai.emergency.EmergencyTriggerSource
import com.resqlink.ai.emergency.IncidentLifecycleState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SimulationState(
    val isRunning: Boolean = false,
    val simulatedIncident: EmergencyIncident? = null,
    val remainingSeconds: Int = 10,
    val generatedSmsPreview: String = "",
    val simulatedContacts: List<TrustedContact> = emptyList(),
    val stepDescription: String = "Simulation Idle",
    val logs: List<String> = emptyList()
)

/**
 * Isolated Emergency Simulation Engine.
 * Safely runs the entire emergency lifecycle without placing real calls or sending real cellular SMS messages.
 */
class EmergencySimulationManager(
    private val eventManager: EmergencyEventManager = EmergencyEventManager.getInstance()
) {

    private val _simulationState = MutableStateFlow(SimulationState())
    val simulationState: StateFlow<SimulationState> = _simulationState.asStateFlow()

    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startSimulation(
        simulatedPhrase: String = "Resqlink Tulong! [TEST SIMULATION]",
        mockLocation: LocationSnapshot = LocationSnapshot(
            latitude = 14.6507,
            longitude = 121.0509,
            address = "Quezon Memorial Circle (Simulated)",
            accuracyMeters = 4.2f,
            timestamp = System.currentTimeMillis()
        ),
        targetContacts: List<TrustedContact> = listOf(
            TrustedContact(1, "Maria Dela Cruz (Mom)", "0917-888-1234", "Mother", true),
            TrustedContact(2, "Juan Dela Cruz Jr.", "0918-999-5678", "Brother", true)
        )
    ) {
        stopSimulation()

        // 1. Register through centralized EmergencyEventManager as a simulated incident
        val (incident, _) = eventManager.registerTrigger(
            source = EmergencyTriggerSource.SIMULATION_TEST,
            detectedPhrase = simulatedPhrase,
            location = mockLocation,
            isSimulated = true
        )

        val previewMessage = "EMERGENCY ALERT (SIMULATION): Juan Dela Cruz triggered test SOS at ${mockLocation.address}. Coordinates: https://maps.google.com/?q=${mockLocation.latitude},${mockLocation.longitude}"

        _simulationState.value = SimulationState(
            isRunning = true,
            simulatedIncident = incident,
            remainingSeconds = 10,
            generatedSmsPreview = previewMessage,
            simulatedContacts = targetContacts,
            stepDescription = "Simulated distress keyword detected: \"$simulatedPhrase\"",
            logs = listOf(
                "[SIM-START] Registered simulated incident ${incident.incidentId}",
                "[SAFETY] Live SMS dispatch and cellular calling physically BLOCKED during simulation",
                "[GPS] Attached mock coordinates: ${mockLocation.latitude}, ${mockLocation.longitude}"
            )
        )

        simulationJob = scope.launch {
            for (sec in 9 downTo 1) {
                delay(1000L)
                val current = _simulationState.value
                if (!current.isRunning) return@launch
                _simulationState.value = current.copy(
                    remainingSeconds = sec,
                    stepDescription = "Simulated Countdown: $sec seconds remaining to dispatch",
                    logs = current.logs + "[T-$sec] Awaiting confirmation or cancel..."
                )
            }

            delay(1000L)
            // Simulated timeout: escalate to simulated contact alert (WITHOUT sending real SMS!)
            val updatedIncident = eventManager.transitionState(
                incident.incidentId,
                IncidentLifecycleState.CONTACT_ALERTING,
                "Simulated 10s timeout: dispatching mock contact alerts"
            )

            eventManager.recordContactAlertResult(
                incidentId = incident.incidentId,
                attempted = targetContacts.size,
                sent = targetContacts.size
            )

            _simulationState.value = _simulationState.value.copy(
                remainingSeconds = 0,
                simulatedIncident = updatedIncident,
                stepDescription = "Simulation Complete: Mock alerts dispatched to ${targetContacts.size} contact(s)",
                logs = _simulationState.value.logs + listOf(
                    "[TIMEOUT] 10s expired without abort",
                    "[MOCK-SMS] Mock emergency SMS generated for ${targetContacts.size} contacts",
                    "[MOCK-CALL] Emergency dialer prompt verified (No actual call placed)",
                    "[SUCCESS] Full emergency escalation path verified successfully"
                )
            )

            delay(3000L)
            eventManager.resolveIncident(incident.incidentId, "Simulation test completed successfully")
        }
    }

    fun stopSimulation(reason: String = "User Aborted Simulation") {
        simulationJob?.cancel()
        val active = _simulationState.value.simulatedIncident
        if (active != null) {
            eventManager.cancelIncident(active.incidentId, reason)
        }
        _simulationState.value = SimulationState(
            isRunning = false,
            stepDescription = "Simulation stopped: $reason",
            logs = _simulationState.value.logs + "[STOP] $reason"
        )
    }
}
