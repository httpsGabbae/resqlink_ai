package com.resqlink.ai.readiness

/**
 * # DiagnosticStatus
 *
 * Operational health classification for device emergency readiness checks.
 */
enum class DiagnosticStatus(val label: String) {
    PASS("Operational"),
    WARNING("Needs Attention"),
    FAIL("Offline / Disabled"),
    NOT_TESTED("Not Tested"),
    UNAVAILABLE("Hardware / API Unsupported")
}

/**
 * # DiagnosticItem
 *
 * Represents an individual subsystem health evaluation (e.g., GPS, Microphone, SMS, Sensors).
 *
 * @property id Unique programmatic identifier
 * @property category Broad subsystem category (e.g., "Sensors", "Telephony", "Power")
 * @property name Human-readable name of the hardware or software component tested
 * @property status Current diagnostic outcome
 * @property detail Technical or practical explanation of the current state
 * @property remediation Plain-English instruction on how to fix the issue if WARNING or FAIL
 * @property timestamp Epoch millisecond when the check was performed
 */
data class DiagnosticItem(
    val id: String,
    val category: String,
    val name: String,
    val status: DiagnosticStatus,
    val detail: String,
    val remediation: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * # ReadinessReport
 *
 * Comprehensive diagnostic assessment summarizing device preparedness for real-world emergencies.
 *
 * @property overallPass `true` if all critical safety systems (GPS, SMS, Microphone, Contacts) are operational
 * @property timestamp Epoch millisecond when the overall scan completed
 * @property items Granular list of all individual subsystem diagnostic items
 */
data class ReadinessReport(
    val overallPass: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<DiagnosticItem>
)
