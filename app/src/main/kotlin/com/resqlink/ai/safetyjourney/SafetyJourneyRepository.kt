package com.resqlink.ai.safetyjourney

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SafetyJourneyRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "resqlink_safety_journey_prefs"
        private const val KEY_ACTIVE_JOURNEY = "active_journey_json"
        private const val KEY_JOURNEY_HISTORY = "journey_history_json"

        @Volatile
        private var instance: SafetyJourneyRepository? = null

        fun getInstance(context: Context): SafetyJourneyRepository {
            return instance ?: synchronized(this) {
                instance ?: SafetyJourneyRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _activeJourney = MutableStateFlow<SafetyJourney?>(loadActiveJourney())
    val activeJourney: StateFlow<SafetyJourney?> = _activeJourney.asStateFlow()

    private val _journeyHistory = MutableStateFlow<List<SafetyJourney>>(loadHistory())
    val journeyHistory: StateFlow<List<SafetyJourney>> = _journeyHistory.asStateFlow()

    private fun loadActiveJourney(): SafetyJourney? {
        val stored = prefs.getString(KEY_ACTIVE_JOURNEY, null) ?: return null
        return try {
            json.decodeFromString<SafetyJourney>(stored)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadHistory(): List<SafetyJourney> {
        val stored = prefs.getString(KEY_JOURNEY_HISTORY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SafetyJourney>>(stored)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveActiveJourney(journey: SafetyJourney?) {
        if (journey != null) {
            val encoded = json.encodeToString(journey)
            prefs.edit().putString(KEY_ACTIVE_JOURNEY, encoded).apply()
        } else {
            prefs.edit().remove(KEY_ACTIVE_JOURNEY).apply()
        }
        _activeJourney.value = journey
    }

    @Synchronized
    fun archiveJourney(journey: SafetyJourney) {
        val history = (_journeyHistory.value + journey).takeLast(20)
        val encoded = json.encodeToString(history)
        prefs.edit().putString(KEY_JOURNEY_HISTORY, encoded).apply()
        _journeyHistory.value = history
        saveActiveJourney(null)
    }

    @Synchronized
    fun clearHistory() {
        prefs.edit().remove(KEY_JOURNEY_HISTORY).apply()
        _journeyHistory.value = emptyList()
    }
}
