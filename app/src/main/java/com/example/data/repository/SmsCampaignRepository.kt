package com.example.data.repository

import com.example.data.local.CampaignDao
import com.example.data.local.RecipientDao
import com.example.data.local.SmsTemplateDao
import com.example.data.model.CampaignEntity
import com.example.data.model.CampaignStatus
import com.example.data.model.DeliveryStatus
import com.example.data.model.RecipientEntity
import com.example.data.model.SmsTemplateEntity
import com.example.engine.ParsedContact
import kotlinx.coroutines.flow.Flow

class SmsCampaignRepository(
    private val campaignDao: CampaignDao,
    private val recipientDao: RecipientDao,
    private val templateDao: SmsTemplateDao
) {
    val allCampaigns: Flow<List<CampaignEntity>> = campaignDao.getAllCampaigns()
    val allTemplates: Flow<List<SmsTemplateEntity>> = templateDao.getAllTemplates()

    fun getCampaignById(id: Long): Flow<CampaignEntity?> = campaignDao.getCampaignById(id)

    fun getRecipientsForCampaign(campaignId: Long): Flow<List<RecipientEntity>> =
        recipientDao.getRecipientsForCampaign(campaignId)

    fun getRecipientsByStatus(campaignId: Long, status: DeliveryStatus): Flow<List<RecipientEntity>> =
        recipientDao.getRecipientsByStatus(campaignId, status)

    suspend fun createCampaignWithRecipients(
        campaign: CampaignEntity,
        contacts: List<ParsedContact>
    ): Long {
        val total = contacts.size
        val initialCampaign = campaign.copy(
            totalRecipients = total,
            status = if (campaign.scheduledTimeEpoch != null && campaign.scheduledTimeEpoch > System.currentTimeMillis()) {
                CampaignStatus.SCHEDULED
            } else {
                CampaignStatus.DRAFT
            }
        )
        val campaignId = campaignDao.insertCampaign(initialCampaign)

        val recipientEntities = contacts.map { contact ->
            val carrier = com.example.engine.PhilippineCarrierClassifier.detectCarrier(contact.phoneNumber)
            RecipientEntity(
                campaignId = campaignId,
                phoneNumber = contact.phoneNumber,
                recipientName = contact.name,
                amount = contact.amount,
                carrierName = carrier.shortName,
                status = DeliveryStatus.QUEUED
            )
        }

        recipientDao.insertRecipients(recipientEntities)
        return campaignId
    }

    suspend fun getCampaignByIdDirect(id: Long): CampaignEntity? = campaignDao.getCampaignByIdDirect(id)

    suspend fun updateCampaignStatus(id: Long, status: CampaignStatus) {
        campaignDao.updateStatus(id, status)
    }

    suspend fun deleteCampaign(id: Long) {
        campaignDao.deleteCampaign(id)
    }

    suspend fun deleteCampaigns(ids: List<Long>) {
        campaignDao.deleteCampaigns(ids)
    }

    suspend fun insertTemplate(template: SmsTemplateEntity): Long {
        return templateDao.insertTemplate(template)
    }

    suspend fun updateTemplate(template: SmsTemplateEntity) {
        templateDao.updateTemplate(template)
    }

    suspend fun toggleFavoriteTemplate(id: Long, isFavorite: Boolean) {
        templateDao.setFavorite(id, isFavorite)
    }

    suspend fun incrementTemplateUsage(id: Long) {
        templateDao.incrementUsage(id)
    }

    suspend fun deleteTemplate(id: Long) {
        templateDao.deleteTemplate(id)
    }

    suspend fun checkDueScheduledCampaigns(): List<CampaignEntity> {
        val now = System.currentTimeMillis()
        return campaignDao.getDueScheduledCampaigns(now)
    }
}
