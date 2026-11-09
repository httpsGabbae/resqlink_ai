package com.resqlink.ai.domain.model

enum class HotlineCategory(val displayName: String) {
    UNIVERSAL_911("Universal Emergency (911)"),
    BARANGAY("Barangay Emergency Hall"),
    POLICE_PNP("Philippine National Police (PNP)"),
    MEDICAL_HOSPITAL("Hospital / Emergency Room"),
    DISASTER_DRRMO("Disaster & Rescue (DRRMO)"),
    FIRE_BFP("Bureau of Fire Protection (BFP)"),
    CUSTOM_CONTACT("Custom Emergency Contact")
}

data class Hotline(
    val id: String,
    val name: String,
    val category: HotlineCategory,
    val primaryNumber: String,
    val alternateNumber: String? = null,
    val cityMunicipality: String,
    val province: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double? = null,
    val isPinned: Boolean = false,
    val lastVerifiedTimestamp: Long = System.currentTimeMillis(),
    val source: String = "SYSTEM",
    val isCustom: Boolean = false
)
