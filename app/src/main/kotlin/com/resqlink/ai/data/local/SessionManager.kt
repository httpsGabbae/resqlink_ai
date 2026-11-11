package com.resqlink.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.resqlink.ai.domain.model.CallPriority
import com.resqlink.ai.domain.model.SimPreference
import com.resqlink.ai.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * # SessionManager
 *
 * Manages the user's local session, medical profile, and app settings using Android's
 * private [SharedPreferences].
 *
 * Ordinary Human Overview:
 * When you change a setting in the app (such as setting Call Priority to "Family First",
 * turning on Guardian Mode, or updating your blood type), this class saves it securely
 * on your phone's internal storage so your choices are remembered every time you open the app.
 *
 * It exposes a reactive [StateFlow] of [UserProfile] so that any screen in the app
 * automatically updates when your profile changes.
 */
class SessionManager(context: Context) {

    companion object {
        private const val PREFERENCES_FILE_NAME = "resqlink_session"

        // Identity keys
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHOTO_URL = "photo_url"
        private const val KEY_IS_GUEST = "is_guest"

        // Medical profile keys
        private const val KEY_BLOOD_TYPE = "blood_type"
        private const val KEY_ALLERGIES = "allergies"
        private const val KEY_MEDICAL_NOTES = "medical_notes"
        private const val KEY_PRIMARY_CONTACT_NAME = "primary_contact_name"
        private const val KEY_PRIMARY_CONTACT_PHONE = "primary_contact_phone"

        // Telephony and priority keys
        private const val KEY_CALL_PRIORITY = "call_priority"
        private const val KEY_SIM_PREFERENCE = "sim_preference"

        // Optional API integration keys
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_ELEVENLABS_API_KEY = "elevenlabs_api_key"
        private const val KEY_ELEVENLABS_VOICE_ID = "elevenlabs_voice_id"
        private const val KEY_LAST_AI_SCAN_TIME = "last_ai_scan_time"

        // Feature flags
        private const val KEY_GUARDIAN_MODE_ENABLED = "guardian_mode_enabled"
        private const val KEY_EXPERIMENTAL_MOTION_ENABLED = "experimental_motion_enabled"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE)

    /**
     * Reactive state holder emitting the current user profile.
     */
    private val _userProfile = MutableStateFlow(loadUserSessionFromDisk())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    /**
     * Reads all saved user fields from the local [SharedPreferences] disk storage.
     */
    private fun loadUserSessionFromDisk(): UserProfile {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        val displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, "") ?: ""
        val email = sharedPreferences.getString(KEY_EMAIL, "") ?: ""
        val photoUrl = sharedPreferences.getString(KEY_PHOTO_URL, null)
        val isGuest = sharedPreferences.getBoolean(KEY_IS_GUEST, false)

        val bloodType = sharedPreferences.getString(KEY_BLOOD_TYPE, "") ?: ""
        val allergies = sharedPreferences.getString(KEY_ALLERGIES, "") ?: ""
        val medicalNotes = sharedPreferences.getString(KEY_MEDICAL_NOTES, "") ?: ""
        val contactName = sharedPreferences.getString(KEY_PRIMARY_CONTACT_NAME, "") ?: ""
        val contactPhone = sharedPreferences.getString(KEY_PRIMARY_CONTACT_PHONE, "") ?: ""

        val savedPriorityName = sharedPreferences.getString(KEY_CALL_PRIORITY, CallPriority.LOCAL_HOTLINE.name)
            ?: CallPriority.LOCAL_HOTLINE.name
        val callPriority = try {
            CallPriority.valueOf(savedPriorityName)
        } catch (parsingException: Exception) {
            CallPriority.LOCAL_HOTLINE
        }

        val savedSimPreferenceName = sharedPreferences.getString(KEY_SIM_PREFERENCE, SimPreference.SYSTEM_DEFAULT.name)
            ?: SimPreference.SYSTEM_DEFAULT.name
        val simPreference = try {
            SimPreference.valueOf(savedSimPreferenceName)
        } catch (parsingException: Exception) {
            SimPreference.SYSTEM_DEFAULT
        }

