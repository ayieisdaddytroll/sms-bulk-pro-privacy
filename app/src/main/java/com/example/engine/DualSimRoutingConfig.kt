package com.example.engine

enum class SimRoutingMode(
    val title: String,
    val subtitle: String
) {
    SMART_PREFIX(
        title = "Smart Dual SIM (Prefix Auto-Routing)",
        subtitle = "SIM 1 routes Globe/TM • SIM 2 routes Smart/TNT"
    ),
    SINGLE_SIM(
        title = "Fixed Single SIM",
        subtitle = "Route all messages through one designated SIM card"
    ),
    ROUND_ROBIN(
        title = "Round-Robin Balancing",
        subtitle = "Alternate messages evenly between SIM 1 and SIM 2"
    )
}

data class TargetSimDispatch(
    val slotIndex: Int,
    val subscriptionId: Int,
    val simLabel: String,
    val carrier: PhilippineCarrier,
    val routingReason: String
)

data class DualSimRoutingConfig(
    val mode: SimRoutingMode = SimRoutingMode.SMART_PREFIX,
    val sim1SlotIndex: Int = 0,
    val sim1SubscriptionId: Int = -1,
    val sim1Label: String = "SIM 1 (Globe / TM)",
    val sim1CarrierName: String = "Globe Telecom",
    val sim2SlotIndex: Int = 1,
    val sim2SubscriptionId: Int = -1,
    val sim2Label: String = "SIM 2 (Smart / TNT)",
    val sim2CarrierName: String = "Smart Communications",
    val globeSimSlot: Int = 0, // 0 for SIM 1, 1 for SIM 2
    val smartSimSlot: Int = 1, // 1 for SIM 2, 0 for SIM 1
    val ditoSimSlot: Int = 0,  // 0 for SIM 1 by default
    val fallbackSimSlot: Int = 0,
    val singleSelectedSubId: Int = -1,
    val singleSelectedSimLabel: String = "SIM 1"
) {
    /**
     * Resolves which SIM card slot and subscriptionId should be used to send to this recipient.
     */
    fun resolveTargetSim(phoneNumber: String, index: Int = 0): TargetSimDispatch {
        if (mode == SimRoutingMode.SINGLE_SIM) {
            return TargetSimDispatch(
                slotIndex = if (singleSelectedSubId == sim2SubscriptionId && sim2SubscriptionId != -1) 1 else 0,
                subscriptionId = singleSelectedSubId,
                simLabel = singleSelectedSimLabel,
                carrier = PhilippineCarrierClassifier.detectCarrier(phoneNumber),
                routingReason = "Dispatched via fixed $singleSelectedSimLabel"
            )
        }

        if (mode == SimRoutingMode.ROUND_ROBIN) {
            val useSim2 = (index % 2 == 1)
            val slot = if (useSim2) 1 else 0
            val subId = if (useSim2) sim2SubscriptionId else sim1SubscriptionId
            val label = if (useSim2) sim2Label else sim1Label
            return TargetSimDispatch(
                slotIndex = slot,
                subscriptionId = subId,
                simLabel = label,
                carrier = PhilippineCarrierClassifier.detectCarrier(phoneNumber),
                routingReason = "Round-robin dispatched via $label"
            )
        }

        // SMART_PREFIX mode
        val carrierInfo = PhilippineCarrierClassifier.getCarrierInfo(phoneNumber)
        val carrier = carrierInfo.carrier
        val prefix = carrierInfo.detectedPrefix

        val targetSlot = when (carrier) {
            PhilippineCarrier.GLOBE_TM_GOMO -> globeSimSlot
            PhilippineCarrier.SMART_TNT_SUN -> smartSimSlot
            PhilippineCarrier.DITO -> ditoSimSlot
            PhilippineCarrier.UNKNOWN -> fallbackSimSlot
        }

        val targetSubId = if (targetSlot == 0) sim1SubscriptionId else sim2SubscriptionId
        val targetLabel = if (targetSlot == 0) sim1Label else sim2Label

        val reason = when (carrier) {
            PhilippineCarrier.GLOBE_TM_GOMO -> "Routed via $targetLabel (Globe/TM prefix $prefix)"
            PhilippineCarrier.SMART_TNT_SUN -> "Routed via $targetLabel (Smart/TNT prefix $prefix)"
            PhilippineCarrier.DITO -> "Routed via $targetLabel (DITO prefix $prefix)"
            PhilippineCarrier.UNKNOWN -> "Routed via $targetLabel (Default fallback)"
        }

        return TargetSimDispatch(
            slotIndex = targetSlot,
            subscriptionId = targetSubId,
            simLabel = targetLabel,
            carrier = carrier,
            routingReason = reason
        )
    }
}
