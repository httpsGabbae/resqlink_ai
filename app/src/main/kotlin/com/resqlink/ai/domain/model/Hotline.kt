package com.resqlink.ai.domain.model

/**
 * # HotlineCategory
 *
 * Categorizes the different types of emergency response institutions across the Philippines.
 * This makes it easy for users to filter stations by need (e.g. searching only for Police or Hospitals).
 *
 * @property displayName A clean, formatted label suitable for displaying on UI buttons and headers.
 */
enum class HotlineCategory(val displayName: String) {
    /** National 911 emergency call center. */
    UNIVERSAL_911("Universal Emergency (911)"),

    /** Local village/neighborhood government disaster and security response post. */
    BARANGAY("Barangay Emergency Hall"),

    /** Philippine National Police station or precinct covering law enforcement and security. */
    POLICE_PNP("Philippine National Police (PNP)"),

    /** 24/7 hospital emergency rooms, trauma centers, and ambulance dispatch teams. */
    MEDICAL_HOSPITAL("Hospital / Emergency Room"),

    /** Local government Disaster Risk Reduction and Management Office (DRRMO / Rescue). */
    DISASTER_DRRMO("Disaster & Rescue (DRRMO)"),

    /** Bureau of Fire Protection for firefighting, rescues, and hazardous materials incidents. */
    FIRE_BFP("Bureau of Fire Protection (BFP)"),

    /** A personal or private emergency hotline added manually by the user. */
    CUSTOM_CONTACT("Custom Emergency Contact")
}

/**
 * # Hotline
 *
 * Represents an official emergency station or contact number in the Philippines.
 *
 * Every hotline includes physical geographic coordinates (latitude and longitude)
 * so that ResQLink can calculate the exact straight-line distance (in kilometers)
 * from the user's phone to the station without needing any internet connection.
 *
 * @property id Unique identifier for the hotline (e.g. "911-NAT", "QC-DRRMO", "PRC-143").
 * @property name Official title of the station or hospital (e.g. "Philippine Red Cross").
 * @property category The functional type of emergency service (Police, Fire, Hospital, etc.).
 * @property primaryNumber The primary telephone or mobile number to dial (e.g. "143", "911").
 * @property alternateNumber A secondary backup phone or landline number, if available.
 * @property cityMunicipality The city or municipality where the station is located (e.g. "Quezon City").
 * @property province The province or administrative territory (e.g. "Metro Manila").
 * @property region The administrative region of the Philippines (e.g. "NCR", "Region IV-A").
 * @property latitude GPS latitude coordinate of the physical facility.
 * @property longitude GPS longitude coordinate of the physical facility.
 * @property distanceKm Straight-line distance from the user's live position to this station in kilometers.
 * @property isPinned When true, this station is prioritized and placed at the very top of lists.
 * @property lastVerifiedTimestamp Epoch time in milliseconds when this number was verified.
 * @property source Where this hotline record originated from (e.g. "SYSTEM", "AI_DISCOVERY", "OVERPASS").
 * @property isCustom True if the user manually typed and saved this hotline themselves.
 */
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
