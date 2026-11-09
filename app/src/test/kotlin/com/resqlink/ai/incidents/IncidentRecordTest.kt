package com.resqlink.ai.incidents

import org.junit.Assert.*
import org.junit.Test

class IncidentRecordTest {

    @Test
    fun testFormattedText_containsRequiredSections() {
        val record = IncidentRecord(
            incidentId = "INC-20261109-TEST01",
            triggerSource = "Voice AI Hotword",
            initialDetectedPhrase = "Resqlink Tulong!",
            finalState = "RESOLVED",
            startTimeMillis = 1731140000000L,
            resolvedTimeMillis = 1731140120000L,
            latitude = 14.6507,
            longitude = 121.0509,
            locationAccuracyMeters = 3.5f,
            contactsAttempted = 2,
            contactsSent = 2,
            timeline = listOf(
                IncidentTimelineEntry(1731140000000L, "SUSPECTED", "Acoustic keyword detected"),
                IncidentTimelineEntry(1731140010000L, "CONTACT_ALERTING", "SMS dispatched to 2 contacts")
            )
        )

        val report = record.toFormattedText()

        assertTrue(report.contains("INC-20261109-TEST01"))
        assertTrue(report.contains("Voice AI Hotword"))
        assertTrue(report.contains("Resqlink Tulong!"))
        assertTrue(report.contains("14.6507, 121.0509"))
        assertTrue(report.contains("2 / 2 contact(s)"))
        assertTrue(report.contains("Acoustic keyword detected"))
    }

    @Test
    fun testJsonSerialization_roundTripPreservesFields() {
        val original = IncidentRecord(
            incidentId = "INC-JSON-99",
            triggerSource = "One-Tap SOS Button",
            finalState = "CANCELLED",
            startTimeMillis = 1731145000000L,
            contactsAttempted = 1,
            contactsSent = 0
        )

        val jsonString = IncidentExporter.generateJsonReport(original)
        assertTrue(jsonString.contains("INC-JSON-99"))
        assertTrue(jsonString.contains("One-Tap SOS Button"))
    }
}
