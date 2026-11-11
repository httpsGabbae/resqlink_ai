package com.resqlink.ai.rescuecard

import org.junit.Assert.*
import org.junit.Test

class RescueProfileTest {

    @Test
    fun testSanitizedSummary_includesOnlyPermittedFields() {
        val profile = RescueProfile(
            fullName = "Alex Vance",
            bloodType = "O+",
            allergies = "Penicillin",
            medications = "Metformin 500mg",
            medicalConditions = "Asthma",
            emergencyInstructions = "Epipen in left pocket",
            isBloodTypeVisible = true,
            isAllergiesVisible = true,
            isMedicationsVisible = false, // Private
            isConditionsVisible = false   // Private
        )

        val summary = profile.toSanitizedEmergencySummary()

        assertTrue(summary.contains("Alex Vance"))
        assertTrue(summary.contains("O+"))
        assertTrue(summary.contains("Penicillin"))
        assertFalse(summary.contains("Metformin"))
        assertFalse(summary.contains("Asthma"))
    }

    @Test
    fun testQrPayload_formatsCompactKeyValue() {
        val profile = RescueProfile(
            fullName = "Taylor Morgan",
            bloodType = "B+",
            allergies = "None",
            medicalConditions = "Hypertension",
            primaryContactName = "Morgan Lee",
            primaryContactPhone = "0917-123-4567"
        )

        val qr = profile.toQrPayload()

        assertTrue(qr.contains("app=ResQLink"))
        assertTrue(qr.contains("name=Taylor Morgan"))
        assertTrue(qr.contains("blood=B+"))
        assertTrue(qr.contains("ice1=Morgan Lee:0917-123-4567"))
    }
}
