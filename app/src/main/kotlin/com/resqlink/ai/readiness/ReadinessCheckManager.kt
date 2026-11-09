package com.resqlink.ai.readiness

import android.Manifest
import android.app.NotificationManager
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

class ReadinessCheckManager(
    private val context: Context,
    private val dbHelper: HotlineDatabaseHelper = HotlineDatabaseHelper(context)
) {

    fun runDiagnostics(): ReadinessReport {
        val items = mutableListOf<DiagnosticItem>()

        // 1. Microphone Hardware & Permission
        items.add(checkMicrophone())

        // 2. Guardian Mode Service State
        items.add(checkGuardianService())

        // 3. Location Hardware & Permission
        items.add(checkLocation())

        // 4. SMS Dispatch Capabilities
        items.add(checkSms())

        // 5. Emergency Contacts Configuration
        items.add(checkEmergencyContacts())

        // 6. Notifications & Heads-Up Alerts
        items.add(checkNotifications())

        // 7. Battery Optimization / Background Restrictions
        items.add(checkBatteryOptimization())

        // 8. Cellular Network & Telephony
        items.add(checkCellularNetwork())

        val overallPass = items.none { it.status == DiagnosticStatus.FAIL }

        return ReadinessReport(
            overallPass = overallPass,
            timestamp = System.currentTimeMillis(),
            items = items
        )
    }

    private fun checkMicrophone(): DiagnosticItem {
        val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        if (!hasFeature) {
            return DiagnosticItem(
                id = "mic_hardware",
                category = "Sensors",
                name = "Microphone Hardware",
                status = DiagnosticStatus.UNAVAILABLE,
                detail = "Device does not report built-in microphone hardware."
            )
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasPermission) {
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

    private fun checkGuardianService(): DiagnosticItem {
        val isRunning = GuardianForegroundService.isRunning
        return if (isRunning) {
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

    private fun checkLocation(): DiagnosticItem {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) {
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
        val isGpsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled == true
        } else {
            @Suppress("DEPRECATION")
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }

        return if (isGpsEnabled) {
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

    private fun checkSms(): DiagnosticItem {
        val hasSmsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val hasSim = telephonyManager?.simState == TelephonyManager.SIM_STATE_READY

        return when {
            !hasSmsPerm -> DiagnosticItem(
                id = "sms_perm",
                category = "Telephony",
                name = "Emergency SMS Dispatch",
                status = DiagnosticStatus.FAIL,
                detail = "SEND_SMS permission is not granted.",
                remediation = "Grant SMS permission so emergency alerts with live GPS can reach trusted contacts."
            )
            !hasSim -> DiagnosticItem(
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

    private fun checkEmergencyContacts(): DiagnosticItem {
        val contacts = dbHelper.getTrustedContacts()
        return when {
            contacts.isEmpty() -> DiagnosticItem(
                id = "contacts_list",
                category = "Safety Net",
                name = "Trusted ICE Contacts",
                status = DiagnosticStatus.FAIL,
                detail = "Zero trusted emergency contacts are configured in the Contacts tab.",
                remediation = "Add at least one family member or guardian in the Contacts screen."
            )
            contacts.none { it.isAuthorizedForSafetyCheck } -> DiagnosticItem(
                id = "contacts_auth",
                category = "Safety Net",
                name = "Trusted ICE Contacts",
                status = DiagnosticStatus.WARNING,
                detail = "${contacts.size} contact(s) configured, but none are authorized to receive emergency SMS.",
                remediation = "Enable 'Authorize Emergency SMS' on your trusted contact."
            )
            else -> DiagnosticItem(
                id = "contacts_ready",
                category = "Safety Net",
                name = "Trusted ICE Contacts",
                status = DiagnosticStatus.PASS,
                detail = "${contacts.count { it.isAuthorizedForSafetyCheck }} contact(s) configured and authorized to receive emergency alerts."
            )
        }
    }

    private fun checkNotifications(): DiagnosticItem {
        val areEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!areEnabled) {
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
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
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

    private fun checkBatteryOptimization(): DiagnosticItem {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

        return if (isIgnoring) {
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

    private fun checkCellularNetwork(): DiagnosticItem {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val networkOperator = telephonyManager?.networkOperatorName
        return if (!networkOperator.isNullOrBlank()) {
            DiagnosticItem(
                id = "cellular_carrier",
                category = "Telephony",
                name = "Carrier Network",
                status = DiagnosticStatus.PASS,
                detail = "Registered on carrier: $networkOperator"
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
