package com.resqlink.ai.data.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.LocationSnapshot
import com.resqlink.ai.domain.model.TrustedContact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsDispatcher(private val context: Context) {

    private val smsManager: SmsManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    fun dispatchUnresponsiveAlerts(
        contacts: List<TrustedContact>,
        location: LocationSnapshot?,
        nearestHotline: Hotline?,
        userName: String = "ResQLink User"
    ): Int {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSmsPermission) return 0

        val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val mapsLink = location?.let { "https://maps.google.com/?q=${it.latitude},${it.longitude} (Acc: ${it.accuracyMeters.toInt()}m)" }
            ?: "GPS unavailable"

        val hotlineInfo = nearestHotline?.let { "${it.name} (${it.primaryNumber})" } ?: "911 Universal"

        val message = "EMERGENCY ALERT (ResQLink AI): $userName may be unresponsive or in distress.\n" +
                "Location: $mapsLink\n" +
                "Nearest Hotline: $hotlineInfo\n" +
                "Time: $timeString\n" +
                "Do not ignore."

        var sentCount = 0
        val authorizedContacts = contacts.filter { it.isAuthorizedForSafetyCheck }

        for (contact in authorizedContacts) {
            try {
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(contact.phoneNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(contact.phoneNumber, null, message, null, null)
                }
                sentCount++
            } catch (e: Exception) {
                // Track failure silently to allow remaining contacts to receive the message
            }
        }

        return sentCount
    }
}
