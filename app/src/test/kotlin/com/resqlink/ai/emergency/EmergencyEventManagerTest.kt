package com.resqlink.ai.emergency

import com.resqlink.ai.domain.model.LocationSnapshot
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EmergencyEventManagerTest {

    private lateinit var manager: EmergencyEventManager

    @Before
    fun setUp() {
        manager = EmergencyEventManager.getInstance()
        manager.clearActiveIncident()
    }

    @Test
    fun testRegisterVoiceTrigger_createsAwaitingConfirmationIncident() {
        val (incident, isNew) = manager.registerTrigger(
            source = EmergencyTriggerSource.VOICE_KEYWORD,
            detectedPhrase = "Resqlink Tulong!"
        )

        assertTrue(isNew)
        assertNotNull(incident)
        assertTrue(incident.incidentId.startsWith("INC-"))
        assertEquals(EmergencyTriggerSource.VOICE_KEYWORD, incident.triggerSource)
        assertEquals(IncidentLifecycleState.AWAITING_CONFIRMATION, incident.state)
        assertEquals("Resqlink Tulong!", incident.initialDetectedPhrase)
    }

    @Test
    fun testRegisterManualSos_createsConfirmedIncident() {
        val (incident, isNew) = manager.registerTrigger(
            source = EmergencyTriggerSource.MANUAL_SOS_BUTTON,
            detectedPhrase = "One-Tap SOS"
        )

        assertTrue(isNew)
        assertEquals(IncidentLifecycleState.CONFIRMED, incident.state)
    }

    @Test
    fun testCorroboratingTrigger_deduplicatesWithoutDuplicateIncident() {
        val (firstIncident, isNewFirst) = manager.registerTrigger(
            source = EmergencyTriggerSource.VOICE_KEYWORD,
            detectedPhrase = "Resqlink Tulong!"
        )
        assertTrue(isNewFirst)

        // Second trigger occurs while first is ongoing
        val (secondIncident, isNewSecond) = manager.registerTrigger(
            source = EmergencyTriggerSource.MANUAL_SOS_BUTTON,
            detectedPhrase = "Manual SOS pressed"
        )

        assertFalse(isNewSecond)
        assertEquals(firstIncident.incidentId, secondIncident.incidentId)
        assertTrue(secondIncident.corroboratingSignals.size >= 2)
    }

    @Test
    fun testSimulationTrigger_usesSimPrefix() {
        val (incident, isNew) = manager.registerTrigger(
            source = EmergencyTriggerSource.SIMULATION_TEST,
            detectedPhrase = "Safe Dry Run",
            isSimulated = true
        )

        assertTrue(isNew)
        assertTrue(incident.isSimulated)
        assertTrue(incident.incidentId.startsWith("SIM-"))
    }

    @Test
    fun testCancelIncident_transitionsStateAndArchives() {
        val (incident, _) = manager.registerTrigger(
            source = EmergencyTriggerSource.VOICE_KEYWORD,
            detectedPhrase = "False Alarm"
        )

        manager.cancelIncident(incident.incidentId, "User pressed cancel")

        val current = manager.currentIncident.value
        assertNotNull(current)
        assertEquals(IncidentLifecycleState.CANCELLED, current?.state)
        assertNotNull(current?.resolvedTimeMillis)
        assertTrue(manager.incidentHistory.value.any { it.incidentId == incident.incidentId })
    }
}
