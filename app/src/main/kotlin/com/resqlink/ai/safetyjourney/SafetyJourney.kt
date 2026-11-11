package com.resqlink.ai.safetyjourney

import kotlinx.serialization.Serializable

/**
 * # JourneyStatus
 *
 * Tracks the progress of a scheduled commute or travel safety session.
 */
enum class JourneyStatus(val displayLabel: String) {
    /** No travel monitoring active. */
    IDLE("No Active Journey"),

    /** The user is currently traveling; the countdown timer is running normally. */
    ACTIVE("Journey Active"),

    /** The expected arrival time passed. A 5-minute audible grace period is counting down. */
    GRACE_PERIOD("Check-in Overdue (Grace Period)"),

    /** The user reached their destination and confirmed they arrived safely. */
    RESOLVED_SAFE("Completed Safely"),

    /** The grace period expired without a response; an SMS alert was dispatched to contacts. */
    ESCALATED_CONTACTS("Escalated to Trusted Contacts"),

    /** The user aborted or stopped travel monitoring before the deadline. */
    CANCELLED("Journey Cancelled")
}

/**
 * # SafetyJourney
 *
 * Data model for a timed travel safety session.
 *
 * Ordinary Human Overview:
 * When riding a late-night taxi, walking home alone, or commuting through unfamiliar roads,
 * a user can start a Safety Journey (e.g. "Heading home, 25 minutes"):
 * 1. An exact hardware alarm deadline is scheduled in the phone's operating system.
 * 2. If the user arrives and taps "I Have Arrived Safely", the session ends cleanly.
 * 3. If the user does NOT check in on time, a 5-minute audible grace period countdown begins.
 * 4. If still unanswered, ResQLink automatically alerts trusted contacts with the user's last known location.
 *
 * Safety Invariant:
 * A missed check-in NEVER automatically calls 911. It escalates ONLY to authorized trusted contacts via SMS.
 *
 * @property id Unique session identifier.
 * @property destination Human label for where the user is heading (e.g. "Home", "Office", "Dorm").
 * @property notes Optional details (e.g. "Taxi plate ABC-1234").
 * @property startTimeMillis Epoch time in milliseconds when the commute started.
 * @property targetDurationMinutes Scheduled trip duration in minutes (e.g. 15, 30, 45).
 * @property deadlineMillis Exact epoch time when the trip is expected to finish.
 * @property gracePeriodMinutes Additional minutes given before escalating to contacts (default: 5 min).
 * @property graceDeadlineMillis Exact epoch time when trusted contacts will be alerted.
 * @property status Current progress state of the journey.
 * @property extensionCount Number of times the user tapped "+15 mins" due to traffic.
 * @property completedTimeMillis Epoch time when the trip was safely completed.
 * @property escalationReason Reason recorded if the session had to alert contacts.
 */
@Serializable
data class SafetyJourney(
    val id: String,
    val destination: String = "Home",
    val notes: String = "",
    val startTimeMillis: Long = System.currentTimeMillis(),
    val targetDurationMinutes: Int = 30,
    val deadlineMillis: Long = startTimeMillis + (targetDurationMinutes * 60 * 1000L),
    val gracePeriodMinutes: Int = 5,
    val graceDeadlineMillis: Long = deadlineMillis + (gracePeriodMinutes * 60 * 1000L),
    val status: JourneyStatus = JourneyStatus.ACTIVE,
    val extensionCount: Int = 0,
    val completedTimeMillis: Long? = null,
    val escalationReason: String? = null
) {
    /**
     * Milliseconds remaining until the planned arrival deadline.
     */
    val remainingTimeMillis: Long
        get() = (deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)

    /**
     * True if the planned arrival time has passed.
     */
    val isOverdue: Boolean
        get() = System.currentTimeMillis() > deadlineMillis

    /**
     * True if both the journey deadline AND the 5-minute grace period have elapsed.
     */
    val isGracePeriodExpired: Boolean
        get() = System.currentTimeMillis() > graceDeadlineMillis
}
