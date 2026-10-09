package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeliveryStatus
import com.example.data.model.RecipientEntity
import com.example.engine.PhilippineCarrierClassifier
import com.example.ui.components.CampaignStatusBadge
import com.example.ui.components.DeliveryBreakdownChart
import com.example.ui.components.DeliveryStatusPieChart
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.CampaignViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CampaignDetailScreen(
    viewModel: CampaignViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val campaign by viewModel.selectedCampaign.collectAsState()
    val recipients by viewModel.selectedCampaignRecipients.collectAsState()
    val searchQuery by viewModel.recipientSearchQuery.collectAsState()
    val statusFilter by viewModel.recipientStatusFilter.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy @ h:mm:ss a", Locale.getDefault()) }

    if (campaign == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Campaign not found or deleted.", color = MaterialTheme.colorScheme.outline)
        }
        return
    }

    val currentCampaign = campaign!!

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Navigation Bar & Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentCampaign.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SIM: ${currentCampaign.simLabel} • ${currentCampaign.totalRecipients} Targets",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                CampaignStatusBadge(status = currentCampaign.status)
            }
        }

        // Real-Time Delivery Status Dashboard Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Local SIM Delivery Status",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-box status metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DeliveryMetricBox(
                            label = "Sent via SIM",
                            count = "${currentCampaign.sentCount}",
                            subtext = "Cellular Radio",
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                        DeliveryMetricBox(
                            label = "Delivered",
                            count = "${currentCampaign.deliveredCount}",
                            subtext = "${currentCampaign.deliveryRate.toInt()}% Rate",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        DeliveryMetricBox(
                            label = "Failed",
                            count = "${currentCampaign.failedCount}",
                            subtext = "Bounced / DND",
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        DeliveryMetricBox(
                            label = "Pending",
                            count = "${(currentCampaign.totalRecipients - currentCampaign.sentCount - currentCampaign.failedCount).coerceAtLeast(0)}",
                            subtext = "In Queue",
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DeliveryStatusPieChart(
                        sent = currentCampaign.sentCount,
                        pending = (currentCampaign.totalRecipients - currentCampaign.sentCount - currentCampaign.failedCount).coerceAtLeast(0),
                        failed = currentCampaign.failedCount,
                        title = "Delivery Status Pie Chart",
                        subtitle = "Sent, pending, & failed based on broadcast feedback"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Carrier Delivery Breakdown",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DeliveryBreakdownChart(
                        total = currentCampaign.totalRecipients,
                        sent = currentCampaign.sentCount,
                        delivered = currentCampaign.deliveredCount,
                        failed = currentCampaign.failedCount
                    )
                }
            }
        }

        // Message Content Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Message Template & Parameters",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Tags: {Name}, {Amount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentCampaign.messageBody,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Recipient Transmission Audit List Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recipient Transmission Audit",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${recipients.size} showing",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setRecipientSearch(it) },
                    placeholder = { Text("Search phone number, name, or amount...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_recipients_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Status Filter Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = statusFilter == null,
                            onClick = { viewModel.setRecipientStatusFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    items(DeliveryStatus.values()) { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { viewModel.setRecipientStatusFilter(if (statusFilter == status) null else status) },
                            label = { Text(status.name) }
                        )
                    }
                }
            }
        }

        // Recipients list
        if (recipients.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recipients match the filter.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            items(recipients, key = { it.id }) { recipient ->
                RecipientAuditRow(recipient = recipient, dateFormatter = dateFormatter)
            }
        }
    }
}

@Composable
fun DeliveryMetricBox(
    label: String,
    count: String,
    subtext: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = subtext, fontSize = 10.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun RecipientAuditRow(
    recipient: RecipientEntity,
    dateFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val carrier = PhilippineCarrierClassifier.detectCarrier(recipient.phoneNumber)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recipient.phoneNumber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = carrier.composeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = carrier.shortName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = carrier.composeColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        recipient.dispatchedSimSlot?.let { slot ->
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "SIM ${slot + 1}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (recipient.recipientName.isNotEmpty()) {
                            Text(
                                text = recipient.recipientName,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Amount: ${recipient.amount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                StatusBadge(status = recipient.status)
            }

            // Timestamps or failure reason
            recipient.failureReason?.let { reason ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reason: $reason",
                    fontSize = 10.sp,
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Medium
                )
            }

            recipient.sentAtEpoch?.let { sentTime ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dispatched: ${dateFormatter.format(Date(sentTime))}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    recipient.deliveredAtEpoch?.let { delTime ->
                        Text(
                            text = "ACK in ${delTime - sentTime}ms",
                            fontSize = 9.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
