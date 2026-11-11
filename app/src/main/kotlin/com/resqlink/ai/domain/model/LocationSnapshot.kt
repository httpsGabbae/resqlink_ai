package com.resqlink.ai.domain.model

/**
 * # LocationSnapshot
 *
 * Represents an exact snapshot of the user's geographic location at a specific moment in time.
 *
 * This data is captured directly from the phone's hardware GPS chip and cellular network towers.
 * It is attached to every emergency alert so first responders and loved ones know exactly where
 * to send rescue teams.
 *
 * @property latitude The geographical latitude coordinate (e.g. 14.6507 for Quezon City).
 * @property longitude The geographical longitude coordinate (e.g. 121.0509 for Quezon City).
 * @property accuracyMeters Estimated accuracy radius in meters (e.g. 4.2m means the user is
 *   within 4.2 meters of the reported coordinate).
 * @property address A human-readable reverse-geocoded address string (e.g. "Diliman, Quezon City"),
 *   or null if operating in pure offline mode without network geocoding.
 * @property timestamp Epoch time in milliseconds when this GPS fix was captured.
 */
data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts the GPS coordinates into a universal Google Maps web link.
     * This link is included in emergency SMS text messages so the recipient
     * can simply tap it to open navigation directly to the user.
     *
     * Example: "https://maps.google.com/?q=14.6507,121.0509"
     */
    fun toMapsUrl(): String = "https://maps.google.com/?q=$latitude,$longitude"

    /**
     * Creates a standard Android geo-intent URI that opens any map application installed
     * on the phone and searches for emergency hospitals near these coordinates.
     *
     * Example: "geo:14.6507,121.0509?q=emergency+hospitals"
     */
    fun toNearbyHospitalsUrl(): String = "geo:$latitude,$longitude?q=emergency+hospitals"
}
