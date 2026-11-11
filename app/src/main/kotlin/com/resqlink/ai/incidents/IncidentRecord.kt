package com.resqlink.ai.incidents

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * # IncidentTimelineEntry
 *
 * Represents an individual milestone or state change in an emergency incident timeline.
 *
 * @property timestampMillis Exact epoch millisecond when this milestone occurred
 * @property stage Short machine tag for the stage (e.g., "TRIGGER", "GPS_LOCK", "SMS_DISPATCH")
 * @property message Human-readable explanation of what the system did
 */
@Serializable
data class IncidentTimelineEntry(
    val timestampMillis: Long,
    val stage: String,
    val message: String
)

/**
 * # IncidentRecord
 *
 * Immutable data model capturing the complete post-incident audit trail of an emergency event.
 *
 * Ordinary Human Overview:
 * When an emergency occurs—or when a user performs a safe dry-run simulation—it is critical
 * to know exactly what happened, when it happened, which family members were alerted,
 * and what GPS coordinates were broadcast.
 *
 * This record:
 * 1. Stores complete telemetry (trigger source, voice phrase detected, duration, coordinates, accuracy).
 * 2. Distinguishes live emergencies from test simulations via [isSimulated].
 * 3. Keeps an exact chronological event log ([timeline]) for medical or forensic review.
 * 4. Generates formatted text summaries ready for export, sharing, or email.
 */
@Serializable
data class IncidentRecord(
    val incidentId: String,
    val triggerSource: String,
    val initialDetectedPhrase: String? = null,
    val finalState: String,
    val startTimeMillis: Long,
    val resolvedTimeMillis: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val isSimulated: Boolean = false,
    val contactsAttempted: Int = 0,
    val contactsSent: Int = 0,
    val emergencyCallInitiated: Boolean = false,
    val timeline: List<IncidentTimelineEntry> = emptyList(),
    val notes: String? = null
) {
    /**
     * Total elapsed duration of the incident in whole seconds.
     */
    val durationSeconds: Long
        get() {
            val endTimeMillis = resolvedTimeMillis ?: System.currentTimeMillis()
            return (endTimeMillis - startTimeMillis) / 1000L
        }

    /**
     * Formats the incident record into an ASCII-bordered, human-readable report.
     */
    fun toFormattedText(): String {
        val fullDateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
        val shortTimeFormatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        val reportBuilder = StringBuilder()

        reportBuilder.appendLine("==================================================")
        reportBuilder.appendLine("             RESQLINK INCIDENT REPORT             ")
        reportBuilder.appendLine("==================================================")
        reportBuilder.appendLine("Incident ID    : $incidentId")
        reportBuilder.appendLine("Trigger Source : $triggerSource")
        if (!initialDetectedPhrase.isNullOrBlank()) {
            reportBuilder.appendLine("Detected Phrase: \"$initialDetectedPhrase\"")
        }
        reportBuilder.appendLine("Final Status   : $finalState")
        reportBuilder.appendLine("Simulation Mode: ${if (isSimulated) "YES (Safe Dry-Run)" else "NO (Live Emergency)"}")
        reportBuilder.appendLine("Start Time     : ${fullDateTimeFormatter.format(Date(startTimeMillis))}")
        resolvedTimeMillis?.let { resolvedTimestamp ->
            reportBuilder.appendLine("End/Resolution : ${fullDateTimeFormatter.format(Date(resolvedTimestamp))}")
        }
        reportBuilder.appendLine("Duration       : ${durationSeconds}s")
        reportBuilder.appendLine("--------------------------------------------------")
        reportBuilder.appendLine("GEOLOCATION")
        if (latitude != null && longitude != null) {
            reportBuilder.appendLine("Coordinates    : $latitude, $longitude")
            reportBuilder.appendLine("Accuracy       : ~${locationAccuracyMeters?.toInt() ?: "?"} meters")
            reportBuilder.appendLine("Map Link       : https://maps.google.com/?q=$latitude,$longitude")
        } else {
            reportBuilder.appendLine("Coordinates    : Not Available / Unavailable at time of incident")
        }
        reportBuilder.appendLine("--------------------------------------------------")
        reportBuilder.appendLine("DISPATCH & ESCALATION")
        reportBuilder.appendLine("SMS Sent       : $contactsSent / $contactsAttempted contact(s)")
        reportBuilder.appendLine("Hotline Call   : ${if (emergencyCallInitiated) "User-Initiated Call Launched" else "No Emergency Call Launched"}")
        reportBuilder.appendLine("--------------------------------------------------")
        reportBuilder.appendLine("CHRONOLOGICAL EVENT TIMELINE")
        if (timeline.isEmpty()) {
            reportBuilder.appendLine(" No intermediate timeline events recorded.")
        } else {
            for (timelineEntry in timeline) {
                val timeString = shortTimeFormatter.format(Date(timelineEntry.timestampMillis))
                reportBuilder.appendLine(" [$timeString] [${timelineEntry.stage}] ${timelineEntry.message}")
            }
        }
        reportBuilder.appendLine("==================================================")
        return reportBuilder.toString()
    }
}
