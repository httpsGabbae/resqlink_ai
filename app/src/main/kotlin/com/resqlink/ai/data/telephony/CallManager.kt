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

data class DetectedSim(
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String
)

class CallManager(private val context: Context) {

    /**
     * Inspect active SIM subscriptions on the device (Lollipop MR1+).
     */
    fun getDetectedSims(): List<DetectedSim> {
        val list = mutableListOf<DetectedSim>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val hasPhoneState = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED

                if (subManager != null && hasPhoneState) {
                    val subs = subManager.activeSubscriptionInfoList
                    subs?.forEach { sub ->
                        val carrier = sub.carrierName?.toString()?.ifBlank { null }
                        val display = sub.displayName?.toString()?.ifBlank { null }
                        val name = carrier ?: display ?: "SIM ${sub.simSlotIndex + 1}"
                        list.add(
                            DetectedSim(
                                slotIndex = sub.simSlotIndex,
                                displayName = "SIM ${sub.simSlotIndex + 1}: $name",
                                carrierName = name
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Permission or OEM sandbox limitation
        }
        return list.sortedBy { it.slotIndex }
    }

    /**
     * Injects standard and OEM-specific dual SIM extras into the call Intent.
     */
    private fun applySimPreference(intent: Intent, simPreference: SimPreference) {
        val slot = simPreference.slotIndex ?: return

        // Universal SIM slot extras (MediaTek, Qualcomm, Samsung, Transsion, Xiaomi, Unisoc)
        intent.putExtra("com.android.phone.extra.slot", slot)
        intent.putExtra("simSlot", slot)
        intent.putExtra("slot", slot)
        intent.putExtra("sim_slot", slot)
        intent.putExtra("com.android.phone.ForceConnectSim", slot)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager

                val hasPhoneState = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED

                if (subManager != null && hasPhoneState) {
                    val targetSub = subManager.activeSubscriptionInfoList?.firstOrNull { it.simSlotIndex == slot }
                    if (targetSub != null) {
                        intent.putExtra("subscription", targetSub.subscriptionId)
                        intent.putExtra("android.telephony.extra.SUBSCRIPTION_INDEX", targetSub.subscriptionId)

                        // TelecomManager PhoneAccountHandle injection
                        if (telecomManager != null && ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            val accounts = telecomManager.callCapablePhoneAccounts
                            for (account in accounts) {
                                if (account.id.contains(targetSub.subscriptionId.toString()) ||
                                    account.id.contains(slot.toString())) {
                                    intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, account)
                                    intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", account)
                                    break
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Standard slot extras above are already applied
        }
    }

    fun initiateEmergencyCall(
        hotline: Hotline,
        simPreference: SimPreference = SimPreference.SYSTEM_DEFAULT
    ): Boolean {
        return try {
            val cleanNumber = hotline.primaryNumber.replace(" ", "").replace("-", "")

            // For 911: Android OS strictly restricts ACTION_CALL for emergency numbers on non-system apps.
            // Route seamlessly to ACTION_DIAL so Android's native emergency framework handles cellular roaming & ELS.
            if (cleanNumber == "911" || hotline.category == HotlineCategory.UNIVERSAL_911) {
                return dial911(simPreference)
            }

            // For Local Municipalities / DRRMO / Police Stations:
            // Use ACTION_CALL if permission is granted for zero-click dialing, otherwise fall back to ACTION_DIAL.
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            applySimPreference(intent, simPreference)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Failsafe: Try basic dialer
            try {
                val fallbackIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${hotline.primaryNumber}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                applySimPreference(fallbackIntent, simPreference)
                context.startActivity(fallbackIntent)
                true
            } catch (fallbackEx: Exception) {
                false
            }
        }
    }

    fun dial911(simPreference: SimPreference = SimPreference.SYSTEM_DEFAULT): Boolean {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:911")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            applySimPreference(dialIntent, simPreference)
            context.startActivity(dialIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun dialFamilyMember(
        phoneNumber: String,
        simPreference: SimPreference = SimPreference.SYSTEM_DEFAULT
    ): Boolean {
        return try {
            val cleanNumber = phoneNumber.replace(" ", "").replace("-", "")
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$cleanNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            applySimPreference(intent, simPreference)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
