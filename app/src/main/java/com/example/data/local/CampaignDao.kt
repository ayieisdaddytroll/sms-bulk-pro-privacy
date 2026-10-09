package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CampaignEntity
import com.example.data.model.CampaignStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {

    @Query("SELECT * FROM campaigns ORDER BY createdAtEpoch DESC")
    fun getAllCampaigns(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    fun getCampaignById(id: Long): Flow<CampaignEntity?>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    suspend fun getCampaignByIdDirect(id: Long): CampaignEntity?

    @Query("SELECT * FROM campaigns WHERE status = :status ORDER BY createdAtEpoch DESC")
    fun getCampaignsByStatus(status: CampaignStatus): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE status = 'SENDING' LIMIT 1")
    fun getActiveSendingCampaign(): Flow<CampaignEntity?>

    @Query("SELECT * FROM campaigns WHERE status = 'SCHEDULED' AND scheduledTimeEpoch <= :currentEpoch ORDER BY scheduledTimeEpoch ASC")
    suspend fun getDueScheduledCampaigns(currentEpoch: Long): List<CampaignEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity): Long

    @Update
    suspend fun updateCampaign(campaign: CampaignEntity)

    @Query("UPDATE campaigns SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: CampaignStatus)

    @Query("""
        UPDATE campaigns 
        SET sentCount = :sent, deliveredCount = :delivered, failedCount = :failed, 
            status = :status, completedAtEpoch = :completedAt
        WHERE id = :id
    """)
    suspend fun updateProgress(
        id: Long,
        sent: Int,
        delivered: Int,
        failed: Int,
        status: CampaignStatus,
        completedAt: Long?
    )

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun deleteCampaign(id: Long)

    @Query("DELETE FROM campaigns WHERE id IN (:ids)")
    suspend fun deleteCampaigns(ids: List<Long>)
}
