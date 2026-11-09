package com.resqlink.ai.readiness

enum class DiagnosticStatus(val label: String) {
    PASS("Operational"),
    WARNING("Needs Attention"),
    FAIL("Offline / Disabled"),
    NOT_TESTED("Not Tested"),
    UNAVAILABLE("Hardware / API Unsupported")
}

data class DiagnosticItem(
    val id: String,
    val category: String,
    val name: String,
    val status: DiagnosticStatus,
    val detail: String,
    val remediation: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ReadinessReport(
    val overallPass: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<DiagnosticItem>
)
