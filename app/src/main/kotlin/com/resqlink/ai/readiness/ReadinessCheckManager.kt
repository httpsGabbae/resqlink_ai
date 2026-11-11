package com.resqlink.ai.readiness

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import android.telephony.TelephonyManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.resqlink.ai.data.local.HotlineDatabaseHelper
import com.resqlink.ai.service.GuardianForegroundService

/**
 * # ReadinessCheckManager
 *
 * Runs comprehensive on-device diagnostic audits to verify all emergency subsystems.
 *
 * Ordinary Human Overview:
 * When danger strikes, you need to know *beforehand* if your phone is properly configured.
 * This manager tests 8 essential safety checkpoints:
 * 1. **Microphone**: Checks physical hardware and `RECORD_AUDIO` permission.
 * 2. **Guardian Background Service**: Checks if hands-free keyword detection is actively listening.
 * 3. **GPS Location**: Checks fine location permission and device location toggle.
 * 4. **SMS Dispatch**: Checks `SEND_SMS` permission and physical SIM readiness.
 * 5. **Emergency Contacts**: Verifies that trusted ICE contacts exist and are authorized.
 * 6. **Notifications**: Verifies lockscreen heads-up alerts are not blocked by the OS.
 * 7. **Battery Optimization**: Checks if Android Doze will aggressively kill background services.
 * 8. **Cellular Network**: Verifies connection to a carrier (Globe, Smart, DITO) for calls.
 */
