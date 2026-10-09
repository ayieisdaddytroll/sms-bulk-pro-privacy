package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.CampaignStatus
import com.example.data.model.DeliveryStatus
import com.example.receiver.SmsDeliveryReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IntentBroadcastFeedback(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val timeEpoch: Long = System.currentTimeMillis(),
    val action: String,
    val campaignId: Long,
    val recipientId: Long,
    val phoneNumber: String,
    val resultCode: Int,
    val isSuccess: Boolean,
    val status: DeliveryStatus,
    val statusDescription: String,
    val errorCodeName: String,
    val simSlot: Int = 1,
    val carrierTag: String = ""
)

data class DeliveryBroadcastSummary(
    val totalAcksReceived: Int = 0,
    val sentAcks: Int = 0,
    val deliveredAcks: Int = 0,
    val failedAcks: Int = 0,
    val sim1Acks: Int = 0,
    val sim2Acks: Int = 0,
    val lastEvent: IntentBroadcastFeedback? = null,
    val recentEvents: List<IntentBroadcastFeedback> = emptyList()
)

object DeliveryFeedbackBus {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _feedbackStream = MutableSharedFlow<IntentBroadcastFeedback>(replay = 50)
    val feedbackStream: SharedFlow<IntentBroadcastFeedback> = _feedbackStream.asSharedFlow()

    private val _summary = MutableStateFlow(DeliveryBroadcastSummary())
    val summary: StateFlow<DeliveryBroadcastSummary> = _summary.asStateFlow()

    fun handleBroadcast(context: Context, intent: Intent, resultCode: Int) {
        val action = intent.action ?: return
        val phone = intent.getStringExtra(SmsDeliveryReceiver.EXTRA_PHONE_NUMBER) ?: "SIM"
        val recipientId = intent.getLongExtra(SmsDeliveryReceiver.EXTRA_RECIPIENT_ID, -1L)
        val campaignId = intent.getLongExtra(SmsDeliveryReceiver.EXTRA_CAMPAIGN_ID, -1L)
        val simSlot = intent.getIntExtra("extra_sim_slot", 1)
        val carrierTag = intent.getStringExtra("extra_carrier") ?: ""
        val now = System.currentTimeMillis()
        val formattedTime = timeFormat.format(Date(now))

        val isSentAction = action == SmsDeliveryReceiver.ACTION_SMS_SENT
        val isDeliveredAction = action == SmsDeliveryReceiver.ACTION_SMS_DELIVERED

        val isSuccess = resultCode == Activity.RESULT_OK
        val errorCodeName = when (resultCode) {
            Activity.RESULT_OK -> "RESULT_OK"
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "RESULT_ERROR_GENERIC_FAILURE"
            SmsManager.RESULT_ERROR_NO_SERVICE -> "RESULT_ERROR_NO_SERVICE"
            SmsManager.RESULT_ERROR_NULL_PDU -> "RESULT_ERROR_NULL_PDU"
            SmsManager.RESULT_ERROR_RADIO_OFF -> "RESULT_ERROR_RADIO_OFF"
            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "RESULT_ERROR_LIMIT_EXCEEDED"
            else -> "ERROR_CODE_$resultCode"
        }

        val status: DeliveryStatus
        val description: String

        if (isSentAction) {
            if (isSuccess) {
                status = DeliveryStatus.SENT
                description = "Cellular SIM transmission confirmed (RESULT_OK)"
            } else {
                status = DeliveryStatus.FAILED
                description = when (resultCode) {
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "SIM Carrier Generic Failure"
                    SmsManager.RESULT_ERROR_NO_SERVICE -> "No Cellular Signal / Service"
                    SmsManager.RESULT_ERROR_NULL_PDU -> "Carrier Null PDU Error"
                    SmsManager.RESULT_ERROR_RADIO_OFF -> "Cellular Radio Off (Airplane Mode)"
                    SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Android SMS Pacing Limit Exceeded"
                    else -> "SIM Transmission Error (Code: $resultCode)"
                }
            }
        } else if (isDeliveredAction) {
            if (isSuccess) {
                status = DeliveryStatus.DELIVERED
                description = "Carrier Delivery Receipt confirmed at recipient handset"
            } else {
                status = DeliveryStatus.FAILED
                description = "Carrier handset delivery receipt failure ($errorCodeName)"
            }
        } else {
            return
        }

        val event = IntentBroadcastFeedback(
            timestamp = formattedTime,
            timeEpoch = now,
            action = action,
            campaignId = campaignId,
            recipientId = recipientId,
            phoneNumber = phone,
            resultCode = resultCode,
            isSuccess = isSuccess,
            status = status,
            statusDescription = description,
            errorCodeName = errorCodeName,
            simSlot = simSlot,
            carrierTag = carrierTag
        )

        // Update live in-memory state
        val currentSummary = _summary.value
        val newRecent = (listOf(event) + currentSummary.recentEvents).take(30)
        _summary.value = currentSummary.copy(
            totalAcksReceived = currentSummary.totalAcksReceived + 1,
            sentAcks = currentSummary.sentAcks + (if (status == DeliveryStatus.SENT) 1 else 0),
            deliveredAcks = currentSummary.deliveredAcks + (if (status == DeliveryStatus.DELIVERED) 1 else 0),
            failedAcks = currentSummary.failedAcks + (if (status == DeliveryStatus.FAILED) 1 else 0),
            sim1Acks = currentSummary.sim1Acks + (if (simSlot == 1) 1 else 0),
            sim2Acks = currentSummary.sim2Acks + (if (simSlot == 2) 1 else 0),
            lastEvent = event,
            recentEvents = newRecent
        )

        scope.launch {
            _feedbackStream.emit(event)
        }

        // Persist to Room Database asynchronously
        if (recipientId > 0) {
            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    val recipientDao = db.recipientDao()
                    val campaignDao = db.campaignDao()

                    val failureReason = if (status == DeliveryStatus.FAILED) description else null
                    val sentAt = if (status == DeliveryStatus.SENT || status == DeliveryStatus.DELIVERED) now else null
                    val deliveredAt = if (status == DeliveryStatus.DELIVERED) now else null

                    recipientDao.updateDeliveryState(
                        id = recipientId,
                        status = status,
                        reason = failureReason,
                        sentAt = sentAt,
                        deliveredAt = deliveredAt
                    )

                    // Refresh campaign aggregate counts
                    if (campaignId > 0) {
                        val allRecipients = recipientDao.getRecipientsForCampaignDirect(campaignId)
                        val total = allRecipients.size
                        val sentCount = allRecipients.count { it.status == DeliveryStatus.SENT || it.status == DeliveryStatus.DELIVERED }
                        val deliveredCount = allRecipients.count { it.status == DeliveryStatus.DELIVERED }
                        val failedCount = allRecipients.count { it.status == DeliveryStatus.FAILED }
                        val isComplete = (sentCount + failedCount) >= total && total > 0

                        campaignDao.updateProgress(
                            id = campaignId,
                            sent = sentCount,
                            delivered = deliveredCount,
                            failed = failedCount,
                            status = if (isComplete) CampaignStatus.COMPLETED else CampaignStatus.SENDING,
                            completedAt = if (isComplete) now else null
                        )
                    }
                } catch (e: Exception) {
                    Log.e("DeliveryFeedbackBus", "Error updating DB from broadcast: ${e.message}", e)
                }
            }
        }
    }
}
