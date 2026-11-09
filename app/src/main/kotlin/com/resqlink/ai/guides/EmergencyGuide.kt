package com.resqlink.ai.guides

import kotlinx.serialization.Serializable

enum class GuideCategory(val displayNameEn: String, val displayNameFil: String) {
    CPR("CPR & Cardiac Arrest", "CPR at Pag-atake sa Puso"),
    BLEEDING("Severe Bleeding & Wounds", "Malubhang Pagdurugo at Sugat"),
    CHOKING("Choking First Aid", "Pang-unang Lunas sa Pagkabulaukan"),
    EARTHQUAKE("Earthquake Safety", "Kaligtasan sa Lindol"),
    FIRE("Fire Evacuation & Burns", "Paglikas sa Sunog at Paso"),
    TYPHOON("Typhoon & Storm Surge", "Bagyo at Daluyong ng Bagyo"),
    FLOOD("Flood Safety & Water Hazards", "Kaligtasan sa Baha at Sakuna")
}

enum class GuideLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    FILIPINO("fil", "Filipino (Tagalog)")
}

@Serializable
data class GuideStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val criticalAlert: String? = null
)

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