class ReadinessCheckManager(
    private val context: Context,
    private val databaseHelper: HotlineDatabaseHelper = HotlineDatabaseHelper(context)
) {

    /**
     * Executes the diagnostic suite and aggregates results into a [ReadinessReport].
     */
    fun runDiagnostics(): ReadinessReport {
        val diagnosticResultsList = mutableListOf<DiagnosticItem>()

        // 1. Microphone Hardware & Permission
        diagnosticResultsList.add(checkMicrophoneStatus())

        // 2. Guardian Mode Service State
        diagnosticResultsList.add(checkGuardianServiceStatus())

        // 3. Location Hardware & Permission
        diagnosticResultsList.add(checkLocationStatus())

        // 4. SMS Dispatch Capabilities
        diagnosticResultsList.add(checkSmsDispatchStatus())

        // 5. Emergency Contacts Configuration
        diagnosticResultsList.add(checkEmergencyContactsStatus())

        // 6. Notifications & Heads-Up Alerts
        diagnosticResultsList.add(checkNotificationsStatus())

        // 7. Battery Optimization / Background Restrictions
        diagnosticResultsList.add(checkBatteryOptimizationStatus())

        // 8. Cellular Network & Telephony
        diagnosticResultsList.add(checkCellularNetworkStatus())

        val isSystemFullyOperational = diagnosticResultsList.none { it.status == DiagnosticStatus.FAIL }

        return ReadinessReport(
            overallPass = isSystemFullyOperational,
            timestamp = System.currentTimeMillis(),
            items = diagnosticResultsList
        )
    }

    private fun checkMicrophoneStatus(): DiagnosticItem {
        val hasMicrophoneHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        if (!hasMicrophoneHardware) {
            return DiagnosticItem(
                id = "mic_hardware",
                category = "Sensors",
                name = "Microphone Hardware",
                status = DiagnosticStatus.UNAVAILABLE,
                detail = "Device does not report built-in microphone hardware."
            )
        }

        val hasAudioRecordingPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasAudioRecordingPermission) {
            DiagnosticItem(
                id = "mic_permission",
                category = "Sensors",
                name = "Audio Input Access",
                status = DiagnosticStatus.PASS,
                detail = "Microphone permission granted for speech recognition and Guardian Mode."
            )
        } else {
            DiagnosticItem(
                id = "mic_permission",
                category = "Sensors",
                name = "Audio Input Access",
                status = DiagnosticStatus.FAIL,
                detail = "RECORD_AUDIO permission is not granted.",
                remediation = "Grant Microphone access in Android App Settings."
            )
        }
    }

    private fun checkGuardianServiceStatus(): DiagnosticItem {
        val isServiceCurrentlyRunning = GuardianForegroundService.isRunning
        return if (isServiceCurrentlyRunning) {
            DiagnosticItem(
                id = "guardian_service",
                category = "Background AI",
                name = "Guardian Voice Detection",
                status = DiagnosticStatus.PASS,
                detail = "Persistent foreground listener is active and monitoring offline for distress keywords."
            )
        } else {
            DiagnosticItem(
                id = "guardian_service",
                category = "Background AI",
                name = "Guardian Voice Detection",
                status = DiagnosticStatus.WARNING,
                detail = "Guardian monitoring is currently paused / standby.",
                remediation = "Toggle Guardian Mode ON in the Home screen to arm hands-free distress detection."
            )
        }
    }

    private fun checkLocationStatus(): DiagnosticItem {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocationPermission) {
            return DiagnosticItem(
                id = "location_perm",
                category = "Location",
                name = "GPS Location Access",
                status = DiagnosticStatus.FAIL,
                detail = "Location permission is not granted. Emergency SMS coordinates will be disabled.",
                remediation = "Allow Location permission (Precise) in Settings."
            )
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsHardwareEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            @Suppress("DEPRECATION")
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }

        return if (isGpsHardwareEnabled) {
            DiagnosticItem(
                id = "location_hw",
                category = "Location",
                name = "GPS Provider",
                status = DiagnosticStatus.PASS,
                detail = "Location services are enabled and available for 5km radar and SMS dispatch."
            )
        } else {
            DiagnosticItem(
                id = "location_hw",
                category = "Location",
                name = "GPS Provider",
                status = DiagnosticStatus.WARNING,
                detail = "System location services (GPS) are currently turned OFF on device.",
                remediation = "Enable Location (GPS) in Android quick settings toggle."
            )
        }
    }

    private fun checkSmsDispatchStatus(): DiagnosticItem {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val isSimReady = telephonyManager?.simState == TelephonyManager.SIM_STATE_READY

        return when {
            !hasSmsPermission -> DiagnosticItem(
                id = "sms_perm",
                category = "Telephony",
                name = "Emergency SMS Dispatch",
                status = DiagnosticStatus.FAIL,
                detail = "SEND_SMS permission is not granted.",
                remediation = "Grant SMS permission so emergency alerts with live GPS can reach trusted contacts."
            )
            !isSimReady -> DiagnosticItem(
                id = "sms_sim",
                category = "Telephony",
                name = "SIM Card Status",
                status = DiagnosticStatus.WARNING,
                detail = "No active SIM card detected. Cellular SMS dispatch may fail without an operational carrier.",
                remediation = "Insert an active Smart, Globe, or DITO SIM card."
            )
            else -> DiagnosticItem(
                id = "sms_ready",
                category = "Telephony",
                name = "Emergency SMS Dispatch",
                status = DiagnosticStatus.PASS,
                detail = "SMS dispatch is authorized with an active SIM card detected."
            )
        }
    }

    private fun checkEmergencyContactsStatus(): DiagnosticItem {
        val savedContactsList = databaseHelper.getTrustedContacts()
        return when {
            savedContactsList.isEmpty() -> DiagnosticItem(
                id = "contacts_list",
                category = "Safety Net",
                name = "Trusted ICE Contacts",
                status = DiagnosticStatus.FAIL,
                detail = "Zero trusted emergency contacts are configured in the Contacts tab.",
                remediation = "Add at least one family member or guardian in the Contacts screen."
            )
            savedContactsList.none { it.isAuthorizedForSafetyCheck } -> DiagnosticItem(
                id = "contacts_auth",
                category = "Safety Net",
                name = "Trusted ICE Contacts",
                status = DiagnosticStatus.WARNING,
                detail = "${savedContactsList.size} contact(s) configured, but none are authorized to receive emergency SMS.",
                remediation = "Enable 'Authorize Emergency SMS' on your trusted contact."
            )
            else -> DiagnosticItem(
                id = "contacts_ready",
                category = "Safety Net",
                name = "Trusted ICE Contacts",
                status = DiagnosticStatus.PASS,
                detail = "${savedContactsList.count { it.isAuthorizedForSafetyCheck }} contact(s) configured and authorized to receive emergency alerts."
            )
        }
    }

    private fun checkNotificationsStatus(): DiagnosticItem {
        val areNotificationsGloballyEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!areNotificationsGloballyEnabled) {
            return DiagnosticItem(
                id = "notif_system",
                category = "Alerts",
                name = "Heads-Up Notifications",
                status = DiagnosticStatus.FAIL,
                detail = "Notifications are blocked at the system level.",
                remediation = "Enable Notifications for ResQLink AI in Android Settings."
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPostNotificationsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPostNotificationsPermission) {
                return DiagnosticItem(
                    id = "notif_perm",
                    category = "Alerts",
                    name = "Heads-Up Notifications",
                    status = DiagnosticStatus.FAIL,
                    detail = "POST_NOTIFICATIONS permission not granted (Android 13+).",
                    remediation = "Allow notification permission when prompted."
                )
            }
        }

        return DiagnosticItem(
            id = "notif_ready",
            category = "Alerts",
            name = "Heads-Up Notifications",
            status = DiagnosticStatus.PASS,
            detail = "High-priority emergency alerts and lockscreen HUD are operational."
        )
    }

    private fun checkBatteryOptimizationStatus(): DiagnosticItem {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        return if (isIgnoringBatteryOptimizations) {
            DiagnosticItem(
                id = "battery_opt",
                category = "Reliability",
                name = "Background Battery Optimization",
                status = DiagnosticStatus.PASS,
                detail = "App is exempted from aggressive Android battery sleeping."
            )
        } else {
            DiagnosticItem(
                id = "battery_opt",
                category = "Reliability",
                name = "Background Battery Optimization",
                status = DiagnosticStatus.WARNING,
                detail = "Battery optimization is active; Android OS may terminate background Guardian monitoring under low power.",
                remediation = "Select 'Unrestricted' battery usage in App Info settings for continuous reliability."
            )
        }
    }

    private fun checkCellularNetworkStatus(): DiagnosticItem {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val carrierNetworkName = telephonyManager?.networkOperatorName
        return if (!carrierNetworkName.isNullOrBlank()) {
            DiagnosticItem(
                id = "cellular_carrier",
                category = "Telephony",
                name = "Carrier Network",
                status = DiagnosticStatus.PASS,
                detail = "Registered on carrier: $carrierNetworkName"
            )
        } else {
            DiagnosticItem(
                id = "cellular_carrier",
                category = "Telephony",
                name = "Carrier Network",
                status = DiagnosticStatus.WARNING,
                detail = "No cellular carrier operator detected. Voice calls and SMS require active network signal."
            )
        }
    }
}
