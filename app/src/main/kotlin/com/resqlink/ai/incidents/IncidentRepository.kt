package com.resqlink.ai.incidents

import android.content.Context
import com.resqlink.ai.data.local.HotlineDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Offline repository for managing incident records and audit logs.
 */
class IncidentRepository(private val dbHelper: HotlineDatabaseHelper) {

    companion object {
        @Volatile
        private var instance: IncidentRepository? = null

        fun getInstance(context: Context): IncidentRepository {
            return instance ?: synchronized(this) {
                instance ?: IncidentRepository(HotlineDatabaseHelper(context.applicationContext)).also {
                    instance = it
                }
            }
        }
    }

    private val _incidents = MutableStateFlow<List<IncidentRecord>>(emptyList())
    val incidents: StateFlow<List<IncidentRecord>> = _incidents.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _incidents.value = dbHelper.getAllIncidentLogs()
    }

    suspend fun saveIncident(record: IncidentRecord): Boolean = withContext(Dispatchers.IO) {
        val rowId = dbHelper.insertIncidentLog(record)
        if (rowId != -1L) {
            _incidents.value = dbHelper.getAllIncidentLogs()
            true
        } else false
    }

    suspend fun getIncident(id: String): IncidentRecord? = withContext(Dispatchers.IO) {
        dbHelper.getIncidentLogById(id)
    }

    suspend fun deleteIncident(id: String): Boolean = withContext(Dispatchers.IO) {
        val deleted = dbHelper.deleteIncidentLog(id) > 0
        if (deleted) {
            _incidents.value = dbHelper.getAllIncidentLogs()
        }
        deleted
    }

    suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
        val count = dbHelper.clearAllIncidentLogs()
        _incidents.value = emptyList()
        count >= 0
    }
}
