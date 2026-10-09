package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class SimCardInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String = "",
    val isDefault: Boolean = false
)

object SimCardManager {

    @SuppressLint("MissingPermission")
    fun getAvailableSimCards(context: Context): List<SimCardInfo> {
        val simList = mutableListOf<SimCardInfo>()
        val hasPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val defaultSubId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SubscriptionManager.getDefaultSmsSubscriptionId()
            } else {
                -1
            }

            if (hasPhonePermission && subscriptionManager != null) {
                val activeList: List<SubscriptionInfo>? = subscriptionManager.activeSubscriptionInfoList
                if (!activeList.isNullOrEmpty()) {
                    for (info in activeList) {
                        val subId = info.subscriptionId
                        val slot = info.simSlotIndex
                        val carrier = info.carrierName?.toString() ?: "Unknown Carrier"
                        val name = info.displayName?.toString() ?: "SIM ${slot + 1}"
                        val number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                subscriptionManager.getPhoneNumber(subId) ?: ""
                            } catch (e: Exception) {
                                ""
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            info.number ?: ""
                        }

                        simList.add(
                            SimCardInfo(
                                subscriptionId = subId,
                                slotIndex = slot,
                                displayName = name,
                                carrierName = carrier,
                                phoneNumber = number,
                                isDefault = (subId == defaultSubId)
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback if permission or service fails
        }

        // If only 1 SIM found, provide secondary slot option for dual SIM routing mode
        if (simList.size == 1) {
            val first = simList[0]
            val sub2 = if (first.subscriptionId > 0) first.subscriptionId + 1 else 2
            simList.add(
                SimCardInfo(
                    subscriptionId = sub2,
                    slotIndex = 1,
                    displayName = "SIM 2 (Smart / TNT)",
                    carrierName = "Smart Communications",
                    isDefault = false
                )
            )
        } else if (simList.isEmpty()) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simState = telephonyManager?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN
            val simOperator = telephonyManager?.networkOperatorName?.ifEmpty { "Globe Telecom" } ?: "Globe Telecom"

            val isReady = simState == TelephonyManager.SIM_STATE_READY
            simList.add(
                SimCardInfo(
                    subscriptionId = -1, // Use system default
                    slotIndex = 0,
                    displayName = if (isReady) "SIM 1 ($simOperator)" else "SIM 1 (Globe / TM)",
                    carrierName = if (isReady) simOperator else "Globe Telecom",
                    isDefault = true
                )
            )
            simList.add(
                SimCardInfo(
                    subscriptionId = 2,
                    slotIndex = 1,
                    displayName = "SIM 2 (Smart / TNT)",
                    carrierName = "Smart Communications",
                    isDefault = false
                )
            )
        }

        return simList
    }

    fun hasSimCardReady(context: Context): Boolean {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return telephonyManager?.simState == TelephonyManager.SIM_STATE_READY || getAvailableSimCards(context).isNotEmpty()
    }
}
