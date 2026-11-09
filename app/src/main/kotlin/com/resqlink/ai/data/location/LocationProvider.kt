package com.resqlink.ai.data.location

import android.annotation.SuppressLint
import android.content.Context
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

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentOrCachedLocation(timeoutMs: Long = 4000L): LocationSnapshot? {
        return try {
            // First attempt: High accuracy current location with timeout
            val cts = CancellationTokenSource()
            val freshLocation: Location? = withTimeoutOrNull(timeoutMs) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()
            }

            if (freshLocation != null) {
                val addr = resolveAddress(freshLocation.latitude, freshLocation.longitude)
                return LocationSnapshot(
                    latitude = freshLocation.latitude,
                    longitude = freshLocation.longitude,
                    accuracyMeters = freshLocation.accuracy,
                    address = addr
                )
            }

            // Fallback: Last known location cached by the OS
            val lastLocation: Location? = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                val addr = resolveAddress(lastLocation.latitude, lastLocation.longitude)
                LocationSnapshot(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    accuracyMeters = lastLocation.accuracy,
                    address = addr
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveAddress(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For Android 13+ synchronous fallback
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(latitude, longitude, 1)
                formatAddress(results)
            } else {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(latitude, longitude, 1)
                formatAddress(results)
            }
        } catch (e: Exception) {
            "${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}"
        }
    }

    private fun formatAddress(addresses: List<android.location.Address>?): String {
        if (addresses.isNullOrEmpty()) return "Current GPS Location"
        val addr = addresses[0]
        val line = addr.getAddressLine(0)
        if (!line.isNullOrBlank() && !line.startsWith("Unnamed")) {
            return line
        }
        val subLocality = addr.subLocality
        val locality = addr.locality ?: addr.subAdminArea ?: addr.featureName
        val admin = addr.adminArea ?: ""
        val parts = listOfNotNull(subLocality, locality, admin).filter { it.isNotBlank() }.distinct()
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Current GPS Location"
    }
}
