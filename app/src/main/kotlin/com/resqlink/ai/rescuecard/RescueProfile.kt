package com.resqlink.ai.rescuecard

import kotlinx.serialization.Serializable

/**
 * # RescueProfile
 *
 * Offline Emergency Medical Profile ("Rescue Card") with granular privacy and visibility controls.
 *
 * Ordinary Human Overview:
 * If a person is found unconscious in an accident, first responders (paramedics, ER doctors)
 * need critical medical information immediately:
 * - Blood Type (e.g., O+, A-, etc.)
 * - Severe Allergies (e.g., Penicillin, Peanuts)
 * - Chronic Conditions (e.g., Asthma, Diabetes, Epilepsy)
 * - In-Case-of-Emergency (ICE) family phone numbers
 *
 * Privacy Architecture:
 * ResQLink AI never forces users to expose their private health records. Every single field
 * has an independent visibility toggle ([isBloodTypeVisible], [isAllergiesVisible], etc.).
 * When displaying the Rescue Card or generating an offline QR code, ONLY fields explicitly
 * approved by the user are included.
 */
@Serializable
data class RescueProfile(
    val fullName: String = "",
    val birthYearOrAge: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val medications: String = "",
    val medicalConditions: String = "",
    val emergencyInstructions: String = "",
    val isOrganDonor: Boolean = false,
    val primaryContactName: String = "",
    val primaryContactPhone: String = "",
    val primaryContactRelation: String = "",
    val secondaryContactName: String = "",
    val secondaryContactPhone: String = "",
    val secondaryContactRelation: String = "",

    // Granular visibility opt-in toggles for bystanders and emergency responders
    val isBloodTypeVisible: Boolean = true,
    val isAllergiesVisible: Boolean = true,
    val isMedicationsVisible: Boolean = true,
    val isConditionsVisible: Boolean = true,
    val isInstructionsVisible: Boolean = true,
    val isContactsVisible: Boolean = true,
    val isOrganDonorVisible: Boolean = true
) {
    /**
     * Produces a sanitized, privacy-safe text summary containing ONLY
     * fields explicitly permitted by the user's visibility preferences.
     */
    fun toSanitizedEmergencySummary(): String {
        val summaryBuilder = StringBuilder()
        summaryBuilder.appendLine("=== RESQLINK RESCUE CARD ===")
        summaryBuilder.appendLine("Name: $fullName")
        if (birthYearOrAge.isNotBlank()) summaryBuilder.appendLine("Age/Birth: $birthYearOrAge")
        if (isBloodTypeVisible && bloodType.isNotBlank()) summaryBuilder.appendLine("Blood Type: $bloodType")
        if (isAllergiesVisible && allergies.isNotBlank()) summaryBuilder.appendLine("Allergies: $allergies")
        if (isConditionsVisible && medicalConditions.isNotBlank()) summaryBuilder.appendLine("Conditions: $medicalConditions")
        if (isMedicationsVisible && medications.isNotBlank()) summaryBuilder.appendLine("Medications: $medications")
        if (isInstructionsVisible && emergencyInstructions.isNotBlank()) summaryBuilder.appendLine("Emergency Notes: $emergencyInstructions")
        if (isOrganDonorVisible) summaryBuilder.appendLine("Organ Donor: ${if (isOrganDonor) "YES" else "NO"}")
        if (isContactsVisible) {
            summaryBuilder.appendLine("Emergency Contacts:")
            if (primaryContactPhone.isNotBlank()) {
                summaryBuilder.appendLine(" - $primaryContactName ($primaryContactRelation): $primaryContactPhone")
            }
            if (secondaryContactPhone.isNotBlank()) {
                summaryBuilder.appendLine(" - $secondaryContactName ($secondaryContactRelation): $secondaryContactPhone")
            }
        }
        return summaryBuilder.toString().trim()
    }

    /**
     * Compact payload format suitable for offline QR code display or NFC exchange.
     */
    fun toQrPayload(): String {
        return buildMap {
            put("v", "1.0")
            put("app", "ResQLink")
            put("name", fullName)
            if (isBloodTypeVisible && bloodType.isNotBlank()) put("blood", bloodType)
            if (isAllergiesVisible && allergies.isNotBlank()) put("allergies", allergies)
            if (isConditionsVisible && medicalConditions.isNotBlank()) put("conditions", medicalConditions)
            if (isInstructionsVisible && emergencyInstructions.isNotBlank()) put("notes", emergencyInstructions)
            if (isContactsVisible && primaryContactPhone.isNotBlank()) {
                put("ice1", "$primaryContactName:$primaryContactPhone")
            }
            if (isContactsVisible && secondaryContactPhone.isNotBlank()) {
                put("ice2", "$secondaryContactName:$secondaryContactPhone")
            }
        }.entries.joinToString(";") { keyValuePair -> "${keyValuePair.key}=${keyValuePair.value}" }
    }
}
