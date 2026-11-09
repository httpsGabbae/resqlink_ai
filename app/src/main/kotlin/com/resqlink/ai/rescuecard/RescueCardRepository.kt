package com.resqlink.ai.rescuecard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Repository managing offline local storage for the Rescue Card.
 * Works completely offline without remote cloud synchronization.
 */
class RescueCardRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "resqlink_rescue_card_prefs"
        private const val KEY_PROFILE_JSON = "rescue_card_profile_json"

        @Volatile
        private var instance: RescueCardRepository? = null

        fun getInstance(context: Context): RescueCardRepository {
            return instance ?: synchronized(this) {
                instance ?: RescueCardRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<RescueProfile> = _profile.asStateFlow()

    private fun loadProfile(): RescueProfile {
        val storedJson = prefs.getString(KEY_PROFILE_JSON, null)
        if (!storedJson.isNullOrBlank()) {
            try {
                return json.decodeFromString<RescueProfile>(storedJson)
            } catch (e: Exception) {
                // Fallback to default if corrupted
            }
        }
        return RescueProfile()
    }

    @Synchronized
    fun saveProfile(profile: RescueProfile) {
        val serialized = json.encodeToString(profile)
        prefs.edit().putString(KEY_PROFILE_JSON, serialized).apply()
        _profile.value = profile
    }

    @Synchronized
    fun updateVisibility(
        isBloodTypeVisible: Boolean? = null,
        isAllergiesVisible: Boolean? = null,
        isMedicationsVisible: Boolean? = null,
        isConditionsVisible: Boolean? = null,
        isInstructionsVisible: Boolean? = null,
        isContactsVisible: Boolean? = null,
        isOrganDonorVisible: Boolean? = null
    ) {
        val current = _profile.value
        val updated = current.copy(
            isBloodTypeVisible = isBloodTypeVisible ?: current.isBloodTypeVisible,
            isAllergiesVisible = isAllergiesVisible ?: current.isAllergiesVisible,
            isMedicationsVisible = isMedicationsVisible ?: current.isMedicationsVisible,
            isConditionsVisible = isConditionsVisible ?: current.isConditionsVisible,
            isInstructionsVisible = isInstructionsVisible ?: current.isInstructionsVisible,
            isContactsVisible = isContactsVisible ?: current.isContactsVisible,
            isOrganDonorVisible = isOrganDonorVisible ?: current.isOrganDonorVisible
        )
        saveProfile(updated)
    }

    @Synchronized
    fun resetToDefault() {
        val defaultProfile = RescueProfile()
        saveProfile(defaultProfile)
    }
}
