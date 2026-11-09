package com.resqlink.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.resqlink.ai.domain.model.CallPriority
import com.resqlink.ai.domain.model.SimPreference
import com.resqlink.ai.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "resqlink_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHOTO_URL = "photo_url"
        private const val KEY_IS_GUEST = "is_guest"

        private const val KEY_BLOOD_TYPE = "blood_type"
        private const val KEY_ALLERGIES = "allergies"
        private const val KEY_MEDICAL_NOTES = "medical_notes"
        private const val KEY_PRIMARY_CONTACT_NAME = "primary_contact_name"
        private const val KEY_PRIMARY_CONTACT_PHONE = "primary_contact_phone"
        private const val KEY_CALL_PRIORITY = "call_priority"
        private const val KEY_SIM_PREFERENCE = "sim_preference"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_ELEVENLABS_API_KEY = "elevenlabs_api_key"
        private const val KEY_ELEVENLABS_VOICE_ID = "elevenlabs_voice_id"
        private const val KEY_LAST_AI_SCAN_TIME = "last_ai_scan_time"
        private const val KEY_GUARDIAN_MODE_ENABLED = "guardian_mode_enabled"
        private const val KEY_EXPERIMENTAL_MOTION_ENABLED = "experimental_motion_enabled"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadSession())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadSession(): UserProfile {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val displayName = prefs.getString(KEY_DISPLAY_NAME, "Juan Dela Cruz") ?: "Juan Dela Cruz"
        val email = prefs.getString(KEY_EMAIL, "juan.delacruz@gmail.com") ?: ""
        val photoUrl = prefs.getString(KEY_PHOTO_URL, null)
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, false)

        val bloodType = prefs.getString(KEY_BLOOD_TYPE, "O+") ?: "O+"
        val allergies = prefs.getString(KEY_ALLERGIES, "Penicillin") ?: "Penicillin"
        val medicalNotes = prefs.getString(KEY_MEDICAL_NOTES, "Asthmatic • Inhaler in bag") ?: "Asthmatic • Inhaler in bag"
        val contactName = prefs.getString(KEY_PRIMARY_CONTACT_NAME, "Maria Dela Cruz") ?: "Maria Dela Cruz"
        val contactPhone = prefs.getString(KEY_PRIMARY_CONTACT_PHONE, "0917-888-1234") ?: "0917-888-1234"
        val priorityName = prefs.getString(KEY_CALL_PRIORITY, CallPriority.LOCAL_HOTLINE.name) ?: CallPriority.LOCAL_HOTLINE.name
        val callPriority = try {
            CallPriority.valueOf(priorityName)
        } catch (e: Exception) {
            CallPriority.LOCAL_HOTLINE
        }

        val simPrefName = prefs.getString(KEY_SIM_PREFERENCE, SimPreference.SYSTEM_DEFAULT.name) ?: SimPreference.SYSTEM_DEFAULT.name
        val simPreference = try {
            SimPreference.valueOf(simPrefName)
        } catch (e: Exception) {
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

    fun saveGoogleSession(displayName: String, email: String, photoUrl: String? = null) {
        prefs.edit()
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

    fun continueAsGuest() {
        prefs.edit()
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

    fun updateMedicalProfile(
        displayName: String,
        bloodType: String,
        allergies: String,
        medicalNotes: String,
        primaryContactName: String,
        primaryContactPhone: String
    ) {
        prefs.edit()
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

    fun updateCallPriority(callPriority: CallPriority) {
        prefs.edit()
            .putString(KEY_CALL_PRIORITY, callPriority.name)
            .apply()

        _userProfile.value = _userProfile.value.copy(callPriority = callPriority)
    }

    fun updateSimPreference(simPreference: SimPreference) {
        prefs.edit()
            .putString(KEY_SIM_PREFERENCE, simPreference.name)
            .apply()

        _userProfile.value = _userProfile.value.copy(simPreference = simPreference)
    }

    fun getGeminiLocationApiKey(): String {
        if (AppConfig.GEMINI_LOCATION_API_KEY.isNotBlank()) {
            return AppConfig.GEMINI_LOCATION_API_KEY.trim()
        }
        return getGeminiApiKey()
    }

    fun getGeminiVoiceApiKey(): String {
        if (AppConfig.GEMINI_VOICE_API_KEY.isNotBlank()) {
            return AppConfig.GEMINI_VOICE_API_KEY.trim()
        }
        return getGeminiApiKey()
    }

    fun getGeminiApiKey(): String {
        if (AppConfig.GEMINI_API_KEY.isNotBlank()) {
            return AppConfig.GEMINI_API_KEY.trim()
        }
        return prefs.getString(KEY_GEMINI_API_KEY, "")?.trim() ?: ""
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit()
            .putString(KEY_GEMINI_API_KEY, key.trim())
            .apply()
    }

    fun getGeminiVoiceName(): String {
        return prefs.getString("gemini_voice_name", AppConfig.DEFAULT_GEMINI_VOICE)?.trim() ?: AppConfig.DEFAULT_GEMINI_VOICE
    }

    fun setGeminiVoiceName(voiceName: String) {
        val resolved = voiceName.trim().ifBlank { AppConfig.DEFAULT_GEMINI_VOICE }
        prefs.edit()
            .putString("gemini_voice_name", resolved)
            .apply()
    }

    fun getElevenLabsApiKey(): String {
        if (AppConfig.ELEVENLABS_API_KEY.isNotBlank()) {
            return AppConfig.ELEVENLABS_API_KEY.trim()
        }
        return prefs.getString(KEY_ELEVENLABS_API_KEY, "")?.trim() ?: ""
    }

    fun setElevenLabsApiKey(key: String) {
        prefs.edit()
            .putString(KEY_ELEVENLABS_API_KEY, key.trim())
            .apply()
    }

    fun getElevenLabsVoiceId(): String {
        val stored = prefs.getString(KEY_ELEVENLABS_VOICE_ID, AppConfig.DEFAULT_ELEVENLABS_VOICE_ID)?.trim()
        if (stored.isNullOrBlank() || stored == "21m00Tcm4TlvDq8ikWAM" || stored == "Aoede") {
            return AppConfig.DEFAULT_ELEVENLABS_VOICE_ID
        }
        return stored
    }

    fun setElevenLabsVoiceId(voiceId: String) {
        val resolved = voiceId.trim().ifBlank { AppConfig.DEFAULT_ELEVENLABS_VOICE_ID }
        prefs.edit()
            .putString(KEY_ELEVENLABS_VOICE_ID, resolved)
            .apply()
    }

    fun getLastAiScanTime(): Long {
        return prefs.getLong(KEY_LAST_AI_SCAN_TIME, 0L)
    }

    fun setLastAiScanTime(timestamp: Long) {
        prefs.edit()
            .putLong(KEY_LAST_AI_SCAN_TIME, timestamp)
            .apply()
    }

    fun isGuardianModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_GUARDIAN_MODE_ENABLED, true)
    }

    fun setGuardianModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GUARDIAN_MODE_ENABLED, enabled).apply()
    }

    fun isExperimentalMotionDetectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_EXPERIMENTAL_MOTION_ENABLED, false) // Strictly disabled by default
    }

    fun setExperimentalMotionDetectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EXPERIMENTAL_MOTION_ENABLED, enabled).apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()

        _userProfile.value = _userProfile.value.copy(isLoggedIn = false)
    }
}
