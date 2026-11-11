package com.resqlink.ai.rescuecard

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Offline Emergency Medical Profile (Rescue Card) with granular privacy and visibility controls.
 * Users control which fields are exposed to bystanders and first responders during an active emergency.
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
    
    // Granular visibility opt-in flags for responders
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
        val sb = StringBuilder()
        sb.appendLine("=== RESQLINK RESCUE CARD ===")
        sb.appendLine("Name: $fullName")
        if (birthYearOrAge.isNotBlank()) sb.appendLine("Age/Birth: $birthYearOrAge")
        if (isBloodTypeVisible && bloodType.isNotBlank()) sb.appendLine("Blood Type: $bloodType")
        if (isAllergiesVisible && allergies.isNotBlank()) sb.appendLine("Allergies: $allergies")
        if (isConditionsVisible && medicalConditions.isNotBlank()) sb.appendLine("Conditions: $medicalConditions")
        if (isMedicationsVisible && medications.isNotBlank()) sb.appendLine("Medications: $medications")
        if (isInstructionsVisible && emergencyInstructions.isNotBlank()) sb.appendLine("Emergency Notes: $emergencyInstructions")
        if (isOrganDonorVisible) sb.appendLine("Organ Donor: ${if (isOrganDonor) "YES" else "NO"}")
        if (isContactsVisible) {
            sb.appendLine("Emergency Contacts:")
            if (primaryContactPhone.isNotBlank()) {
                sb.appendLine(" - $primaryContactName ($primaryContactRelation): $primaryContactPhone")
            }
            if (secondaryContactPhone.isNotBlank()) {
                sb.appendLine(" - $secondaryContactName ($secondaryContactRelation): $secondaryContactPhone")
            }
        }
        return sb.toString().trim()
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
        }.entries.joinToString(";") { "${it.key}=${it.value}" }
    }
}
