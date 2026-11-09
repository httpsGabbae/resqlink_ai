package com.resqlink.ai.incidents

import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.emergency.EmergencyTriggerSource
import com.resqlink.ai.emergency.IncidentLifecycleState
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class IncidentTimelineEntry(
    val timestampMillis: Long,
    val stage: String,
    val message: String
)

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
    val durationSeconds: Long
        get() = ((resolvedTimeMillis ?: System.currentTimeMillis()) - startTimeMillis) / 1000L

    fun toFormattedText(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("             RESQLINK INCIDENT REPORT             ")
        sb.appendLine("==================================================")
        sb.appendLine("Incident ID    : $incidentId")
        sb.appendLine("Trigger Source : $triggerSource")
        if (!initialDetectedPhrase.isNullOrBlank()) {
            sb.appendLine("Detected Phrase: \"$initialDetectedPhrase\"")
        }
        sb.appendLine("Final Status   : $finalState")
        sb.appendLine("Simulation Mode: ${if (isSimulated) "YES (Safe Dry-Run)" else "NO (Live Emergency)"}")
        sb.appendLine("Start Time     : ${sdf.format(Date(startTimeMillis))}")
        resolvedTimeMillis?.let {
            sb.appendLine("End/Resolution : ${sdf.format(Date(it))}")
        }
        sb.appendLine("Duration       : ${durationSeconds}s")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("GEOLOCATION")
        if (latitude != null && longitude != null) {
            sb.appendLine("Coordinates    : $latitude, $longitude")
            sb.appendLine("Accuracy       : ~${locationAccuracyMeters?.toInt() ?: "?"} meters")
            sb.appendLine("Map Link       : https://maps.google.com/?q=$latitude,$longitude")
        } else {
            sb.appendLine("Coordinates    : Not Available / Unavailable at time of incident")
        }
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("DISPATCH & ESCALATION")
        sb.appendLine("SMS Sent       : $contactsSent / $contactsAttempted contact(s)")
        sb.appendLine("Hotline Call   : ${if (emergencyCallInitiated) "User-Initiated Call Launched" else "No Emergency Call Launched"}")
        sb.appendLine("--------------------------------------------------")
        sb.appendLine("CHRONOLOGICAL EVENT TIMELINE")
        if (timeline.isEmpty()) {
            sb.appendLine(" No intermediate timeline events recorded.")
        } else {
            for (entry in timeline) {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.timestampMillis))
                sb.appendLine(" [$timeStr] [${entry.stage}] ${entry.message}")
            }
        }
        sb.appendLine("==================================================")
        return sb.toString()
    }
}
