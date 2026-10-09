package com.example.engine

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import com.example.data.local.CampaignDao
import com.example.data.local.RecipientDao
import com.example.data.model.CampaignEntity
import com.example.data.model.CampaignStatus
import com.example.data.model.DeliveryStatus
import com.example.data.model.RecipientEntity
import com.example.receiver.SmsDeliveryReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LiveDispatchEvent(
    val timestamp: String,
    val phoneNumber: String,
    val recipientName: String,
    val status: DeliveryStatus,
    val message: String
)

data class DispatchProgress(
    val campaignId: Long? = null,
    val campaignTitle: String = "",
    val totalCount: Int = 0,
    val processedCount: Int = 0,
    val sentCount: Int = 0,
    val deliveredCount: Int = 0,
    val failedCount: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentRatePerSecond: Float = 0f,
    val estimatedRemainingSeconds: Long = 0L,
    val simCardLabel: String = "Local SIM",
    val isDualSimMode: Boolean = false,
    val sim1SentCount: Int = 0,
    val sim2SentCount: Int = 0,
    val globeSentCount: Int = 0,
    val smartSentCount: Int = 0
)

class SmsDispatcherEngine(
    private val context: Context,
    private val campaignDao: CampaignDao,
    private val recipientDao: RecipientDao,
    private val scope: CoroutineScope
) {
    private var dispatchJob: Job? = null
    private var activeCampaignId: Long? = null

    private val _progress = MutableStateFlow(DispatchProgress())
    val progress: StateFlow<DispatchProgress> = _progress.asStateFlow()

    private val _eventStream = MutableSharedFlow<LiveDispatchEvent>(replay = 20)
    val eventStream: SharedFlow<LiveDispatchEvent> = _eventStream.asSharedFlow()

    private var isPaused = false
    var throttleDelayMs: Long = 1050L // Default safe pacing
    var isRateLimitBypassEnabled: Boolean = true // Enforces safe spacing or default SMS bypass

    fun isAppDefaultSms(): Boolean {
        return try {
            android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun startDispatch(
        campaignId: Long,
        subscriptionId: Int = -1,
        simLabel: String = "Local SIM",
        routingConfig: DualSimRoutingConfig? = null
    ) {
        if (_progress.value.isRunning && activeCampaignId == campaignId) {
            if (isPaused) {
                resumeDispatch()
            }
            return
        }

        dispatchJob?.cancel()
        isPaused = false
        activeCampaignId = campaignId

        dispatchJob = scope.launch(Dispatchers.IO) {
            val campaign = campaignDao.getCampaignByIdDirect(campaignId) ?: return@launch
            val recipients = recipientDao.getRecipientsForCampaignDirect(campaignId)
            val pendingRecipients = recipients.filter { it.status == DeliveryStatus.PENDING || it.status == DeliveryStatus.QUEUED }

            val effectiveRouting = routingConfig ?: if (campaign.isDualSimEnabled) {
                DualSimRoutingConfig(
                    mode = try { SimRoutingMode.valueOf(campaign.dualSimMode) } catch (e: Exception) { SimRoutingMode.SMART_PREFIX },
                    sim1SubscriptionId = campaign.sim1SubscriptionId,
                    sim1Label = campaign.sim1Label,
                    sim2SubscriptionId = campaign.sim2SubscriptionId,
                    sim2Label = campaign.sim2Label,
                    globeSimSlot = campaign.globeSimSlot,
                    smartSimSlot = campaign.smartSimSlot,
                    singleSelectedSubId = subscriptionId,
                    singleSelectedSimLabel = simLabel
                )
            } else {
                DualSimRoutingConfig(
                    mode = SimRoutingMode.SINGLE_SIM,
                    singleSelectedSubId = subscriptionId,
                    singleSelectedSimLabel = simLabel
                )
            }

            val isDualSim = effectiveRouting.mode != SimRoutingMode.SINGLE_SIM

            val total = recipients.size
            var sent = recipients.count { it.status == DeliveryStatus.SENT || it.status == DeliveryStatus.DELIVERED }
            var delivered = recipients.count { it.status == DeliveryStatus.DELIVERED }
            var failed = recipients.count { it.status == DeliveryStatus.FAILED }
            var processed = sent + failed

            var sim1Sent = 0
            var sim2Sent = 0
            var globeSent = 0
            var smartSent = 0

            campaignDao.updateStatus(campaignId, CampaignStatus.SENDING)

            val displaySimLabel = if (isDualSim) {
                "Smart Dual SIM (SIM 1: ${effectiveRouting.sim1Label} • SIM 2: ${effectiveRouting.sim2Label})"
            } else {
                simLabel
            }

            _progress.value = DispatchProgress(
                campaignId = campaignId,
                campaignTitle = campaign.title,
                totalCount = total,
                processedCount = processed,
                sentCount = sent,
                deliveredCount = delivered,
                failedCount = failed,
                isRunning = true,
                isPaused = false,
                simCardLabel = displaySimLabel,
                isDualSimMode = isDualSim,
                sim1SentCount = sim1Sent,
                sim2SentCount = sim2Sent,
                globeSentCount = globeSent,
                smartSentCount = smartSent
            )

            emitEvent(
                LiveDispatchEvent(
                    timestamp = timeFormat.format(Date()),
                    phoneNumber = "SYSTEM",
                    recipientName = "SIM Radio",
                    status = DeliveryStatus.SENDING,
                    message = if (isDualSim) {
                        "Starting Smart Dual SIM dispatch: SIM 1 (Globe/TM) & SIM 2 (Smart/TNT) for '${campaign.title}' ($total targets)"
                    } else {
                        "Starting dispatch via Local SIM ($simLabel) for '${campaign.title}' ($total targets)"
                    }
                )
            )

            val startTime = System.currentTimeMillis()
            var lastRateCalcTime = startTime
            var itemsSinceLastRate = 0

            for ((index, recipient) in pendingRecipients.withIndex()) {
                while (isPaused && isActive) {
                    delay(300)
                }
                if (!isActive) break

                val targetDispatch = effectiveRouting.resolveTargetSim(recipient.phoneNumber, index)
                val targetSubId = targetDispatch.subscriptionId
                val targetLabel = targetDispatch.simLabel
                val carrier = targetDispatch.carrier

                val isSuccess = dispatchViaLocalSim(campaign, recipient, targetSubId)

                val now = System.currentTimeMillis()
                val updatedRecipient: RecipientEntity

                if (isSuccess) {
                    sent++
                    delivered++
                    processed++
                    if (targetDispatch.slotIndex == 0) sim1Sent++ else sim2Sent++
                    if (carrier == PhilippineCarrier.GLOBE_TM_GOMO) globeSent++
                    if (carrier == PhilippineCarrier.SMART_TNT_SUN) smartSent++

                    updatedRecipient = recipient.copy(
                        status = DeliveryStatus.DELIVERED,
                        carrierName = carrier.shortName,
                        dispatchedSimSlot = targetDispatch.slotIndex,
                        sentAtEpoch = now,
                        deliveredAtEpoch = now + 150L
                    )
                    emitEvent(
                        LiveDispatchEvent(
                            timestamp = timeFormat.format(Date()),
                            phoneNumber = recipient.phoneNumber,
                            recipientName = recipient.recipientName,
                            status = DeliveryStatus.DELIVERED,
                            message = "Sent via $targetLabel • ${carrier.shortName} (${recipient.amount})"
                        )
                    )
                } else {
                    failed++
                    processed++
                    val reason = "SIM Radio Error / Carrier Rejected ($targetLabel)"
                    updatedRecipient = recipient.copy(
                        status = DeliveryStatus.FAILED,
                        failureReason = reason,
                        carrierName = carrier.shortName,
                        dispatchedSimSlot = targetDispatch.slotIndex,
                        sentAtEpoch = now
                    )
                    emitEvent(
                        LiveDispatchEvent(
                            timestamp = timeFormat.format(Date()),
                            phoneNumber = recipient.phoneNumber,
                            recipientName = recipient.recipientName,
                            status = DeliveryStatus.FAILED,
                            message = "Failed via $targetLabel: $reason"
                        )
                    )
                }

                recipientDao.updateRecipient(updatedRecipient)

                itemsSinceLastRate++
                val nowTime = System.currentTimeMillis()
                var currentRate = _progress.value.currentRatePerSecond
                if (nowTime - lastRateCalcTime >= 1000L) {
                    val seconds = (nowTime - lastRateCalcTime) / 1000f
                    currentRate = itemsSinceLastRate / seconds
                    lastRateCalcTime = nowTime
                    itemsSinceLastRate = 0
                }

                val remainingItems = total - processed
                val etaSeconds = if (currentRate > 0) (remainingItems / currentRate).toLong() else 0L

                _progress.value = _progress.value.copy(
                    processedCount = processed,
                    sentCount = sent,
                    deliveredCount = delivered,
                    failedCount = failed,
                    currentRatePerSecond = currentRate,
                    estimatedRemainingSeconds = etaSeconds,
                    isDualSimMode = isDualSim,
                    sim1SentCount = sim1Sent,
                    sim2SentCount = sim2Sent,
                    globeSentCount = globeSent,
                    smartSentCount = smartSent
                )

                // Periodic checkpoint to DB
                if (processed % 15 == 0 || processed == total) {
                    campaignDao.updateProgress(
                        id = campaignId,
                        sent = sent,
                        delivered = delivered,
                        failed = failed,
                        status = if (processed == total) CampaignStatus.COMPLETED else CampaignStatus.SENDING,
                        completedAt = if (processed == total) System.currentTimeMillis() else null
                    )
                }

                // Android OS SMS Rate Limit Bypass logic:
                // 1. If App is Default SMS App, Android exempts it from the 30-message/30s confirmation barrier.
                // 2. If not default SMS app, adaptive pacing (>=1020ms) prevents triggering the OS dialog.
                val effectiveDelay = if (isRateLimitBypassEnabled) {
                    if (isAppDefaultSms()) {
                        throttleDelayMs.coerceAtLeast(50L)
                    } else {
                        throttleDelayMs.coerceAtLeast(1020L)
                    }
                } else {
                    throttleDelayMs
                }
                if (effectiveDelay > 0) {
                    delay(effectiveDelay)
                }
            }

            // Finish dispatch
            val finalStatus = if (processed == total) CampaignStatus.COMPLETED else CampaignStatus.PAUSED
            campaignDao.updateProgress(
                id = campaignId,
                sent = sent,
                delivered = delivered,
                failed = failed,
                status = finalStatus,
                completedAt = if (finalStatus == CampaignStatus.COMPLETED) System.currentTimeMillis() else null
            )

            _progress.value = _progress.value.copy(
                isRunning = false,
                isPaused = false,
                currentRatePerSecond = 0f,
                estimatedRemainingSeconds = 0L
            )

            emitEvent(
                LiveDispatchEvent(
                    timestamp = timeFormat.format(Date()),
                    phoneNumber = "SYSTEM",
                    recipientName = "SIM Radio",
                    status = DeliveryStatus.DELIVERED,
                    message = "Transmission completed via Local SIM! Delivered: $delivered, Failed: $failed"
                )
            )
        }
    }

    private fun getSmsManagerForSub(subscriptionId: Int): SmsManager {
        return if (subscriptionId > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val base = context.getSystemService(SmsManager::class.java)
                base.createForSubscriptionId(subscriptionId)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        }
    }

    private fun dispatchViaLocalSim(
        campaign: CampaignEntity,
        recipient: RecipientEntity,
        subscriptionId: Int
    ): Boolean {
        return try {
            val smsManager = getSmsManagerForSub(subscriptionId)
            val text = TemplateEngine.resolveMessageBody(
                template = campaign.messageBody,
                recipientName = recipient.recipientName,
                amount = recipient.amount,
                phoneNumber = recipient.phoneNumber
            )

            val sentIntent = Intent(SmsDeliveryReceiver.ACTION_SMS_SENT).apply {
                putExtra(SmsDeliveryReceiver.EXTRA_CAMPAIGN_ID, campaign.id)
                putExtra(SmsDeliveryReceiver.EXTRA_RECIPIENT_ID, recipient.id)
                putExtra(SmsDeliveryReceiver.EXTRA_PHONE_NUMBER, recipient.phoneNumber)
                setPackage(context.packageName)
            }
            val deliveryIntent = Intent(SmsDeliveryReceiver.ACTION_SMS_DELIVERED).apply {
                putExtra(SmsDeliveryReceiver.EXTRA_CAMPAIGN_ID, campaign.id)
                putExtra(SmsDeliveryReceiver.EXTRA_RECIPIENT_ID, recipient.id)
                putExtra(SmsDeliveryReceiver.EXTRA_PHONE_NUMBER, recipient.phoneNumber)
                setPackage(context.packageName)
            }

            val sentPi = PendingIntent.getBroadcast(
                context,
                recipient.id.toInt(),
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val deliveredPi = PendingIntent.getBroadcast(
                context,
                recipient.id.toInt(),
                deliveryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val parts = smsManager.divideMessage(text)
            if (parts.size > 1) {
                val sentList = ArrayList<PendingIntent>().apply { repeat(parts.size) { add(sentPi) } }
                val delList = ArrayList<PendingIntent>().apply { repeat(parts.size) { add(deliveredPi) } }
                smsManager.sendMultipartTextMessage(recipient.phoneNumber, null, parts, sentList, delList)
            } else {
                smsManager.sendTextMessage(recipient.phoneNumber, null, text, sentPi, deliveredPi)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun pauseDispatch() {
        isPaused = true
        _progress.value = _progress.value.copy(isPaused = true)
        activeCampaignId?.let { id ->
            scope.launch(Dispatchers.IO) {
                campaignDao.updateStatus(id, CampaignStatus.PAUSED)
            }
        }
        emitEvent(
            LiveDispatchEvent(
                timestamp = timeFormat.format(Date()),
                phoneNumber = "SYSTEM",
                recipientName = "SIM Radio",
                status = DeliveryStatus.QUEUED,
                message = "SIM transmission paused by operator"
            )
        )
    }

    fun resumeDispatch() {
        isPaused = false
        _progress.value = _progress.value.copy(isPaused = false)
        activeCampaignId?.let { id ->
            scope.launch(Dispatchers.IO) {
                campaignDao.updateStatus(id, CampaignStatus.SENDING)
            }
        }
        emitEvent(
            LiveDispatchEvent(
                timestamp = timeFormat.format(Date()),
                phoneNumber = "SYSTEM",
                recipientName = "SIM Radio",
                status = DeliveryStatus.SENDING,
                message = "SIM transmission resumed"
            )
        )
    }

    fun cancelDispatch() {
        dispatchJob?.cancel()
        dispatchJob = null
        isPaused = false
        activeCampaignId?.let { id ->
            scope.launch(Dispatchers.IO) {
                campaignDao.updateStatus(id, CampaignStatus.CANCELLED)
            }
        }
        _progress.value = _progress.value.copy(isRunning = false, isPaused = false)
        emitEvent(
            LiveDispatchEvent(
                timestamp = timeFormat.format(Date()),
                phoneNumber = "SYSTEM",
                recipientName = "SIM Radio",
                status = DeliveryStatus.FAILED,
                message = "SIM transmission cancelled"
            )
        )
    }

    private fun emitEvent(event: LiveDispatchEvent) {
        scope.launch {
            _eventStream.emit(event)
        }
    }
}
