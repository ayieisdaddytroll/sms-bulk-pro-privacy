package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CampaignStatus {
    DRAFT,
    SCHEDULED,
    SENDING,
    PAUSED,
    COMPLETED,
    CANCELLED
}

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val senderId: String = "SMSBLAST",
    val messageBody: String,
    val scheduledTimeEpoch: Long? = null,
    val status: CampaignStatus = CampaignStatus.DRAFT,
    val simSubscriptionId: Int = -1,
    val simLabel: String = "Local SIM",
    val isDualSimEnabled: Boolean = false,
    val dualSimMode: String = "SMART_PREFIX", // SMART_PREFIX, SINGLE_SIM, ROUND_ROBIN
    val sim1SubscriptionId: Int = -1,
    val sim1Label: String = "SIM 1 (Globe / TM)",
    val sim2SubscriptionId: Int = -1,
    val sim2Label: String = "SIM 2 (Smart / TNT)",
    val globeSimSlot: Int = 0, // 0 for SIM 1, 1 for SIM 2
    val smartSimSlot: Int = 1, // 1 for SIM 2, 0 for SIM 1
    val totalRecipients: Int = 0,
    val sentCount: Int = 0,
    val deliveredCount: Int = 0,
    val failedCount: Int = 0,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val startedAtEpoch: Long? = null,
    val completedAtEpoch: Long? = null
) {
    val deliveryRate: Float
        get() = if (sentCount > 0) (deliveredCount.toFloat() / sentCount.toFloat()) * 100f else 0f
}
