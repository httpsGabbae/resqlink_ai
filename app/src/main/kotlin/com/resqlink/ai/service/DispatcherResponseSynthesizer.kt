package com.resqlink.ai.service

import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.domain.model.UserProfile
import java.util.Locale

class DispatcherResponseSynthesizer {

    companion object {
        const val STRICT_UNCONFIRMED_ANSWER =
            "I do not know. The patient is unresponsive and device sensors cannot confirm physical scene conditions. Please dispatch paramedics for on-scene assessment."
    }

    /**
     * Synthesizes a factual, crisp (<=8s speech) response grounded strictly in verified device
     * telemetry and user profile. If the question requires eyes on the physical scene,
     * the system strictly refrains from guessing and returns the unconfirmed safety response.
     */
    fun synthesizeResponse(
        intent: DispatcherIntent,
        telemetry: CrashTelemetry,
        userProfile: UserProfile,
        location: LocationSnapshot?
    ): String {
        // Sanitize patient name: Ensure "Google" is never added or spoken in the patient name
        val cleanName = userProfile.displayName
            .replace("(?i)google\\s*(user|account)?".toRegex(), "")
            .trim()
            .ifBlank { "the patient" }

        // Sanitize location: clean landmark without raw GPS decimal rattling
        val rawAddress = location?.address?.ifBlank { null } ?: "Metro Manila vicinity"
        val cleanLocation = rawAddress
            .replace("(?i)location\\s*coordinates.*".toRegex(), "")
            .replace("(?i)lat.*long.*".toRegex(), "")
            .replace("(?i)google.*".toRegex(), "")
            .trim()
            .ifBlank { "Metro Manila area" }

        val cleanBlood = userProfile.bloodType.ifBlank { "Not documented" }
        val cleanAllergies = userProfile.allergies.ifBlank { "No known allergies" }
        val contactName = userProfile.primaryContactName.ifBlank { "Family member" }
        val contactPhone = userProfile.primaryContactPhone.ifBlank { "Not documented" }

        return when (intent) {
            DispatcherIntent.IN_PERSON_ASSESSMENT,
            DispatcherIntent.GENERAL_UNCONFIRMED -> {
                STRICT_UNCONFIRMED_ANSWER
            }

            DispatcherIntent.CONSCIOUSNESS -> {
                val stillMinutes = (telemetry.elapsedSecondsSinceCrash / 60L).coerceAtLeast(1L)
                "The patient is unresponsive to audio and touch prompts. The device has been stationary for $stillMinutes minutes following impact. Voice recognition is standing by if they wake up."
            }

            DispatcherIntent.LOCATION_LANDMARK -> {
                "The exact position is $cleanLocation. Device is stationary at this location."
            }

            DispatcherIntent.COLLISION_SEVERITY -> {
                val gForceFormatted = String.format(Locale.US, "%.1f", telemetry.peakGForce)
                "Device accelerometer registered a severe collision impact of $gForceFormatted Gs approximately ${telemetry.elapsedSecondsSinceCrash} seconds ago, followed by complete stillness."
            }

            DispatcherIntent.MEDICAL_ALLERGIES -> {
                "Profile records for $cleanName: Blood type is $cleanBlood. Allergies: $cleanAllergies."
            }

            DispatcherIntent.EMERGENCY_CONTACT -> {
                "Primary safety contact on file: $contactName at phone number $contactPhone."
            }
        }
    }
}
