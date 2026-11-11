package com.resqlink.ai.safetyjourney

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * # SafetyJourneyRepository
 *
 * Persists and streams the user's active travel session and historical journey logs.
 *
 * Ordinary Human Overview:
 * If your phone battery dies, or if Android kills the app to reclaim memory while you are
 * in a taxi, your active safety session cannot be lost.
 *
 * This repository:
 * 1. Serializes the active [SafetyJourney] directly to private device storage (`SharedPreferences`) as JSON.
 * 2. Restores the exact deadline timestamps immediately upon app restart.
 * 3. Exposes reactive Kotlin `StateFlow` streams so Compose UI widgets update automatically
 *    whenever a journey begins, extends, or completes.
 * 4. Maintains a rotating historical log of the last 20 completed or escalated journeys.
 */
class SafetyJourneyRepository(context: Context) {

    companion object {
        private const val PREFERENCES_FILE_NAME = "resqlink_safety_journey_prefs"
        private const val KEY_ACTIVE_JOURNEY_JSON = "active_journey_json"
        private const val KEY_JOURNEY_HISTORY_JSON = "journey_history_json"
        private const val MAXIMUM_HISTORY_RECORDS = 20

        @Volatile
        private var singletonInstance: SafetyJourneyRepository? = null

        fun getInstance(context: Context): SafetyJourneyRepository {
            return singletonInstance ?: synchronized(this) {
                singletonInstance ?: SafetyJourneyRepository(context.applicationContext).also { singletonInstance = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private val jsonSerializer: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _activeJourney = MutableStateFlow<SafetyJourney?>(loadActiveJourneyFromDisk())
    val activeJourney: StateFlow<SafetyJourney?> = _activeJourney.asStateFlow()

    private val _journeyHistory = MutableStateFlow<List<SafetyJourney>>(loadJourneyHistoryFromDisk())
    val journeyHistory: StateFlow<List<SafetyJourney>> = _journeyHistory.asStateFlow()

    /**
     * Reads and decodes the current active journey from persistent disk storage.
     */
    private fun loadActiveJourneyFromDisk(): SafetyJourney? {
        val storedJson = sharedPreferences.getString(KEY_ACTIVE_JOURNEY_JSON, null) ?: return null
        return try {
            jsonSerializer.decodeFromString<SafetyJourney>(storedJson)
        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Reads and decodes the past journey history list from persistent disk storage.
     */
    private fun loadJourneyHistoryFromDisk(): List<SafetyJourney> {
        val storedJson = sharedPreferences.getString(KEY_JOURNEY_HISTORY_JSON, null) ?: return emptyList()
        return try {
            jsonSerializer.decodeFromString<List<SafetyJourney>>(storedJson)
        } catch (exception: Exception) {
            emptyList()
        }
    }

    /**
     * Writes or clears the active journey in persistent storage and notifies UI observers.
     */
    @Synchronized
    fun saveActiveJourney(journey: SafetyJourney?) {
        if (journey != null) {
            val encodedJsonString = jsonSerializer.encodeToString(journey)
            sharedPreferences.edit().putString(KEY_ACTIVE_JOURNEY_JSON, encodedJsonString).apply()
        } else {
            sharedPreferences.edit().remove(KEY_ACTIVE_JOURNEY_JSON).apply()
        }
        _activeJourney.value = journey
    }

    /**
     * Moves a completed or escalated journey into the historical archive and clears active status.
     */
    @Synchronized
    fun archiveJourney(journey: SafetyJourney) {
        val updatedHistoryList = (_journeyHistory.value + journey).takeLast(MAXIMUM_HISTORY_RECORDS)
        val encodedHistoryJson = jsonSerializer.encodeToString(updatedHistoryList)
        sharedPreferences.edit().putString(KEY_JOURNEY_HISTORY_JSON, encodedHistoryJson).apply()
        _journeyHistory.value = updatedHistoryList
        saveActiveJourney(null)
    }

    /**
     * Clears all saved historical journey logs from disk.
     */
    @Synchronized
    fun clearHistory() {
        sharedPreferences.edit().remove(KEY_JOURNEY_HISTORY_JSON).apply()
        _journeyHistory.value = emptyList()
    }
}
