package com.resqlink.ai.incidents

import android.content.Context
import com.resqlink.ai.data.local.HotlineDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * # IncidentRepository
 *
 * Manages the offline storage, retrieval, and deletion of emergency incident audit records.
 *
 * Ordinary Human Overview:
 * When an emergency occurs, ResQLink AI creates an audit record documenting what happened.
 * This repository:
 * 1. Executes all SQLite database queries safely on background threads (`Dispatchers.IO`).
 * 2. Emits an observable Kotlin `StateFlow` so the History/Incident UI screen updates live.
 * 3. Supports single-record lookups, deletion, and full history clearing.
 */
class IncidentRepository(private val databaseHelper: HotlineDatabaseHelper) {

    companion object {
        @Volatile
        private var singletonInstance: IncidentRepository? = null

        fun getInstance(context: Context): IncidentRepository {
            return singletonInstance ?: synchronized(this) {
                singletonInstance ?: IncidentRepository(HotlineDatabaseHelper(context.applicationContext)).also {
                    singletonInstance = it
                }
            }
        }
    }

    private val _incidents = MutableStateFlow<List<IncidentRecord>>(emptyList())
    val incidents: StateFlow<List<IncidentRecord>> = _incidents.asStateFlow()

    /**
     * Re-reads all saved incident records from the on-device SQLite database
     * and publishes them to reactive UI observers.
     */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        _incidents.value = databaseHelper.getAllIncidentLogs()
    }

    /**
     * Persists a new incident record to the SQLite database.
     *
     * @param record The completed or ongoing [IncidentRecord]
     * @return `true` if saved successfully, `false` otherwise
     */
    suspend fun saveIncident(record: IncidentRecord): Boolean = withContext(Dispatchers.IO) {
        val insertedRowId = databaseHelper.insertIncidentLog(record)
        if (insertedRowId != -1L) {
            _incidents.value = databaseHelper.getAllIncidentLogs()
            true
        } else {
            false
        }
    }

    /**
     * Retrieves an individual incident record by its unique ID string.
     */
    suspend fun getIncident(id: String): IncidentRecord? = withContext(Dispatchers.IO) {
        databaseHelper.getIncidentLogById(id)
    }

    /**
     * Deletes a specific incident record from the SQLite database.
     */
    suspend fun deleteIncident(id: String): Boolean = withContext(Dispatchers.IO) {
        val deletedRowCount = databaseHelper.deleteIncidentLog(id)
        val wasSuccessfullyDeleted = deletedRowCount > 0
        if (wasSuccessfullyDeleted) {
            _incidents.value = databaseHelper.getAllIncidentLogs()
        }
        wasSuccessfullyDeleted
    }

    /**
     * Deletes all incident records from the database table.
     */
    suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
        val clearedRowsCount = databaseHelper.clearAllIncidentLogs()
        _incidents.value = emptyList()
        clearedRowsCount >= 0
    }
}
