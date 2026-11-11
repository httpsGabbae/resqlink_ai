package com.resqlink.ai.rescuecard

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * # RescueCardRepository
 *
 * Repository managing offline local storage for the user's emergency medical profile.
 *
 * Ordinary Human Overview:
 * All medical information stored in the Rescue Card stays strictly on the physical device.
 * No health data is ever transmitted to a cloud server, ensuring 100% privacy and complete
 * offline availability even without a cellular network or Wi-Fi connection.
 */
class RescueCardRepository(context: Context) {

    companion object {
        private const val PREFERENCES_FILE_NAME = "resqlink_rescue_card_prefs"
        private const val KEY_PROFILE_JSON = "rescue_card_profile_json"

        @Volatile
        private var singletonInstance: RescueCardRepository? = null

        fun getInstance(context: Context): RescueCardRepository {
            return singletonInstance ?: synchronized(this) {
                singletonInstance ?: RescueCardRepository(context.applicationContext).also { singletonInstance = it }
            }
        }
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    private val jsonSerializer: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _profile = MutableStateFlow(loadProfileFromDisk())
    val profile: StateFlow<RescueProfile> = _profile.asStateFlow()

    /**
     * Loads the saved Rescue Profile JSON from persistent storage.
     * Returns an empty profile if no data exists or if parsing fails.
     */
    private fun loadProfileFromDisk(): RescueProfile {
        val storedJsonString = sharedPreferences.getString(KEY_PROFILE_JSON, null)
        if (!storedJsonString.isNullOrBlank()) {
            try {
                return jsonSerializer.decodeFromString<RescueProfile>(storedJsonString)
            } catch (exception: Exception) {
                // Fallback to default empty profile if JSON data is corrupted
            }
        }
        return RescueProfile()
    }

    /**
     * Saves the updated Rescue Profile to private disk storage and notifies UI observers.
     */
    @Synchronized
    fun saveProfile(profile: RescueProfile) {
        val serializedJsonString = jsonSerializer.encodeToString(profile)
        sharedPreferences.edit().putString(KEY_PROFILE_JSON, serializedJsonString).apply()
        _profile.value = profile
    }

    /**
     * Atomically toggles specific field visibility preferences without altering medical data.
     */
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
        val currentProfile = _profile.value
        val updatedProfile = currentProfile.copy(
            isBloodTypeVisible = isBloodTypeVisible ?: currentProfile.isBloodTypeVisible,
            isAllergiesVisible = isAllergiesVisible ?: currentProfile.isAllergiesVisible,
            isMedicationsVisible = isMedicationsVisible ?: currentProfile.isMedicationsVisible,
            isConditionsVisible = isConditionsVisible ?: currentProfile.isConditionsVisible,
            isInstructionsVisible = isInstructionsVisible ?: currentProfile.isInstructionsVisible,
            isContactsVisible = isContactsVisible ?: currentProfile.isContactsVisible,
            isOrganDonorVisible = isOrganDonorVisible ?: currentProfile.isOrganDonorVisible
        )
        saveProfile(updatedProfile)
    }

    /**
     * Resets the Rescue Profile to a blank default state.
     */
    @Synchronized
    fun resetToDefault() {
        val defaultBlankProfile = RescueProfile()
        saveProfile(defaultBlankProfile)
    }
}
