package com.resqlink.ai.incidents

import android.content.Context
import android.content.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Handles exporting incident audit reports to formatted plain text or structured JSON,
 * and dispatching via standard Android Sharesheet (Intent.ACTION_SEND).
 */
object IncidentExporter {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun generateTextReport(record: IncidentRecord): String {
        return record.toFormattedText()
    }

    fun generateJsonReport(record: IncidentRecord): String {
        return json.encodeToString(record)
    }

    fun shareReport(context: Context, record: IncidentRecord, asJson: Boolean = false) {
        val content = if (asJson) generateJsonReport(record) else generateTextReport(record)
        val mimeType = if (asJson) "application/json" else "text/plain"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_SUBJECT, "ResQLink Incident Report - ${record.incidentId}")
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(sendIntent, "Share ResQLink Incident Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
