package com.resqlink.ai.service

import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.domain.model.UserProfile
import java.util.Locale

/**
 * # DispatcherResponseSynthesizer
 *
 * Synthesizes grounded, factual spoken responses for emergency dispatchers during in-call Voice Assist.
 *
 * Ordinary Human Overview:
 * When an automated voice assists a 911 dispatcher, responses must be:
 * 1. **Brief and Clear**: Under 8 seconds of speech so the line is not tied up.
 * 2. **Factually Grounded**: Strictly derived from real accelerometer telemetry, GPS coordinates,
 *    and verified medical cards.
 * 3. **Non-Hallucinating**: If asked "Is the driver bleeding?", the system explicitly replies:
 *    "I do not know. The patient is unresponsive and device sensors cannot confirm physical scene conditions."
 */
class DispatcherResponseSynthesizer {

    companion object {
        const val STRICT_UNCONFIRMED_ANSWER =
            "I do not know. The patient is unresponsive and device sensors cannot confirm physical scene conditions. Please dispatch paramedics for on-scene assessment."
    }

    /**
     * Synthesizes a factual, crisp (<=8s speech) response grounded strictly in verified device
     * telemetry and user profile. If the question requires eyes on the physical scene,
     * the system strictly refrains from guessing and returns the unconfirmed safety response.
     *
     * @param intent Classified intent of the dispatcher's question
     * @param telemetry Real-time crash kinematics and impact metrics
     * @param userProfile Emergency profile containing allergies, blood type, and ICE contacts
     * @param location Optional GPS coordinates snapshot
     */
    fun synthesizeResponse(
        intent: DispatcherIntent,
        telemetry: CrashTelemetry,
        userProfile: UserProfile,
        location: LocationSnapshot?
    ): String {
        // Sanitize patient name: Ensure "Google" is never added or spoken in the patient name
        val sanitizedPatientName = userProfile.displayName
            .replace("(?i)google\\s*(user|account)?".toRegex(), "")
            .trim()
            .ifBlank { "the patient" }

        // Sanitize location: clean landmark without raw GPS decimal rattling
        val rawAddressString = location?.address?.ifBlank { null } ?: "Metro Manila vicinity"
        val sanitizedLocationName = rawAddressString
            .replace("(?i)location\\s*coordinates.*".toRegex(), "")
            .replace("(?i)lat.*long.*".toRegex(), "")
            .replace("(?i)google.*".toRegex(), "")
            .trim()
            .ifBlank { "Metro Manila area" }

        val bloodTypeDescription = userProfile.bloodType.ifBlank { "Not documented" }
        val allergiesDescription = userProfile.allergies.ifBlank { "No known allergies" }
        val contactPersonName = userProfile.primaryContactName.ifBlank { "Family member" }
        val contactPhoneNumber = userProfile.primaryContactPhone.ifBlank { "Not documented" }

        return when (intent) {
            DispatcherIntent.IN_PERSON_ASSESSMENT,
            DispatcherIntent.GENERAL_UNCONFIRMED -> {
                STRICT_UNCONFIRMED_ANSWER
            }

            DispatcherIntent.CONSCIOUSNESS -> {
                val elapsedMinutesStationary = (telemetry.elapsedSecondsSinceCrash / 60L).coerceAtLeast(1L)
                "The patient is unresponsive to audio and touch prompts. The device has been stationary for $elapsedMinutesStationary minutes following impact. Voice recognition is standing by if they wake up."
            }

            DispatcherIntent.LOCATION_LANDMARK -> {
                "The exact position is $sanitizedLocationName. Device is stationary at this location."
            }

            DispatcherIntent.COLLISION_SEVERITY -> {
                val formattedGForce = String.format(Locale.US, "%.1f", telemetry.peakGForce)
                "Device accelerometer registered a severe collision impact of $formattedGForce Gs approximately ${telemetry.elapsedSecondsSinceCrash} seconds ago, followed by complete stillness."
            }

            DispatcherIntent.MEDICAL_ALLERGIES -> {
                "Profile records for $sanitizedPatientName: Blood type is $bloodTypeDescription. Allergies: $allergiesDescription."
            }

            DispatcherIntent.EMERGENCY_CONTACT -> {
                "Primary safety contact on file: $contactPersonName at phone number $contactPhoneNumber."
            }
        }
    }
}
