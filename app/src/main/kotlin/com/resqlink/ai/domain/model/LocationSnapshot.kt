package com.resqlink.ai.domain.model

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"
    fun toNearbyHospitalsUrl(): String = "geo:$latitude,$longitude?q=emergency+hospitals"
}
