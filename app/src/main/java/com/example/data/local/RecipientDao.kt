package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DeliveryStatus
import com.example.data.model.RecipientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipientDao {

    @Query("SELECT * FROM recipients WHERE campaignId = :campaignId ORDER BY id ASC")
    fun getRecipientsForCampaign(campaignId: Long): Flow<List<RecipientEntity>>

    @Query("SELECT * FROM recipients WHERE campaignId = :campaignId ORDER BY id ASC")
    suspend fun getRecipientsForCampaignDirect(campaignId: Long): List<RecipientEntity>

    @Query("SELECT * FROM recipients WHERE campaignId = :campaignId AND status = :status ORDER BY id ASC")
    fun getRecipientsByStatus(campaignId: Long, status: DeliveryStatus): Flow<List<RecipientEntity>>

    @Query("SELECT * FROM recipients WHERE campaignId = :campaignId AND status = :status ORDER BY id ASC")
    suspend fun getRecipientsByStatusDirect(campaignId: Long, status: DeliveryStatus): List<RecipientEntity>

    @Query("SELECT COUNT(*) FROM recipients WHERE campaignId = :campaignId AND status = :status")
    fun countByStatus(campaignId: Long, status: DeliveryStatus): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipients(recipients: List<RecipientEntity>)

    @Update
    suspend fun updateRecipient(recipient: RecipientEntity)

    @Query("""
        UPDATE recipients 
        SET status = :status, failureReason = :reason, sentAtEpoch = :sentAt, deliveredAtEpoch = :deliveredAt
        WHERE id = :id
    """)
    suspend fun updateDeliveryState(
        id: Long,
        status: DeliveryStatus,
        reason: String?,
        sentAt: Long?,
        deliveredAt: Long?
    )

    @Query("""
        UPDATE recipients 
        SET status = :status, failureReason = :reason, sentAtEpoch = :sentAt, deliveredAtEpoch = :deliveredAt
        WHERE id IN (:ids)
    """)
    suspend fun updateBatchDeliveryState(
        ids: List<Long>,
        status: DeliveryStatus,
        reason: String?,
        sentAt: Long?,
        deliveredAt: Long?
    )

    @Query("SELECT * FROM recipients WHERE campaignId = :campaignId AND (phoneNumber LIKE '%' || :query || '%' OR recipientName LIKE '%' || :query || '%') LIMIT 100")
    fun searchRecipients(campaignId: Long, query: String): Flow<List<RecipientEntity>>
}
