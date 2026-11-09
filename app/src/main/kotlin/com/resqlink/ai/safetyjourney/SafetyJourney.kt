package com.resqlink.ai.safetyjourney

import kotlinx.serialization.Serializable

enum class JourneyStatus(val displayLabel: String) {
    IDLE("No Active Journey"),
    ACTIVE("Journey Active"),
    GRACE_PERIOD("Check-in Overdue (Grace Period)"),
    RESOLVED_SAFE("Completed Safely"),
    ESCALATED_CONTACTS("Escalated to Trusted Contacts"),
    CANCELLED("Journey Cancelled")
}

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
    val remainingTimeMillis: Long
        get() = (deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)

    val isOverdue: Boolean
        get() = System.currentTimeMillis() > deadlineMillis

    val isGracePeriodExpired: Boolean
        get() = System.currentTimeMillis() > graceDeadlineMillis
}
