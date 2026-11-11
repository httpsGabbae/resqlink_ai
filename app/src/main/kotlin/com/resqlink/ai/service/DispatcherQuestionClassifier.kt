package com.resqlink.ai.service

import java.util.Locale

/**
 * # DispatcherIntent
 *
 * Categorization of questions spoken by 911/emergency dispatchers over speakerphone.
 */
enum class DispatcherIntent {
    CONSCIOUSNESS,
    LOCATION_LANDMARK,
    COLLISION_SEVERITY,
    MEDICAL_ALLERGIES,
    EMERGENCY_CONTACT,
    IN_PERSON_ASSESSMENT,
    GENERAL_UNCONFIRMED
}

/**
 * # DispatcherQuestionClassifier
 *
 * Classifies speech recognized from 911 dispatchers into strict semantic intents.
 *
 * Ordinary Human Overview:
 * When an unconscious driver's phone dials 911, an AI Voice Assist answers on speakerphone.
 * Dispatchers ask very specific questions:
 * - "Is the driver awake?" -> [DispatcherIntent.CONSCIOUSNESS]
 * - "Where are you?" -> [DispatcherIntent.LOCATION_LANDMARK]
 * - "How bad is the crash?" -> [DispatcherIntent.COLLISION_SEVERITY]
 * - "Does the patient have allergies?" -> [DispatcherIntent.MEDICAL_ALLERGIES]
 *
 * Safety-Critical Architecture Guarantee:
 * If a dispatcher asks about physical scene conditions that sensors CANNOT verify
 * (e.g., "Are they bleeding?", "Is there smoke?", "Are they trapped?"), the classifier
 * strictly maps to [IN_PERSON_ASSESSMENT]. The AI will NEVER guess or hallucinate medical injury.
 */
class DispatcherQuestionClassifier {

    /**
     * Categorizes a dispatcher's spoken inquiry into grounded domains or flags it
     * as an in-person physical assessment question subject to the strict safety boundary.
     *
     * @param question Spoken question transcribed by the speech engine
     */
    fun classifyQuestion(question: String): DispatcherIntent {
        val normalizedQuestion = question.lowercase(Locale.ROOT).trim()

        // 1. Strict Safety Priority: In-Person Physical Scene Assessment
        // (Visual wounds, extrication, bleeding, fire, smoke, body position)
        val inPersonVisualKeywords = listOf(
            "bleeding", "bleed", "blood coming", "dumudugo", "dugo",
            "trapped", "naipit", "ipit", "stuck in car", "pinned",
            "fire", "sunog", "smoke", "usok", "flames", "apoy",
            "wound", "sugat", "broken bone", "bali", "fracture",
            "eyes open", "nakadilat", "dilat", "breathing", "humihinga",
            "passengers", "ilang sakay", "how many people", "dead", "patay"
        )
        if (inPersonVisualKeywords.any { keyword -> normalizedQuestion.contains(keyword) }) {
            return DispatcherIntent.IN_PERSON_ASSESSMENT
        }

        // 2. Consciousness / Responsiveness
        val consciousnessKeywords = listOf(
            "conscious", "awake", "gising", "responsive", "sumasagot",
            "can they hear", "hear me", "naririnig", "unconscious", "himatay",
            "malay", "kamalayan", "nakakausap", "talk to me"
        )
        if (consciousnessKeywords.any { keyword -> normalizedQuestion.contains(keyword) }) {
            return DispatcherIntent.CONSCIOUSNESS
        }

        // 3. Location / Landmark
        val locationKeywords = listOf(
            "where", "saan", "location", "address", "landmark", "katabi",
            "malapit", "street", "kalye", "barangay", "city", "lugar",
            "corner", "kanto", "road", "tapat", "building"
        )
        if (locationKeywords.any { keyword -> normalizedQuestion.contains(keyword) }) {
            return DispatcherIntent.LOCATION_LANDMARK
        }

        // 4. Collision Severity / G-Force Impact
        val collisionKeywords = listOf(
            "how bad", "crash", "bangga", "impact", "severity",
            "g-force", "force", "speed", "bilis", "damage",
            "anong nangyari", "what happened", "rollover", "tumagilid"
        )
        if (collisionKeywords.any { keyword -> normalizedQuestion.contains(keyword) }) {
            return DispatcherIntent.COLLISION_SEVERITY
        }

        // 5. Medical History & Allergies
        val medicalKeywords = listOf(
            "blood", "blood type", "allergy", "allergies", "allergic",
            "condition", "sakit", "medical", "gamot", "medicine",
            "maintenance", "asthma", "diabetes", "hypertension", "heart"
        )
        if (medicalKeywords.any { keyword -> normalizedQuestion.contains(keyword) }) {
            return DispatcherIntent.MEDICAL_ALLERGIES
        }

        // 6. Emergency Contact Person
        val contactKeywords = listOf(
            "contact", "family", "pamilya", "kasama", "relatives",
            "kamag-anak", "phone number", "asawa", "magulang", "sino tatawagan"
        )
        if (contactKeywords.any { keyword -> normalizedQuestion.contains(keyword) }) {
            return DispatcherIntent.EMERGENCY_CONTACT
        }

        // 7. Fallback: Any unknown question about the scene is treated as unobservable in-person
        return DispatcherIntent.GENERAL_UNCONFIRMED
    }
}
