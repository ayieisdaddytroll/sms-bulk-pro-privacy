package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CampaignEntity
import com.example.data.model.CampaignStatus
import com.example.data.model.DeliveryStatus
import com.example.data.model.RecipientEntity
import com.example.data.model.SmsTemplateEntity
import com.example.data.repository.SmsCampaignRepository
import com.example.engine.ColumnMappingConfig
import com.example.engine.ContactImportService
import com.example.engine.CsvParseResult
import com.example.engine.CsvParser
import com.example.engine.DeliveryBroadcastSummary
import com.example.engine.DeliveryFeedbackBus
import com.example.engine.DispatchProgress
import com.example.engine.DualSimRoutingConfig
import com.example.engine.IntentBroadcastFeedback
import com.example.engine.LiveDispatchEvent
import com.example.engine.ParsedContact
import com.example.engine.PhilippineCarrier
import com.example.engine.PhilippineCarrierClassifier
import com.example.engine.RawImportSheet
import com.example.engine.SimCardInfo
import com.example.engine.SimCardManager
import com.example.engine.SimRoutingMode
import com.example.engine.SmsDispatcherEngine
import com.example.engine.TemplateEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NewCampaignFormState(
    val title: String = "",
    val senderId: String = "",
    val messageBody: String = "",
    val scheduledTimeEpoch: Long? = null,
    val contacts: List<ParsedContact> = emptyList(),
    val parseSummary: CsvParseResult? = null,
    val throttleSpeed: Long = 1050L,
    val bypassAndroidRateLimit: Boolean = true,
    val selectedSubscriptionId: Int = -1,
    val selectedSimLabel: String = "SIM 1 (Globe / TM)",
    val isDualSimEnabled: Boolean = true,
    val dualSimMode: SimRoutingMode = SimRoutingMode.SMART_PREFIX,
    val sim1SubscriptionId: Int = -1,
    val sim1Label: String = "SIM 1 (Globe / TM)",
    val sim2SubscriptionId: Int = -1,
    val sim2Label: String = "SIM 2 (Smart / TNT)",
    val globeSimSlot: Int = 0, // 0 for SIM 1, 1 for SIM 2
    val smartSimSlot: Int = 1, // 1 for SIM 2, 0 for SIM 1
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class CampaignViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = SmsCampaignRepository(
        campaignDao = database.campaignDao(),
        recipientDao = database.recipientDao(),
        templateDao = database.smsTemplateDao()
    )

    val dispatcherEngine = SmsDispatcherEngine(
        context = application,
        campaignDao = database.campaignDao(),
        recipientDao = database.recipientDao(),
        scope = viewModelScope
    )

    val allCampaigns: StateFlow<List<CampaignEntity>> = repository.allCampaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTemplates: StateFlow<List<SmsTemplateEntity>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dispatchProgress: StateFlow<DispatchProgress> = dispatcherEngine.progress

    private val _liveEvents = MutableStateFlow<List<LiveDispatchEvent>>(emptyList())
    val liveEvents: StateFlow<List<LiveDispatchEvent>> = _liveEvents.asStateFlow()

    val intentFeedbackSummary: StateFlow<DeliveryBroadcastSummary> = DeliveryFeedbackBus.summary
    val intentFeedbackStream: SharedFlow<IntentBroadcastFeedback> = DeliveryFeedbackBus.feedbackStream

    private val _formState = MutableStateFlow(NewCampaignFormState())
    val formState: StateFlow<NewCampaignFormState> = _formState.asStateFlow()

    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    private val _availableSimCards = MutableStateFlow<List<SimCardInfo>>(emptyList())
    val availableSimCards: StateFlow<List<SimCardInfo>> = _availableSimCards.asStateFlow()

    // Campaign Details Selection
    private val _selectedCampaignId = MutableStateFlow<Long?>(null)
    val selectedCampaignId: StateFlow<Long?> = _selectedCampaignId.asStateFlow()

    val selectedCampaign: StateFlow<CampaignEntity?> = _selectedCampaignId
        .filterNotNull()
        .flatMapLatest { id -> repository.getCampaignById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _recipientSearchQuery = MutableStateFlow("")
    val recipientSearchQuery: StateFlow<String> = _recipientSearchQuery.asStateFlow()

    private val _recipientStatusFilter = MutableStateFlow<DeliveryStatus?>(null)
    val recipientStatusFilter: StateFlow<DeliveryStatus?> = _recipientStatusFilter.asStateFlow()

    val selectedCampaignRecipients: StateFlow<List<RecipientEntity>> = _selectedCampaignId
        .filterNotNull()
        .flatMapLatest { id -> repository.getRecipientsForCampaign(id) }
        .combine(_recipientSearchQuery) { list, query ->
            if (query.isBlank()) list else list.filter {
                it.phoneNumber.contains(query, ignoreCase = true) ||
                        it.recipientName.contains(query, ignoreCase = true) ||
                        it.amount.contains(query, ignoreCase = true)
            }
        }
        .combine(_recipientStatusFilter) { list, status ->
            if (status == null) list else list.filter { it.status == status }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Templates search & filter
    private val _templateSearchQuery = MutableStateFlow("")
    val templateSearchQuery: StateFlow<String> = _templateSearchQuery.asStateFlow()

    private val _templateCategoryFilter = MutableStateFlow<String?>(null)
    val templateCategoryFilter: StateFlow<String?> = _templateCategoryFilter.asStateFlow()

    val filteredTemplates: StateFlow<List<SmsTemplateEntity>> = combine(
        allTemplates,
        _templateSearchQuery,
        _templateCategoryFilter
    ) { list, query, category ->
        list.filter { template ->
            val matchesQuery = query.isBlank() ||
                    template.title.contains(query, ignoreCase = true) ||
                    template.body.contains(query, ignoreCase = true) ||
                    template.category.contains(query, ignoreCase = true)
            val matchesCategory = category == null || template.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // SMS Inbox & Conversations
    private val _inboxMessages = MutableStateFlow<List<com.example.data.model.SmsMessageItem>>(emptyList())
    val inboxMessages: StateFlow<List<com.example.data.model.SmsMessageItem>> = _inboxMessages.asStateFlow()

    private val _inboxSearchQuery = MutableStateFlow("")
    val inboxSearchQuery: StateFlow<String> = _inboxSearchQuery.asStateFlow()

    private val _selectedConversationAddress = MutableStateFlow<String?>(null)
    val selectedConversationAddress: StateFlow<String?> = _selectedConversationAddress.asStateFlow()

    private val _isRefreshingInbox = MutableStateFlow(false)
    val isRefreshingInbox: StateFlow<Boolean> = _isRefreshingInbox.asStateFlow()

    val conversations: StateFlow<List<com.example.data.model.SmsConversation>> = combine(
        _inboxMessages,
        _inboxSearchQuery
    ) { msgs, query ->
        val convs = com.example.engine.SmsInboxManager.groupIntoConversations(msgs)
        if (query.isBlank()) {
            convs
        } else {
            convs.filter {
                it.address.contains(query, ignoreCase = true) ||
                        it.senderName.contains(query, ignoreCase = true) ||
                        it.lastMessage.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeConversationMessages: StateFlow<List<com.example.data.model.SmsMessageItem>> = combine(
        _inboxMessages,
        _selectedConversationAddress
    ) { msgs, address ->
        if (address == null) emptyList()
        else {
            val normalizedTarget = address.filter { it.isDigit() || it == '+' }
            msgs.filter {
                it.address.filter { c -> c.isDigit() || c == '+' } == normalizedTarget
            }.sortedBy { it.dateEpoch }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadInboxCount: StateFlow<Int> = _inboxMessages
        .combine(_inboxMessages) { msgs, _ -> msgs.count { !it.isRead && it.isIncoming } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Collect live events from dispatcher
        viewModelScope.launch {
            dispatcherEngine.eventStream.collect { event ->
                _liveEvents.value = (listOf(event) + _liveEvents.value).take(50)
            }
        }

        // Collect intent broadcast feedback events
        viewModelScope.launch {
            DeliveryFeedbackBus.feedbackStream.collect { feedback ->
                val event = LiveDispatchEvent(
                    timestamp = feedback.timestamp,
                    phoneNumber = feedback.phoneNumber,
                    recipientName = "ACK #${feedback.resultCode}",
                    status = feedback.status,
                    message = "${feedback.statusDescription} [Intent Broadcast]"
                )
                _liveEvents.value = (listOf(event) + _liveEvents.value).take(50)
            }
        }

        refreshSimCards()
        refreshDefaultSmsAppStatus()
        loadInboxMessages()
    }

    private val _pendingImportSheet = MutableStateFlow<RawImportSheet?>(null)
    val pendingImportSheet: StateFlow<RawImportSheet?> = _pendingImportSheet.asStateFlow()

    fun dismissImportSheet() {
        _pendingImportSheet.value = null
    }

    fun openImportMappingDialog(sheet: RawImportSheet) {
        _pendingImportSheet.value = sheet
    }

    fun loadFileForImport(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                var fileName = "imported_contacts.csv"
                app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: "imported_contacts"
                    }
                }
                val inputStream = app.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val rawSheet = ContactImportService.parseStream(inputStream, fileName)
                    withContext(Dispatchers.Main) {
                        if (rawSheet.rows.isEmpty()) {
                            _formState.value = _formState.value.copy(errorMessage = "The selected file is empty or contains no records")
                        } else {
                            _pendingImportSheet.value = rawSheet
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _formState.value = _formState.value.copy(errorMessage = "Failed to load file: ${e.localizedMessage}")
                }
            }
        }
    }

    fun loadTextForImport(rawText: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val rawSheet = ContactImportService.parseRawText(rawText)
            withContext(Dispatchers.Main) {
                if (rawSheet.rows.isEmpty()) {
                    _formState.value = _formState.value.copy(errorMessage = "No contact rows found in pasted text")
                } else {
                    _pendingImportSheet.value = rawSheet
                }
            }
        }
    }

    fun applyColumnMapping(config: ColumnMappingConfig) {
        val raw = _pendingImportSheet.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val result = ContactImportService.mapToRecipients(raw, config)
            withContext(Dispatchers.Main) {
                _pendingImportSheet.value = null
                _formState.value = _formState.value.copy(
                    contacts = result.validContacts.take(1000),
                    parseSummary = result,
                    errorMessage = if (result.validContacts.isEmpty()) "No valid numbers found with current column mapping" else null
                )
            }
        }
    }

    fun refreshDefaultSmsAppStatus() {
        val app = getApplication<Application>()
        val defaultPackage = Telephony.Sms.getDefaultSmsPackage(app)
        _isDefaultSmsApp.value = (defaultPackage == app.packageName)
    }

    fun refreshSimCards() {
        val app = getApplication<Application>()
        val sims = SimCardManager.getAvailableSimCards(app)
        _availableSimCards.value = sims
        if (sims.isNotEmpty()) {
            val sim1 = sims.firstOrNull { it.slotIndex == 0 } ?: sims[0]
            val sim2 = sims.firstOrNull { it.slotIndex == 1 } ?: if (sims.size > 1) sims[1] else sim1
            val defaultSim = sims.firstOrNull { it.isDefault } ?: sim1
            _formState.value = _formState.value.copy(
                selectedSubscriptionId = defaultSim.subscriptionId,
                selectedSimLabel = "${defaultSim.displayName} (${defaultSim.carrierName})",
                sim1SubscriptionId = sim1.subscriptionId,
                sim1Label = "${sim1.displayName} (${sim1.carrierName})",
                sim2SubscriptionId = sim2.subscriptionId,
                sim2Label = "${sim2.displayName} (${sim2.carrierName})"
            )
        }
    }

    fun getCarrierBreakdown(contacts: List<ParsedContact>): Map<PhilippineCarrier, Int> {
        val map = mutableMapOf<PhilippineCarrier, Int>()
        PhilippineCarrier.values().forEach { map[it] = 0 }
        for (c in contacts) {
            val carrier = PhilippineCarrierClassifier.detectCarrier(c.phoneNumber)
            map[carrier] = (map[carrier] ?: 0) + 1
        }
        return map
    }

    fun selectCampaign(id: Long?) {
        _selectedCampaignId.value = id
        _recipientSearchQuery.value = ""
        _recipientStatusFilter.value = null
    }

    fun setRecipientSearch(query: String) {
        _recipientSearchQuery.value = query
    }

    fun setRecipientStatusFilter(status: DeliveryStatus?) {
        _recipientStatusFilter.value = status
    }

    fun updateForm(transform: (NewCampaignFormState) -> NewCampaignFormState) {
        _formState.value = transform(_formState.value)
    }

    fun importCsvFromUri(uri: Uri) {
        loadFileForImport(uri)
    }

    fun importRawNumbersText(text: String) {
        loadTextForImport(text)
    }

    fun setTemplateSearchQuery(query: String) {
        _templateSearchQuery.value = query
    }

    fun setTemplateCategoryFilter(category: String?) {
        _templateCategoryFilter.value = category
    }

    fun applyTemplate(template: SmsTemplateEntity) {
        _formState.value = _formState.value.copy(
            title = _formState.value.title.ifBlank { template.title },
            messageBody = template.body
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementTemplateUsage(template.id)
        }
    }

    fun generateDefaultCampaignTitle(scheduledTimeEpoch: Long? = null): String {
        val targetEpoch = scheduledTimeEpoch ?: System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(targetEpoch))
        return "Campaign $formattedDate"
    }

    fun launchCampaign(onSuccess: (Long) -> Unit) {
        val form = _formState.value
        val effectiveTitle = form.title.trim().ifBlank {
            generateDefaultCampaignTitle(form.scheduledTimeEpoch)
        }
        if (form.messageBody.isBlank()) {
            _formState.value = form.copy(errorMessage = "SMS message body cannot be empty")
            return
        }
        if (form.contacts.isEmpty()) {
            _formState.value = form.copy(errorMessage = "Please import or select at least 1 contact (up to 1,000)")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _formState.value = _formState.value.copy(isSubmitting = true, errorMessage = null)

            val effectiveSimLabel = if (form.isDualSimEnabled) {
                when (form.dualSimMode) {
                    SimRoutingMode.SMART_PREFIX -> "Smart Dual SIM (SIM 1: Globe/TM • SIM 2: Smart/TNT)"
                    SimRoutingMode.ROUND_ROBIN -> "Dual SIM (Round-Robin)"
                    SimRoutingMode.SINGLE_SIM -> form.selectedSimLabel
                }
            } else {
                form.selectedSimLabel
            }

            val campaign = CampaignEntity(
                title = effectiveTitle,
                senderId = "",
                messageBody = form.messageBody.trim(),
                scheduledTimeEpoch = form.scheduledTimeEpoch,
                simSubscriptionId = form.selectedSubscriptionId,
                simLabel = effectiveSimLabel,
                isDualSimEnabled = form.isDualSimEnabled,
                dualSimMode = form.dualSimMode.name,
                sim1SubscriptionId = form.sim1SubscriptionId,
                sim1Label = form.sim1Label,
                sim2SubscriptionId = form.sim2SubscriptionId,
                sim2Label = form.sim2Label,
                globeSimSlot = form.globeSimSlot,
                smartSimSlot = form.smartSimSlot
            )

            val campaignId = repository.createCampaignWithRecipients(campaign, form.contacts)
            dispatcherEngine.throttleDelayMs = form.throttleSpeed
            dispatcherEngine.isRateLimitBypassEnabled = form.bypassAndroidRateLimit

            val routingConfig = if (form.isDualSimEnabled) {
                DualSimRoutingConfig(
                    mode = form.dualSimMode,
                    sim1SlotIndex = 0,
                    sim1SubscriptionId = form.sim1SubscriptionId,
                    sim1Label = form.sim1Label,
                    sim2SlotIndex = 1,
                    sim2SubscriptionId = form.sim2SubscriptionId,
                    sim2Label = form.sim2Label,
                    globeSimSlot = form.globeSimSlot,
                    smartSimSlot = form.smartSimSlot,
                    singleSelectedSubId = form.selectedSubscriptionId,
                    singleSelectedSimLabel = form.selectedSimLabel
                )
            } else {
                DualSimRoutingConfig(
                    mode = SimRoutingMode.SINGLE_SIM,
                    singleSelectedSubId = form.selectedSubscriptionId,
                    singleSelectedSimLabel = form.selectedSimLabel
                )
            }

            // If immediate, dispatch directly through Local SIM Card
            if (form.scheduledTimeEpoch == null || form.scheduledTimeEpoch <= System.currentTimeMillis()) {
                dispatcherEngine.startDispatch(
                    campaignId = campaignId,
                    subscriptionId = form.selectedSubscriptionId,
                    simLabel = effectiveSimLabel,
                    routingConfig = routingConfig
                )
            }

            withContext(Dispatchers.Main) {
                _formState.value = NewCampaignFormState(
                    title = "",
                    senderId = "",
                    messageBody = "",
                    contacts = emptyList(),
                    parseSummary = null,
                    selectedSubscriptionId = form.selectedSubscriptionId,
                    selectedSimLabel = form.selectedSimLabel,
                    isDualSimEnabled = form.isDualSimEnabled,
                    dualSimMode = form.dualSimMode,
                    sim1SubscriptionId = form.sim1SubscriptionId,
                    sim1Label = form.sim1Label,
                    sim2SubscriptionId = form.sim2SubscriptionId,
                    sim2Label = form.sim2Label,
                    globeSimSlot = form.globeSimSlot,
                    smartSimSlot = form.smartSimSlot
                )
                selectCampaign(campaignId)
                onSuccess(campaignId)
            }
        }
    }

    fun pauseActiveDispatch() {
        dispatcherEngine.pauseDispatch()
    }

    fun resumeActiveDispatch() {
        dispatcherEngine.resumeDispatch()
    }

    fun cancelActiveDispatch() {
        dispatcherEngine.cancelDispatch()
    }

    fun startScheduledCampaignNow(campaignId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val campaign = repository.getCampaignByIdDirect(campaignId)
            val form = _formState.value
            val routingConfig = if (campaign?.isDualSimEnabled == true) {
                DualSimRoutingConfig(
                    mode = try { SimRoutingMode.valueOf(campaign.dualSimMode) } catch (e: Exception) { SimRoutingMode.SMART_PREFIX },
                    sim1SlotIndex = 0,
                    sim1SubscriptionId = campaign.sim1SubscriptionId,
                    sim1Label = campaign.sim1Label,
                    sim2SlotIndex = 1,
                    sim2SubscriptionId = campaign.sim2SubscriptionId,
                    sim2Label = campaign.sim2Label,
                    globeSimSlot = campaign.globeSimSlot,
                    smartSimSlot = campaign.smartSimSlot
                )
            } else null

            dispatcherEngine.startDispatch(
                campaignId = campaignId,
                subscriptionId = campaign?.simSubscriptionId ?: form.selectedSubscriptionId,
                simLabel = campaign?.simLabel ?: form.selectedSimLabel,
                routingConfig = routingConfig
            )
        }
    }

    fun deleteCampaign(campaignId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCampaign(campaignId)
            if (_selectedCampaignId.value == campaignId) {
                _selectedCampaignId.value = null
            }
        }
    }

    fun deleteMultipleCampaigns(campaignIds: Set<Long>) {
        if (campaignIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCampaigns(campaignIds.toList())
            if (campaignIds.contains(_selectedCampaignId.value)) {
                _selectedCampaignId.value = null
            }
        }
    }

    fun createTemplate(
        title: String,
        category: String,
        body: String,
        isFavorite: Boolean = false
    ) {
        val extracted = TemplateEngine.extractPlaceholders(body).joinToString(", ")
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTemplate(
                SmsTemplateEntity(
                    title = title.trim(),
                    category = category.trim().ifEmpty { "General" },
                    body = body.trim(),
                    sampleVariables = extracted.ifEmpty { "{Name}, {Amount}" },
                    isFavorite = isFavorite,
                    usageCount = 0
                )
            )
        }
    }

    fun updateTemplate(template: SmsTemplateEntity) {
        val extracted = TemplateEngine.extractPlaceholders(template.body).joinToString(", ")
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTemplate(template.copy(sampleVariables = extracted.ifEmpty { "{Name}, {Amount}" }))
        }
    }

    fun toggleFavoriteTemplate(template: SmsTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavoriteTemplate(template.id, !template.isFavorite)
        }
    }

    fun duplicateTemplate(template: SmsTemplateEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTemplate(
                template.copy(
                    id = 0,
                    title = "${template.title} (Copy)",
                    usageCount = 0,
                    createdAtEpoch = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveCurrentFormAsTemplate(title: String, category: String) {
        val form = _formState.value
        createTemplate(
            title = title.ifBlank { form.title.ifBlank { "Saved Template" } },
            category = category.ifBlank { "Custom" },
            body = form.messageBody,
            isFavorite = false
        )
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTemplate(id)
        }
    }

    // Inbox Actions
    fun loadInboxMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingInbox.value = true
            val app = getApplication<Application>()
            val messages = com.example.engine.SmsInboxManager.readInboxMessages(app)
            withContext(Dispatchers.Main) {
                _inboxMessages.value = messages
                _isRefreshingInbox.value = false
            }
        }
    }

    fun setInboxSearchQuery(query: String) {
        _inboxSearchQuery.value = query
    }

    fun selectConversation(address: String?) {
        _selectedConversationAddress.value = address
        if (address != null) {
            markConversationAsRead(address)
        }
    }

    fun markConversationAsRead(address: String) {
        val normalizedTarget = address.filter { it.isDigit() || it == '+' }
        _inboxMessages.value = _inboxMessages.value.map {
            if (it.address.filter { c -> c.isDigit() || c == '+' } == normalizedTarget) {
                it.copy(isRead = true)
            } else it
        }
    }

    fun sendReply(
        recipientAddress: String,
        text: String,
        subscriptionId: Int = -1,
        onSuccess: () -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val success = com.example.engine.SmsInboxManager.sendReply(
                context = app,
                recipientAddress = recipientAddress,
                messageText = text.trim(),
                subscriptionId = subscriptionId
            )
            if (success) {
                val updated = com.example.engine.SmsInboxManager.readInboxMessages(app)
                withContext(Dispatchers.Main) {
                    _inboxMessages.value = updated
                    onSuccess()
                }
            }
        }
    }

    fun deleteMultipleConversations(addresses: Set<String>) {
        if (addresses.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            com.example.engine.SmsInboxManager.deleteConversations(addresses.toList(), app)
            val updated = com.example.engine.SmsInboxManager.readInboxMessages(app)
            withContext(Dispatchers.Main) {
                _inboxMessages.value = updated
                if (addresses.contains(_selectedConversationAddress.value)) {
                    _selectedConversationAddress.value = null
                }
            }
        }
    }

    fun deleteMultipleMessages(messageIds: Set<Long>) {
        if (messageIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            com.example.engine.SmsInboxManager.deleteMessages(messageIds.toList(), app)
            val updated = com.example.engine.SmsInboxManager.readInboxMessages(app)
            withContext(Dispatchers.Main) {
                _inboxMessages.value = updated
            }
        }
    }
}
