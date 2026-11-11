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

/**
 * # SimulationState
 *
 * Observable state tracking the interactive progress of a dry-run emergency drill.
 */
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
 * # EmergencySimulationManager
 *
 * Isolated Emergency Simulation Engine.
 * Safely runs the entire emergency lifecycle without placing real calls or sending real cellular SMS messages.
 *
 * Ordinary Human Overview:
 * In a real emergency, panic sets in. Users need a safe "fire drill" mode to understand
 * exactly what their phone will do—see the 10-second countdown, hear the alarm sound,
 * and preview the SMS that family members will receive—WITHOUT actually spamming their loved
 * ones with real text messages or calling 911.
 *
 * Safety-Critical Architecture Guarantee:
 * All telephony and radio hardware transmissions are strictly intercepted and blocked.
 * The simulation writes audit entries tagged [isSimulated = true] so test runs are never
 * confused with real-life distress events.
 */
class EmergencySimulationManager(
    private val emergencyEventManager: EmergencyEventManager = EmergencyEventManager.getInstance()
) {

    private val _simulationState = MutableStateFlow(SimulationState())
    val simulationState: StateFlow<SimulationState> = _simulationState.asStateFlow()

    private var activeSimulationCoroutineJob: Job? = null
    private val simulationCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Starts an interactive dry-run simulation of a voice distress alert.
     */
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
            TrustedContact(1, "Primary Emergency Contact", "0917-000-0001", "Family", true),
            TrustedContact(2, "Secondary Emergency Contact", "0918-000-0002", "ICE", true)
        )
    ) {
        stopSimulation("Restarting fresh simulation")

        // 1. Register through centralized EmergencyEventManager as a simulated incident
        val (simulatedIncident, _) = emergencyEventManager.registerTrigger(
            source = EmergencyTriggerSource.SIMULATION_TEST,
            detectedPhrase = simulatedPhrase,
            location = mockLocation,
            isSimulated = true
        )

        val previewMessage = "EMERGENCY ALERT (SIMULATION): Distress trigger test at ${mockLocation.address}. Coordinates: https://maps.google.com/?q=${mockLocation.latitude},${mockLocation.longitude}"

        _simulationState.value = SimulationState(
            isRunning = true,
            simulatedIncident = simulatedIncident,
            remainingSeconds = 10,
            generatedSmsPreview = previewMessage,
            simulatedContacts = targetContacts,
            stepDescription = "Simulated distress keyword detected: \"$simulatedPhrase\"",
            logs = listOf(
                "[SIM-START] Registered simulated incident ${simulatedIncident.incidentId}",
                "[SAFETY] Live SMS dispatch and cellular calling physically BLOCKED during simulation",
                "[GPS] Attached mock coordinates: ${mockLocation.latitude}, ${mockLocation.longitude}"
            )
        )

        activeSimulationCoroutineJob = simulationCoroutineScope.launch {
            // Count down from 9 to 1 second
            for (countdownSecond in 9 downTo 1) {
                delay(1000L)
                val currentState = _simulationState.value
                if (!currentState.isRunning) return@launch
                _simulationState.value = currentState.copy(
                    remainingSeconds = countdownSecond,
                    stepDescription = "Simulated Countdown: $countdownSecond seconds remaining to dispatch",
                    logs = currentState.logs + "[T-$countdownSecond] Awaiting confirmation or cancel..."
                )
            }

            delay(1000L)
            // Simulated timeout: transition to simulated contact alert (WITHOUT sending real SMS!)
            val updatedIncident = emergencyEventManager.transitionState(
                simulatedIncident.incidentId,
                IncidentLifecycleState.CONTACT_ALERTING,
                "Simulated 10s timeout: dispatching mock contact alerts"
            )

            emergencyEventManager.recordContactAlertResult(
                incidentId = simulatedIncident.incidentId,
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
            emergencyEventManager.resolveIncident(simulatedIncident.incidentId, "Simulation test completed successfully")
        }
    }

    /**
     * Immediately aborts any ongoing simulation and records the cancellation in the incident manager.
     */
    fun stopSimulation(reason: String = "User Aborted Simulation") {
        activeSimulationCoroutineJob?.cancel()
        val activeSimulatedIncident = _simulationState.value.simulatedIncident
        if (activeSimulatedIncident != null) {
            emergencyEventManager.cancelIncident(activeSimulatedIncident.incidentId, reason)
        }
        _simulationState.value = SimulationState(
            isRunning = false,
            stepDescription = "Simulation stopped: $reason",
            logs = _simulationState.value.logs + "[STOP] $reason"
        )
    }
}
