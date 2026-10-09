package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SmsTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsTemplateDao {
    @Query("SELECT * FROM sms_templates ORDER BY isFavorite DESC, usageCount DESC, id DESC")
    fun getAllTemplates(): Flow<List<SmsTemplateEntity>>

    @Query("SELECT * FROM sms_templates WHERE category = :category ORDER BY isFavorite DESC, usageCount DESC, id DESC")
    fun getTemplatesByCategory(category: String): Flow<List<SmsTemplateEntity>>

    @Query("SELECT * FROM sms_templates WHERE isFavorite = 1 ORDER BY usageCount DESC, id DESC")
    fun getFavoriteTemplates(): Flow<List<SmsTemplateEntity>>

    @Query("SELECT * FROM sms_templates WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' ORDER BY isFavorite DESC, id DESC")
    fun searchTemplates(query: String): Flow<List<SmsTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: SmsTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<SmsTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: SmsTemplateEntity)

    @Query("UPDATE sms_templates SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE sms_templates SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("DELETE FROM sms_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)

    @Query("SELECT COUNT(*) FROM sms_templates")
    suspend fun getTemplateCount(): Int
}
