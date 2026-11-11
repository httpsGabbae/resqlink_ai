package com.resqlink.ai.data.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.HotlineCategory
import com.resqlink.ai.domain.model.SimPreference

/**
 * # DetectedSim
 *
 * Represents a physical SIM card currently inserted into the Android phone.
 *
 * @property slotIndex 0 for SIM Slot 1, 1 for SIM Slot 2.
 * @property displayName User-friendly label (e.g. "SIM 1: Smart Communications").
 * @property carrierName Name of the telecommunications carrier (e.g. "Smart", "Globe", "DITO").
 */
data class DetectedSim(
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String
)

/**
 * # CallManager
 *
 * Responsible for placing outgoing cellular phone calls during an emergency.
 *
 * Ordinary Human Overview:
 * When an emergency is triggered, this class determines the proper Android Intent:
 * 1. **911 (Universal Emergency)**: The Android operating system strictly prevents 3rd-party
 *    apps from silently calling 911 via `ACTION_CALL` without user intervention (for safety).
 *    ResQLink opens the system dialer via `ACTION_DIAL`, allowing Android's Emergency Location
 *    Service (ELS) and cellular emergency roaming to take over.
 * 2. **Local Municipal Stations & Trusted Contacts**: If the user has granted the `CALL_PHONE`
 *    permission, ResQLink places an immediate direct call via `ACTION_CALL` with zero extra taps.
 * 3. **Dual-SIM Routing**: Injects multi-OEM intent extras (supporting MediaTek, Qualcomm, Samsung,
 *    Transsion, Xiaomi, and Unisoc chipsets) so the call goes through the user's preferred SIM card.
 */
class CallManager(private val context: Context) {

    /**
     * Inspects the active SIM subscriptions on the phone to detect inserted SIM cards.
     *
     * @return A list of [DetectedSim] objects sorted by slot index (Slot 1, Slot 2).
     */
    fun getDetectedSims(): List<DetectedSim> {
        val detectedSimList = mutableListOf<DetectedSim>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED

                if (subscriptionManager != null && hasPhoneStatePermission) {
                    val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList
                    activeSubscriptions?.forEach { subscriptionInfo ->
                        val carrier = subscriptionInfo.carrierName?.toString()?.ifBlank { null }
                        val display = subscriptionInfo.displayName?.toString()?.ifBlank { null }
                        val resolvedName = carrier ?: display ?: "SIM ${subscriptionInfo.simSlotIndex + 1}"
                        detectedSimList.add(
                            DetectedSim(
                                slotIndex = subscriptionInfo.simSlotIndex,
                                displayName = "SIM ${subscriptionInfo.simSlotIndex + 1}: $resolvedName",
                                carrierName = resolvedName
                            )
                        )
                    }
                }
            }
        } catch (ignoredException: Exception) {
            // Silently fall back if the device manufacturer restricts subscription inspection
        }
        return detectedSimList.sortedBy { it.slotIndex }
    }

    /**
     * Injects standard and OEM-specific dual-SIM extras into the call Intent.
     * This avoids the "Choose SIM" popup from blocking an urgent emergency call.
     */
    private fun applySimPreference(callIntent: Intent, simPreference: SimPreference) {
        val targetSlotIndex = simPreference.slotIndex ?: return

        // Universal SIM slot extras recognized by various Android phone manufacturers:
        // MediaTek, Qualcomm, Samsung, Transsion (Infinix/Tecno), Xiaomi, and Unisoc
        callIntent.putExtra("com.android.phone.extra.slot", targetSlotIndex)
        callIntent.putExtra("simSlot", targetSlotIndex)
        callIntent.putExtra("slot", targetSlotIndex)
        callIntent.putExtra("sim_slot", targetSlotIndex)
        callIntent.putExtra("com.android.phone.ForceConnectSim", targetSlotIndex)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

                val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED

                if (subscriptionManager != null && hasPhoneStatePermission) {
                    val targetSubscription = subscriptionManager.activeSubscriptionInfoList
                        ?.firstOrNull { it.simSlotIndex == targetSlotIndex }

                    if (targetSubscription != null) {
                        callIntent.putExtra("subscription", targetSubscription.subscriptionId)
                        callIntent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", targetSubscription.subscriptionId)

                        // If supported, bind the specific Telecom PhoneAccountHandle
                        if (telecomManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            val capableAccounts = telecomManager.callCapablePhoneAccounts
                            for (accountHandle in capableAccounts) {
                                if (accountHandle.id.contains(targetSubscription.subscriptionId.toString()) ||
                                    accountHandle.id.contains(targetSlotIndex.toString())) {
                                    callIntent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accountHandle)
                                    callIntent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", accountHandle)
                                    break
                                }
                            }
                        }
                    }
                }
            }
        } catch (ignoredException: Exception) {
            // Standard slot extras applied above serve as the universal fallback
        }
    }

    /**
     * Initiates an emergency phone call to the designated station.
     *
     * @param hotline The emergency hotline to call.
     * @param simPreference The user's preferred SIM card slot.
     * @return True if the call Intent was successfully handed off to the Android system.
     */
    fun initiateEmergencyCall(
        hotline: Hotline,
        simPreference: SimPreference = SimPreference.SYSTEM_DEFAULT
    ): Boolean {
        return try {
            val sanitizedNumber = hotline.primaryNumber.replace(" ", "").replace("-", "")

            // 1. Safety check for National 911:
            // Android strictly requires non-system apps to use ACTION_DIAL for national emergency numbers.
            if (sanitizedNumber == "911" || hotline.category == HotlineCategory.UNIVERSAL_911) {
                return dial911(simPreference)
            }

            // 2. For local municipal DRRMO, police, and hospitals:
            // Use ACTION_CALL if the CALL_PHONE permission is granted for immediate hands-free calling.
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val callIntent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$sanitizedNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$sanitizedNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            applySimPreference(callIntent, simPreference)
            context.startActivity(callIntent)
            true
        } catch (primaryException: Exception) {
            // Failsafe: Fallback to the standard Android system dialer pad
            try {
                val fallbackIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${hotline.primaryNumber}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                applySimPreference(fallbackIntent, simPreference)
                context.startActivity(fallbackIntent)
                true
            } catch (fallbackException: Exception) {
                false
            }
        }
    }

    /**
     * Opens the native Android emergency dialer pre-filled with 911.
     */
    fun dial911(simPreference: SimPreference = SimPreference.SYSTEM_DEFAULT): Boolean {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:911")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            applySimPreference(dialIntent, simPreference)
            context.startActivity(dialIntent)
            true
        } catch (exception: Exception) {
            false
        }
    }

    /**
     * Dials a trusted family member or ICE contact.
     *
     * @param phoneNumber The recipient's telephone number.
     * @param simPreference The user's preferred SIM card slot.
     */
    fun dialFamilyMember(
        phoneNumber: String,
        simPreference: SimPreference = SimPreference.SYSTEM_DEFAULT
    ): Boolean {
        return try {
            val sanitizedNumber = phoneNumber.replace(" ", "").replace("-", "")
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val callIntent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$sanitizedNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$sanitizedNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            applySimPreference(callIntent, simPreference)
            context.startActivity(callIntent)
            true
        } catch (exception: Exception) {
            false
        }
    }
}
