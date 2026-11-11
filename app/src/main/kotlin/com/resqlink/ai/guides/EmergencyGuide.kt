package com.resqlink.ai.guides

import kotlinx.serialization.Serializable

/**
 * # GuideCategory
 *
 * Distinct medical emergency and natural disaster classifications for offline survival guides.
 */
enum class GuideCategory(val displayNameEn: String, val displayNameFil: String) {
    CPR("CPR & Cardiac Arrest", "CPR at Pag-atake sa Puso"),
    BLEEDING("Severe Bleeding & Wounds", "Malubhang Pagdurugo at Sugat"),
    CHOKING("Choking First Aid", "Pang-unang Lunas sa Pagkabulaukan"),
    EARTHQUAKE("Earthquake Safety", "Kaligtasan sa Lindol"),
    FIRE("Fire Evacuation & Burns", "Paglikas sa Sunog at Paso"),
    TYPHOON("Typhoon & Storm Surge", "Bagyo at Daluyong ng Bagyo"),
    FLOOD("Flood Safety & Water Hazards", "Kaligtasan sa Baha at Sakuna")
}

/**
 * # GuideLanguage
 *
 * Supported offline languages for medical instructions (English and Filipino/Tagalog).
 */
enum class GuideLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    FILIPINO("fil", "Filipino (Tagalog)")
}

/**
 * # GuideStep
 *
 * A single actionable protocol step within an emergency first-aid or disaster guide.
 *
 * @property stepNumber 1-based sequential step order (1, 2, 3...)
 * @property title Concise action title (e.g., "Check Responsiveness", "Compress Chest")
 * @property instruction Plain-English or Filipino instructions on what to physically do
 * @property criticalAlert Optional life-saving safety warning (e.g., "DO NOT give water to unconscious victims")
 */
@Serializable
data class GuideStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val criticalAlert: String? = null
)

/**
 * # EmergencyGuide
 *
 * Complete standalone emergency guide stored 100% offline in application memory.
 *
 * Ordinary Human Overview:
 * When catastrophic typhoons, floods, or earthquakes disrupt telecommunications towers,
 * internet access is completely cut off.
 *
 * Every single guide in ResQLink AI:
 * 1. Is pre-compiled into local application storage.
 * 2. Requires zero bytes of network data to open.
 * 3. Follows certified Philippine Red Cross and NDRRMO protocols in both English and Tagalog.
 * 4. Highlights dangerous misconceptions ("What NOT To Do") to prevent secondary injury.
 */
@Serializable
data class EmergencyGuide(
    val id: String,
    val category: GuideCategory,
    val language: GuideLanguage,
    val title: String,
    val summary: String,
    val steps: List<GuideStep>,
    val whatNotToDo: List<String>,
    val authoritativeSource: String,
    val emergencyHotlineNumber: String = "911"
)