        return UserProfile(
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            bloodType = bloodType,
            allergies = allergies,
            medicalNotes = medicalNotes,
            primaryContactName = contactName,
            primaryContactPhone = contactPhone,
            callPriority = callPriority,
            simPreference = simPreference,
            isLoggedIn = isLoggedIn,
            isGuest = isGuest
        )
    }

    /**
     * Stores Google Sign-In profile information.
     */
    fun saveGoogleSession(displayName: String, email: String, photoUrl: String? = null) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_PHOTO_URL, photoUrl)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()

        _userProfile.value = _userProfile.value.copy(
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            isLoggedIn = true,
            isGuest = false
        )
    }

    /**
     * Initializes a guest session allowing immediate offline emergency use without logging in.
     */
    fun continueAsGuest() {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_DISPLAY_NAME, "Emergency Guest")
            .putString(KEY_EMAIL, "")
            .putBoolean(KEY_IS_GUEST, true)
            .apply()

        _userProfile.value = _userProfile.value.copy(
            displayName = "Emergency Guest",
            email = "",
            isLoggedIn = true,
            isGuest = true
        )
    }

    /**
     * Updates and saves the user's vital medical profile details.
     */
    fun updateMedicalProfile(
        displayName: String,
        bloodType: String,
        allergies: String,
        medicalNotes: String,
        primaryContactName: String,
        primaryContactPhone: String
    ) {
        sharedPreferences.edit()
            .putString(KEY_DISPLAY_NAME, displayName)
            .putString(KEY_BLOOD_TYPE, bloodType)
            .putString(KEY_ALLERGIES, allergies)
            .putString(KEY_MEDICAL_NOTES, medicalNotes)
            .putString(KEY_PRIMARY_CONTACT_NAME, primaryContactName)
            .putString(KEY_PRIMARY_CONTACT_PHONE, primaryContactPhone)
            .apply()

        _userProfile.value = _userProfile.value.copy(
            displayName = displayName,
            bloodType = bloodType,
            allergies = allergies,
            medicalNotes = medicalNotes,
            primaryContactName = primaryContactName,
            primaryContactPhone = primaryContactPhone
        )
    }

    /**
     * Updates the emergency call priority preference (Local Station First, 911 First, or Family First).
     */
    fun updateCallPriority(callPriority: CallPriority) {
        sharedPreferences.edit()
            .putString(KEY_CALL_PRIORITY, callPriority.name)
            .apply()

        _userProfile.value = _userProfile.value.copy(callPriority = callPriority)
    }

    /**
     * Updates the preferred dual-SIM slot for emergency calls.
     */
    fun updateSimPreference(simPreference: SimPreference) {
        sharedPreferences.edit()
            .putString(KEY_SIM_PREFERENCE, simPreference.name)
            .apply()

        _userProfile.value = _userProfile.value.copy(simPreference = simPreference)
    }

    /**
     * Resolves the Gemini Location key from config or disk.
     */
    fun getGeminiLocationApiKey(): String {
        if (AppConfig.GEMINI_LOCATION_API_KEY.isNotBlank()) {
            return AppConfig.GEMINI_LOCATION_API_KEY.trim()
        }
        return getGeminiApiKey()
    }

    /**
     * Resolves the Gemini Voice key from config or disk.
     */
    fun getGeminiVoiceApiKey(): String {
        if (AppConfig.GEMINI_VOICE_API_KEY.isNotBlank()) {
            return AppConfig.GEMINI_VOICE_API_KEY.trim()
        }
        return getGeminiApiKey()
    }

    /**
     * Returns the generic Gemini API key.
     */
    fun getGeminiApiKey(): String {
        if (AppConfig.GEMINI_API_KEY.isNotBlank()) {
            return AppConfig.GEMINI_API_KEY.trim()
        }
        return sharedPreferences.getString(KEY_GEMINI_API_KEY, "")?.trim() ?: ""
    }

    fun setGeminiApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(KEY_GEMINI_API_KEY, apiKey.trim())
            .apply()
    }

    fun getGeminiVoiceName(): String {
        return sharedPreferences.getString("gemini_voice_name", AppConfig.DEFAULT_GEMINI_VOICE)?.trim()
            ?: AppConfig.DEFAULT_GEMINI_VOICE
    }

    fun setGeminiVoiceName(voiceName: String) {
        val resolvedVoiceName = voiceName.trim().ifBlank { AppConfig.DEFAULT_GEMINI_VOICE }
        sharedPreferences.edit()
            .putString("gemini_voice_name", resolvedVoiceName)
            .apply()
    }

    fun getElevenLabsApiKey(): String {
        if (AppConfig.ELEVENLABS_API_KEY.isNotBlank()) {
            return AppConfig.ELEVENLABS_API_KEY.trim()
        }
        return sharedPreferences.getString(KEY_ELEVENLABS_API_KEY, "")?.trim() ?: ""
    }

    fun setElevenLabsApiKey(apiKey: String) {
        sharedPreferences.edit()
            .putString(KEY_ELEVENLABS_API_KEY, apiKey.trim())
            .apply()
    }

    fun getElevenLabsVoiceId(): String {
        val storedVoiceId = sharedPreferences.getString(KEY_ELEVENLABS_VOICE_ID, AppConfig.DEFAULT_ELEVENLABS_VOICE_ID)?.trim()
        if (storedVoiceId.isNullOrBlank() || storedVoiceId == "21m00Tcm4TlvDq8ikWAM" || storedVoiceId == "Aoede") {
            return AppConfig.DEFAULT_ELEVENLABS_VOICE_ID
        }
        return storedVoiceId
    }

    fun setElevenLabsVoiceId(voiceId: String) {
        val resolvedVoiceId = voiceId.trim().ifBlank { AppConfig.DEFAULT_ELEVENLABS_VOICE_ID }
        sharedPreferences.edit()
            .putString(KEY_ELEVENLABS_VOICE_ID, resolvedVoiceId)
            .apply()
    }

    fun getLastAiScanTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_AI_SCAN_TIME, 0L)
    }

    fun setLastAiScanTime(timestamp: Long) {
        sharedPreferences.edit()
            .putLong(KEY_LAST_AI_SCAN_TIME, timestamp)
            .apply()
    }

    fun isGuardianModeEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_GUARDIAN_MODE_ENABLED, true)
    }

    fun setGuardianModeEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_GUARDIAN_MODE_ENABLED, enabled).apply()
    }

    fun isExperimentalMotionDetectionEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_EXPERIMENTAL_MOTION_ENABLED, false) // Strictly disabled by default
    }

    fun setExperimentalMotionDetectionEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_EXPERIMENTAL_MOTION_ENABLED, enabled).apply()
    }

    fun logout() {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()

        _userProfile.value = _userProfile.value.copy(isLoggedIn = false)
    }
}
