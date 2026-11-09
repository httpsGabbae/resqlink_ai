package com.resqlink.ai.service

import java.util.Locale

enum class DispatcherIntent {
    CONSCIOUSNESS,
    LOCATION_LANDMARK,
    COLLISION_SEVERITY,
    MEDICAL_ALLERGIES,
    EMERGENCY_CONTACT,
    IN_PERSON_ASSESSMENT,
    GENERAL_UNCONFIRMED
}

class DispatcherQuestionClassifier {

    /**
     * Categorizes a dispatcher's spoken inquiry into grounded domains or flags it
     * as an in-person physical assessment question subject to the strict safety boundary.
     */
    fun classifyQuestion(question: String): DispatcherIntent {
        val q = question.lowercase(Locale.ROOT).trim()

        // 1. Strict Priority Check: In-Person Physical Scene Assessment
        // (Visual wounds, extrication, bleeding, fire, smoke, body position)
        val inPersonKeywords = listOf(
            "bleeding", "bleed", "blood coming", "dumudugo", "dugo",
            "trapped", "naipit", "ipit", "stuck in car", "pinned",
            "fire", "sunog", "smoke", "usok", "flames", "apoy",
            "wound", "sugat", "broken bone", "bali", "fracture",
            "eyes open", "nakadilat", "dilat", "breathing", "humihinga",
            "passengers", "ilang sakay", "how many people", "dead", "patay"
        )
        if (inPersonKeywords.any { q.contains(it) }) {
            return DispatcherIntent.IN_PERSON_ASSESSMENT
        }

        // 2. Consciousness / Responsiveness
        val consciousnessKeywords = listOf(
            "conscious", "awake", "gising", "responsive", "sumasagot",
            "can they hear", "hear me", "naririnig", "unconscious", "himatay",
            "malay", "kamalayan", "nakakausap", "talk to me"
        )
        if (consciousnessKeywords.any { q.contains(it) }) {
            return DispatcherIntent.CONSCIOUSNESS
        }

        // 3. Location / Landmark
        val locationKeywords = listOf(
            "where", "saan", "location", "address", "landmark", "katabi",
            "malapit", "street", "kalye", "barangay", "city", "lugar",
            "corner", "kanto", "road", "tapat", "building"
        )
        if (locationKeywords.any { q.contains(it) }) {
            return DispatcherIntent.LOCATION_LANDMARK
        }

        // 4. Collision Severity / G-Force Impact
        val collisionKeywords = listOf(
            "how bad", "crash", "bangga", "impact", "severity",
            "g-force", "force", "speed", "bilis", "damage",
            "anong nangyari", "what happened", "rollover", "tumagilid"
        )
        if (collisionKeywords.any { q.contains(it) }) {
            return DispatcherIntent.COLLISION_SEVERITY
        }

        // 5. Medical History & Allergies
        val medicalKeywords = listOf(
            "blood", "blood type", "allergy", "allergies", "allergic",
            "condition", "sakit", "medical", "gamot", "medicine",
            "maintenance", "asthma", "diabetes", "hypertension", "heart"
        )
        if (medicalKeywords.any { q.contains(it) }) {
            return DispatcherIntent.MEDICAL_ALLERGIES
        }

        // 6. Emergency Contact Person
        val contactKeywords = listOf(
            "contact", "family", "pamilya", "kasama", "relatives",
            "kamag-anak", "phone number", "asawa", "magulang", "sino tatawagan"
        )
        if (contactKeywords.any { q.contains(it) }) {
            return DispatcherIntent.EMERGENCY_CONTACT
        }

        // 7. Fallback: Any unknown question about the scene is treated as unobservable in-person
        return DispatcherIntent.GENERAL_UNCONFIRMED
    }
}
