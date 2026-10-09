package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SmsConversation
import com.example.data.model.SmsMessageItem
import com.example.ui.viewmodel.CampaignViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InboxScreen(
    viewModel: CampaignViewModel
) {
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsState()
    val searchQuery by viewModel.inboxSearchQuery.collectAsState()
    val selectedAddress by viewModel.selectedConversationAddress.collectAsState()
    val conversationMessages by viewModel.activeConversationMessages.collectAsState()
    val isRefreshing by viewModel.isRefreshingInbox.collectAsState()
    val availableSims by viewModel.availableSimCards.collectAsState()

    if (selectedAddress != null) {
        // Detailed Conversation View with Instant Reply & Multi-Message Selection
        ConversationDetailView(
            address = selectedAddress!!,
            messages = conversationMessages,
            availableSims = availableSims,
            onBack = { viewModel.selectConversation(null) },
            onSendReply = { text, subId ->
                viewModel.sendReply(
                    recipientAddress = selectedAddress!!,
                    text = text,
                    subscriptionId = subId,
                    onSuccess = {
                        Toast.makeText(context, "Reply sent via Local SIM!", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onDeleteConversation = {
                viewModel.deleteMultipleConversations(setOf(selectedAddress!!))
                Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
            },
            onDeleteSelectedMessages = { ids ->
                viewModel.deleteMultipleMessages(ids)
                Toast.makeText(context, "${ids.size} message(s) deleted", Toast.LENGTH_SHORT).show()
            }
        )
    } else {
        // Conversations List View with Multi-Select Manual Deletion
        ConversationListView(
            conversations = conversations,
            searchQuery = searchQuery,
            isRefreshing = isRefreshing,
            onSearchQueryChange = { viewModel.setInboxSearchQuery(it) },
            onRefresh = { viewModel.loadInboxMessages() },
            onSelectConversation = { conv ->
                viewModel.selectConversation(conv.address)
            },
            onDeleteMultiple = { addresses ->
                viewModel.deleteMultipleConversations(addresses)
                Toast.makeText(context, "${addresses.size} conversation(s) deleted", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationListView(
    conversations: List<SmsConversation>,
    searchQuery: String,
    isRefreshing: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectConversation: (SmsConversation) -> Unit,
    onDeleteMultiple: (Set<String>) -> Unit
) {
    val unreadCount = conversations.sumOf { it.unreadCount }

    // Multi-selection state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedAddresses by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun toggleSelection(addr: String) {
        selectedAddresses = if (selectedAddresses.contains(addr)) {
            selectedAddresses - addr
        } else {
            selectedAddresses + addr
        }
    }

    fun selectAll() {
        selectedAddresses = conversations.map { it.address }.toSet()
    }

    fun deselectAll() {
        selectedAddresses = emptySet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row: Regular or Multi-Select Action Bar
        item {
            if (isMultiSelectMode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isMultiSelectMode = false
                                    selectedAddresses = emptySet()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedAddresses.size} Selected",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (selectedAddresses.size == conversations.size) {
                                        deselectAll()
                                    } else {
                                        selectAll()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    if (selectedAddresses.size == conversations.size) "Deselect All" else "Select All",
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                enabled = selectedAddresses.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("delete_selected_inbox_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete (${selectedAddresses.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SMS Inbox",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "$unreadCount new",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Incoming customer replies and conversations",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (conversations.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    isMultiSelectMode = true
                                    selectedAddresses = emptySet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("select_multiple_inbox_button")
                            ) {
                                Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        IconButton(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            modifier = Modifier.testTag("refresh_inbox_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Inbox")
                            }
                        }
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search sender name, number, or message...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_inbox_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Count summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${conversations.size} Conversations",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (isMultiSelectMode) "Tap items to select for deletion" else "Tap to open and reply via SIM",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Empty state
        if (conversations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No messages match '$searchQuery'" else "No messages in inbox. Tap Refresh to check.",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(conversations, key = { it.address }) { conv ->
                val isSelected = selectedAddresses.contains(conv.address)
                ConversationItemCard(
                    conversation = conv,
                    isMultiSelectMode = isMultiSelectMode,
                    isSelected = isSelected,
                    onToggleSelect = { toggleSelection(conv.address) },
                    onLongPress = {
                        if (!isMultiSelectMode) {
                            isMultiSelectMode = true
                            selectedAddresses = setOf(conv.address)
                        }
                    },
                    onClick = {
                        if (isMultiSelectMode) {
                            toggleSelection(conv.address)
                        } else {
                            onSelectConversation(conv)
                        }
                    }
                )
            }
        }
    }

    // Confirmation dialog for deleting multiple conversations
    if (showDeleteConfirmDialog && selectedAddresses.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete ${selectedAddresses.size} Conversation(s)?") },
            text = {
                Text(
                    "Are you sure you want to delete ${selectedAddresses.size} selected conversation(s)? All incoming customer SMS and sent replies for these contacts will be permanently deleted.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMultiple(selectedAddresses)
                        showDeleteConfirmDialog = false
                        isMultiSelectMode = false
                        selectedAddresses = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Conversations")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItemCard(
    conversation: SmsConversation,
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onClick: () -> Unit
) {
    val timeFormatted = remember(conversation.lastDateEpoch) {
        formatRelativeTime(conversation.lastDateEpoch)
    }

    val avatarColor = remember(conversation.address) {
        val colors = listOf(
            Color(0xFF2563EB), Color(0xFF059669), Color(0xFF7C3AED),
            Color(0xFFD97706), Color(0xFFDC2626), Color(0xFF0891B2)
        )
        colors[Math.abs(conversation.address.hashCode()) % colors.size]
    }

    val initials = remember(conversation.senderName, conversation.address) {
        if (conversation.senderName.contains(" ")) {
            val parts = conversation.senderName.split(" ")
            "${parts[0].firstOrNull() ?: '?'}${parts[1].firstOrNull() ?: ""}"
        } else {
            conversation.senderName.take(2).uppercase()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation_item_${conversation.address}")
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else if (conversation.unreadCount > 0) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (if multi-select)
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Avatar
            Surface(
                shape = CircleShape,
                color = avatarColor,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contact Info & Message Preview
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.senderName,
                        fontSize = 14.sp,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = timeFormatted,
                        fontSize = 11.sp,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.address,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${conversation.totalMessages} msg",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        fontSize = 12.sp,
                        color = if (conversation.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationDetailView(
    address: String,
    messages: List<SmsMessageItem>,
    availableSims: List<com.example.engine.SimCardInfo>,
    onBack: () -> Unit,
    onSendReply: (text: String, subId: Int) -> Unit,
    onDeleteConversation: () -> Unit,
    onDeleteSelectedMessages: (Set<Long>) -> Unit
) {
    BackHandler {
        onBack()
    }

    var replyText by remember { mutableStateOf("") }
    var selectedSimSubId by remember {
        mutableStateOf(availableSims.firstOrNull { it.isDefault }?.subscriptionId ?: -1)
    }

    // Message multi-select state
    var isMessageSelectMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteMessagesDialog by remember { mutableStateOf(false) }
    var showDeleteConversationDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val senderName = remember(messages) {
        messages.firstOrNull { it.senderName.isNotEmpty() && it.senderName != "You" }?.senderName
            ?: "Customer ($address)"
    }

    val quickReplies = listOf(
        "Thank you! Your payment is confirmed.",
        "We have received your message and are reviewing it.",
        "Your account statement is up to date.",
        "Please let us know if you need further details."
    )

    fun toggleMessageSelect(id: Long) {
        selectedMessageIds = if (selectedMessageIds.contains(id)) {
            selectedMessageIds - id
        } else {
            selectedMessageIds + id
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Conversation Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMessageSelectMode) {
                    IconButton(onClick = {
                        isMessageSelectMode = false
                        selectedMessageIds = emptySet()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Message Select")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${selectedMessageIds.size} Selected",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showDeleteMessagesDialog = true },
                        enabled = selectedMessageIds.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("inbox_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = senderName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = address,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SimCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (availableSims.isNotEmpty()) availableSims.firstOrNull { it.subscriptionId == selectedSimSubId }?.displayName ?: "SIM 1" else "Local SIM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(onClick = { showDeleteConversationDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Conversation", tint = Color.Gray)
                    }
                }
            }
        }

        // Messages Bubble Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isSelected = selectedMessageIds.contains(msg.id)
                ChatBubble(
                    message = msg,
                    isSelectMode = isMessageSelectMode,
                    isSelected = isSelected,
                    onToggleSelect = { toggleMessageSelect(msg.id) },
                    onLongPress = {
                        if (!isMessageSelectMode) {
                            isMessageSelectMode = true
                            selectedMessageIds = setOf(msg.id)
                        }
                    }
                )
            }
        }

        // Quick Reply Suggestions Toolbar
        if (!isMessageSelectMode) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickReplies) { reply ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { replyText = reply },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = reply,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Reply Composer Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // SIM Slot Choice if multiple available
                    if (availableSims.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableSims.forEach { sim ->
                                val isSelected = selectedSimSubId == sim.subscriptionId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSimSubId = sim.subscriptionId },
                                    label = { Text("${sim.displayName} (${sim.carrierName})", fontSize = 10.sp) },
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Type SMS reply via Local SIM...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("inbox_reply_input"),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onSendReply(replyText, selectedSimSubId)
                                    replyText = ""
                                }
                            },
                            enabled = replyText.isNotBlank(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("send_reply_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    // Dialog for deleting selected messages
    if (showDeleteMessagesDialog && selectedMessageIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteMessagesDialog = false },
            title = { Text("Delete ${selectedMessageIds.size} Message(s)?") },
            text = { Text("Are you sure you want to delete the selected message(s)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSelectedMessages(selectedMessageIds)
                        showDeleteMessagesDialog = false
                        isMessageSelectMode = false
                        selectedMessageIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMessagesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for deleting the entire conversation
    if (showDeleteConversationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConversationDialog = false },
            title = { Text("Delete Entire Conversation?") },
            text = { Text("Are you sure you want to delete all messages with $senderName ($address)?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConversation()
                        showDeleteConversationDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Conversation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConversationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(
    message: SmsMessageItem,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val isIncoming = message.isIncoming
    val timeFormatted = remember(message.dateEpoch) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.dateEpoch))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectMode) onToggleSelect() },
                onLongClick = onLongPress
            ),
        horizontalArrangement = if (isIncoming) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectMode && isIncoming) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isIncoming) 4.dp else 16.dp,
                bottomEnd = if (isIncoming) 16.dp else 4.dp
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (isIncoming) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.fillMaxWidth(if (isSelectMode) 0.75f else 0.82f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.body,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else if (isIncoming) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        Color.White
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeFormatted,
                        fontSize = 10.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else if (isIncoming) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            Color.White.copy(alpha = 0.75f)
                        }
                    )
                    if (!isIncoming) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        if (isSelectMode && !isIncoming) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

private fun formatRelativeTime(epochMs: Long): String {
    val diffMs = System.currentTimeMillis() - epochMs
    val seconds = diffMs / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
