package com.resqlink.ai.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.resqlink.ai.domain.model.LocationSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * # LocationProvider
 *
 * Encapsulates Android's Google Play Services [FusedLocationProviderClient] to query
 * live GPS and cellular network location coordinates.
 *
 * Ordinary Human Overview:
 * When an emergency occurs, speed and accuracy are critical:
 * 1. **High-Accuracy Fresh Fix**: First tries to query the hardware GPS satellite chip
 *    for up to 4.0 seconds to get the freshest possible coordinates.
 * 2. **OS Cache Fallback**: If the phone is indoors or GPS satellites take too long to lock,
 *    it instantly falls back to the phone's last-known cached location so the alert is
 *    NEVER blocked or delayed.
 * 3. **Offline Address Formatter**: Attempts to reverse-geocode coordinates into an address
 *    (e.g. "Diliman, Quezon City"). If offline, it formats the raw coordinates cleanly.
 */
class LocationProvider(private val context: Context) {

    /**
     * Google's recommended high-efficiency location engine that blends GPS, Wi-Fi, and cellular towers.
     */
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    /**
     * Retrieves the freshest available GPS location, falling back to cached coordinates if necessary.
     *
     * @param timeoutMilliseconds Maximum time in milliseconds to wait for a satellite fix (default: 4000ms).
     * @return A [LocationSnapshot] containing coordinates, accuracy, and address, or null if unavailable.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentOrCachedLocation(timeoutMs: Long = 4000L): LocationSnapshot? {
        return try {
            // Step 1: Request a fresh high-accuracy satellite location with a strict timeout
            val cancellationTokenSource = CancellationTokenSource()
            val freshHardwareLocation: Location? = withTimeoutOrNull(timeoutMs) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            }

            if (freshHardwareLocation != null) {
                val resolvedAddress = resolveAddress(
                    latitude = freshHardwareLocation.latitude,
                    longitude = freshHardwareLocation.longitude
                )
                return LocationSnapshot(
                    latitude = freshHardwareLocation.latitude,
                    longitude = freshHardwareLocation.longitude,
                    accuracyMeters = freshHardwareLocation.accuracy,
                    address = resolvedAddress
                )
            }

            // Step 2: Fallback to the Android OS cached location if satellite lock timed out
            val lastCachedLocation: Location? = fusedLocationClient.lastLocation.await()
            if (lastCachedLocation != null) {
                val resolvedAddress = resolveAddress(
                    latitude = lastCachedLocation.latitude,
                    longitude = lastCachedLocation.longitude
                )
                LocationSnapshot(
                    latitude = lastCachedLocation.latitude,
                    longitude = lastCachedLocation.longitude,
                    accuracyMeters = lastCachedLocation.accuracy,
                    address = resolvedAddress
                )
            } else {
                null
            }
        } catch (locationException: Exception) {
            null
        }
    }

    /**
     * Converts GPS coordinates into a human-readable street/city address.
     * Runs on a background IO coroutine thread to avoid freezing the app UI.
     */
    private suspend fun resolveAddress(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addressList: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            formatAddress(addressList)
        } catch (geocodingException: Exception) {
            // When completely offline without internet geocoding, display coordinates clearly
            "${String.format(Locale.US, "%.4f", latitude)}, ${String.format(Locale.US, "%.4f", longitude)}"
        }
    }

    /**
     * Formats an Android [Address] object into a clean, concise string for emergency displays and SMS.
     */
    private fun formatAddress(addresses: List<Address>?): String {
        if (addresses.isNullOrEmpty()) {
            return "Current GPS Location"
        }

        val primaryAddress = addresses[0]
        val fullAddressLine = primaryAddress.getAddressLine(0)
        if (!fullAddressLine.isNullOrBlank() && !fullAddressLine.startsWith("Unnamed")) {
            return fullAddressLine
        }

        val subLocality = primaryAddress.subLocality
        val locality = primaryAddress.locality ?: primaryAddress.subAdminArea ?: primaryAddress.featureName
        val adminArea = primaryAddress.adminArea ?: ""
        val addressComponents = listOfNotNull(subLocality, locality, adminArea)
            .filter { it.isNotBlank() }
            .distinct()

        return if (addressComponents.isNotEmpty()) {
            addressComponents.joinToString(", ")
        } else {
            "Current GPS Location"
        }
    }
}
