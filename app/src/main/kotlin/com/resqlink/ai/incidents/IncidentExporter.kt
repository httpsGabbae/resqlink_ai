package com.resqlink.ai.incidents

import android.content.Context
import android.content.Intent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * # IncidentExporter
 *
 * Exports emergency incident audit logs into structured JSON or formatted plain-text documents,
 * and launches the standard Android system share sheet (`ACTION_SEND`).
 *
 * Ordinary Human Overview:
 * After an emergency or safety drill, users or first responders may need to email, save,
 * or print an incident report.
 *
 * This exporter:
 * 1. Formats the data as either human-readable ASCII text or computer-readable pretty-printed JSON.
 * 2. Launches the native Android Share dialog so the user can send the report via Gmail,
 *    Messaging, Google Drive, or Bluetooth.
 */
object IncidentExporter {

    private val prettyPrintJsonSerializer: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Converts the incident record into an ASCII-formatted text report.
     */
    fun generateTextReport(record: IncidentRecord): String {
        return record.toFormattedText()
    }

    /**
     * Serializes the incident record into pretty-printed, indented JSON.
     */
    fun generateJsonReport(record: IncidentRecord): String {
        return prettyPrintJsonSerializer.encodeToString(record)
    }

    /**
     * Opens the native Android Sharesheet so the user can share the incident report.
     *
     * @param context Android application or activity context
     * @param record The incident record to export
     * @param asJson If `true`, exports as `.json` mime type; if `false`, exports as plain text
     */
    fun shareReport(context: Context, record: IncidentRecord, asJson: Boolean = false) {
        val reportContent = if (asJson) generateJsonReport(record) else generateTextReport(record)
        val reportMimeType = if (asJson) "application/json" else "text/plain"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = reportMimeType
            putExtra(Intent.EXTRA_SUBJECT, "ResQLink Incident Report - ${record.incidentId}")
            putExtra(Intent.EXTRA_TEXT, reportContent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val shareChooserIntent = Intent.createChooser(sendIntent, "Share ResQLink Incident Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareChooserIntent)
    }
}
