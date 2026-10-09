package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DeliveryStatus {
    PENDING,
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

@Entity(
    tableName = "recipients",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["campaignId"]),
        Index(value = ["campaignId", "status"]),
        Index(value = ["phoneNumber"])
    ]
)
data class RecipientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val campaignId: Long,
    val phoneNumber: String,
    val recipientName: String = "",
    val amount: String = "$0.00",
    val carrierName: String = "",
    val dispatchedSimSlot: Int? = null,
    val status: DeliveryStatus = DeliveryStatus.PENDING,
    val failureReason: String? = null,
    val sentAtEpoch: Long? = null,
    val deliveredAtEpoch: Long? = null
)
