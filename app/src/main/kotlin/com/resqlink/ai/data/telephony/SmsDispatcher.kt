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

/**
 * # SmsDispatcher
 *
 * Responsible for formatting and transmitting emergency text messages directly across
 * the physical cellular radio via standard SMS.
 *
 * Ordinary Human Overview:
 * When an emergency is triggered, this class composes an alert message containing:
 * 1. An urgent header ("EMERGENCY ALERT: [User Name] may be in distress").
 * 2. Live GPS coordinates with a clickable Google Maps link and accuracy radius.
 * 3. The nearest local emergency hotline identified by the offline database.
 * 4. The exact time the alert was generated.
 *
 * It sends this text to all authorized [TrustedContact] recipients using the Android
 * [SmsManager]. Because it relies purely on standard cellular GSM SMS, it requires NO
 * internet connection, Wi-Fi, or mobile data.
 */
class SmsDispatcher(private val context: Context) {

    /**
     * Resolves the proper [SmsManager] instance based on the device's Android version.
     * Android 12 (API 31+) uses `context.getSystemService(SmsManager::class.java)`,
     * while earlier versions use `SmsManager.getDefault()`.
     */
    private val systemSmsManager: SmsManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    /**
     * Dispatches emergency alerts to all authorized trusted contacts.
     *
     * @param contactList The list of candidate emergency contacts.
     * @param locationSnapshot The user's live GPS fix (latitude, longitude, accuracy).
     * @param nearestHotline The closest emergency station discovered by the offline database.
     * @param userName The name of the user to display in the alert message.
     * @return The number of contacts that successfully received an SMS transmission attempt.
     */
    fun dispatchUnresponsiveAlerts(
        contacts: List<TrustedContact>,
        location: LocationSnapshot?,
        nearestHotline: Hotline?,
        userName: String = "ResQLink User"
    ): Int {
        // 1. Verify the app has permission to send SMS
        val hasSendSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasSendSmsPermission) {
            return 0
        }

        // 2. Format the time and GPS map link
        val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val mapLink = location?.let {
            "https://maps.google.com/?q=${it.latitude},${it.longitude} (Acc: ${it.accuracyMeters.toInt()}m)"
        } ?: "GPS unavailable"

        val hotlineDetails = nearestHotline?.let {
            "${it.name} (${it.primaryNumber})"
        } ?: "911 Universal"

        // 3. Compose the emergency text message
        val emergencyMessage = "EMERGENCY ALERT (ResQLink AI): $userName may be unresponsive or in distress.\n" +
                "Location: $mapLink\n" +
                "Nearest Hotline: $hotlineDetails\n" +
                "Time: $formattedTime\n" +
                "Do not ignore."

        var totalSuccessfulTransmissions = 0
        val authorizedRecipients = contacts.filter { it.isAuthorizedForSafetyCheck }

        // 4. Send the SMS to each authorized contact
        for (contact in authorizedRecipients) {
            try {
                // If message exceeds 160 characters, split into multipart SMS
                val messageParts = systemSmsManager.divideMessage(emergencyMessage)
                if (messageParts.size > 1) {
                    systemSmsManager.sendMultipartTextMessage(
                        contact.phoneNumber,
                        null,
                        messageParts,
                        null,
                        null
                    )
                } else {
                    systemSmsManager.sendTextMessage(
                        contact.phoneNumber,
                        null,
                        emergencyMessage,
                        null,
                        null
                    )
                }
                totalSuccessfulTransmissions++
            } catch (transmissionException: Exception) {
                // Log and continue to the next contact so one invalid number doesn't block others
            }
        }

        return totalSuccessfulTransmissions
    }
}
