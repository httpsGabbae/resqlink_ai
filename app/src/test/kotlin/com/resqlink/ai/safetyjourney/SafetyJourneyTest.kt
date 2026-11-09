package com.resqlink.ai.safetyjourney

import org.junit.Assert.*
import org.junit.Test

class SafetyJourneyTest {

    @Test
    fun testJourneyCreation_calculatesDeadlinesProperly() {
        val now = System.currentTimeMillis()
        val journey = SafetyJourney(
            id = "JRN-TEST1234",
            destination = "University of the Philippines",
            startTimeMillis = now,
            targetDurationMinutes = 30,
            gracePeriodMinutes = 5
        )

        assertEquals("JRN-TEST1234", journey.id)
        assertEquals("University of the Philippines", journey.destination)
        assertEquals(now + (30 * 60 * 1000L), journey.deadlineMillis)
        assertEquals(now + (35 * 60 * 1000L), journey.graceDeadlineMillis)
        assertEquals(JourneyStatus.ACTIVE, journey.status)
        assertFalse(journey.isOverdue)
        assertFalse(journey.isGracePeriodExpired)
    }

    @Test
    fun testJourneyOverdue_whenCurrentTimePassesDeadline() {
        val pastStart = System.currentTimeMillis() - (40 * 60 * 1000L)
        val journey = SafetyJourney(
            id = "JRN-OVERDUE",
            destination = "Market Market Taguig",
            startTimeMillis = pastStart,
            targetDurationMinutes = 30,
            gracePeriodMinutes = 5
        )

        assertTrue(journey.isOverdue)
        assertTrue(journey.isGracePeriodExpired)
        assertEquals(0L, journey.remainingTimeMillis)
    }

    @Test
    fun testJourneyInGracePeriod_whenOverdueBeforeGraceDeadline() {
        val start = System.currentTimeMillis() - (32 * 60 * 1000L)
        val journey = SafetyJourney(
            id = "JRN-GRACE",
            destination = "Cubao Station",
            startTimeMillis = start,
            targetDurationMinutes = 30,
            gracePeriodMinutes = 5
        )

        assertTrue(journey.isOverdue)
        assertFalse(journey.isGracePeriodExpired)
    }
}
