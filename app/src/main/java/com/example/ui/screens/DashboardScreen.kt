package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.engine.DeliveryBroadcastSummary
import com.example.ui.components.DeliveryStatusPieChart
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CampaignEntity
import com.example.engine.LiveDispatchEvent
import com.example.ui.components.CampaignStatusBadge
import com.example.ui.components.MetricStatCard
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.CampaignViewModel

@Composable
fun DashboardScreen(
    viewModel: CampaignViewModel,
    onNavigateToNewBlast: () -> Unit,
    onNavigateToCampaignDetail: (Long) -> Unit,
    onRequestDefaultSms: () -> Unit = {}
) {
    val progress by viewModel.dispatchProgress.collectAsState()
    val liveEvents by viewModel.liveEvents.collectAsState()
    val allCampaigns by viewModel.allCampaigns.collectAsState()
    val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsState()
    val availableSims by viewModel.availableSimCards.collectAsState()
    val intentSummary by viewModel.intentFeedbackSummary.collectAsState()

    var telemetryScope by remember { mutableStateOf(if (progress.isRunning) "active" else "aggregate") }

    // Aggregate summary stats
    val totalSent = allCampaigns.sumOf { it.sentCount }
    val totalDelivered = allCampaigns.sumOf { it.deliveredCount }
    val totalFailed = allCampaigns.sumOf { it.failedCount }
    val avgDeliveryRate = if (totalSent > 0) (totalDelivered.toFloat() / totalSent.toFloat()) * 100f else 0f

    val activeSent = progress.sentCount
    val activePending = (progress.totalCount - progress.processedCount).coerceAtLeast(0)
    val activeFailed = progress.failedCount

    val aggSent = totalSent
    val aggFailed = totalFailed
    val aggTotal = allCampaigns.sumOf { it.totalRecipients }
    val aggPending = (aggTotal - aggSent - aggFailed).coerceAtLeast(0)

    val hasActive = progress.isRunning || progress.totalCount > 0
    val pieSent = if (telemetryScope == "active" && hasActive) activeSent else aggSent
    val piePending = if (telemetryScope == "active" && hasActive) activePending else aggPending
    val pieFailed = if (telemetryScope == "active" && hasActive) activeFailed else aggFailed

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Local SIM Card & Primary SMS App Setup Banner (auto-removed once already set as primary)
        if (!isDefaultSmsApp) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFBEB)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFFDE68A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PRIMARY SMS SETUP REQUIRED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFB45309),
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val simSummary = if (availableSims.isNotEmpty()) {
                                availableSims.joinToString(", ") { "${it.displayName} (${it.carrierName})" }
                            } else "Device Carrier SIM"
                            Text(
                                text = "Local SIM: $simSummary",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Set as primary default SMS app to enable direct cellular SIM transmission and bypass rate limits.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Button(
                            onClick = onRequestDefaultSms,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Set Default", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Hero Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LOCAL SIM MESSAGING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bulk SMS Console",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Dispatches personalized messages with {Name} and {Amount} tags via local SIM card.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = onNavigateToNewBlast,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Blast", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Real-time Transmission Queue Monitor (if running or has active progress)
        if (progress.isRunning || progress.processedCount > 0) {
            item {
                com.example.ui.components.BulkOperationProgressBanner(
                    progress = progress,
                    onPause = { viewModel.pauseActiveDispatch() },
                    onResume = { viewModel.resumeActiveDispatch() },
                    onCancel = { viewModel.cancelActiveDispatch() }
                )
            }
        }

        // Real-Time Delivery Status Dashboard using Pie Chart based on intent broadcast feedback
        item {
            RealTimeDeliveryStatusDashboardCard(
                sent = pieSent,
                pending = piePending,
                failed = pieFailed,
                scope = telemetryScope,
                onScopeChange = { telemetryScope = it },
                hasActiveDispatch = hasActive,
                activeCampaignTitle = progress.campaignTitle,
                intentSummary = intentSummary
            )
        }

        // Live KPI Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SIM Delivery Telemetry",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Delivery Rate",
                        value = "${String.format("%.1f", avgDeliveryRate)}%",
                        subtitle = "$totalDelivered / $totalSent confirmed",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        trailingBadge = "SIM Radio"
                    )
                    MetricStatCard(
                        title = "Active SIM Status",
                        value = if (isDefaultSmsApp) "Ready" else "Standby",
                        subtitle = if (isDefaultSmsApp) "Primary Default SMS" else "Requires default setup",
                        icon = Icons.Default.SimCard,
                        accentColor = if (isDefaultSmsApp) Color(0xFF10B981) else Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        trailingBadge = if (isDefaultSmsApp) "ACTIVE" else "PENDING"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Total Dispatched",
                        value = "$totalSent",
                        subtitle = "Across all campaigns",
                        icon = Icons.Default.Send,
                        accentColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Carrier Bounces",
                        value = "$totalFailed",
                        subtitle = "DND / unreachable",
                        icon = Icons.Default.Error,
                        accentColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Live Event Log Stream (Audit Trail)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (progress.isRunning) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Live SIM Transmission Stream",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (progress.isRunning) "${String.format("%.0f", progress.currentRatePerSecond)} msg/sec" else "Idle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (liveEvents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ready. Launch a blast to see real-time cellular transmissions.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            liveEvents.take(6).forEach { event ->
                                LiveEventRow(event)
                            }
                        }
                    }
                }
            }
        }

        // Recent Campaigns Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Campaigns",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${allCampaigns.size} campaigns",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (allCampaigns.isEmpty()) {
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
                        Text("No campaigns yet. Tap 'New Blast' to start.", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            items(allCampaigns.take(4)) { campaign ->
                DashboardCampaignItem(
                    campaign = campaign,
                    onClick = { onNavigateToCampaignDetail(campaign.id) }
                )
            }
        }
    }
}

@Composable
fun ActiveTransmissionCard(
    progress: com.example.engine.DispatchProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val completionRatio = if (progress.totalCount > 0) {
        progress.processedCount.toFloat() / progress.totalCount.toFloat()
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (progress.isPaused) Color(0xFFF59E0B) else Color(0xFF10B981),
                        modifier = Modifier.size(10.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (progress.isRunning) {
                            if (progress.isPaused) "TRANSMISSION PAUSED" else "TRANSMITTING VIA LOCAL SIM (${progress.simCardLabel})"
                        } else "TRANSMISSION COMPLETED",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (progress.isRunning) {
                        if (progress.isPaused) {
                            IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF10B981))
                            }
                        } else {
                            IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color(0xFFF59E0B))
                            }
                        }
                        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = progress.campaignTitle.ifEmpty { "Local SIM SMS Blast" },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar and numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${progress.processedCount} / ${progress.totalCount} sent",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(completionRatio * 100).toInt()}%",
                    color = Color(0xFF38BDF8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { completionRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF38BDF8),
                trackColor = Color.White.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusCounterChip(
                    label = "Delivered",
                    count = progress.deliveredCount,
                    color = Color(0xFF10B981)
                )
                StatusCounterChip(
                    label = "Failed",
                    count = progress.failedCount,
                    color = Color(0xFFEF4444)
                )
                StatusCounterChip(
                    label = "Speed",
                    countText = "${String.format("%.0f", progress.currentRatePerSecond)}/s",
                    color = Color(0xFF38BDF8)
                )
                StatusCounterChip(
                    label = "ETA",
                    countText = if (progress.isRunning) "${progress.estimatedRemainingSeconds}s" else "0s",
                    color = Color(0xFFA78BFA)
                )
            }
        }
    }
}

@Composable
private fun StatusCounterChip(
    label: String,
    count: Int? = null,
    countText: String? = null,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = countText ?: count?.toString() ?: "0",
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun LiveEventRow(event: LiveDispatchEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.timestamp,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (event.recipientName.isNotEmpty()) "${event.recipientName} (${event.phoneNumber})" else event.phoneNumber,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        StatusBadge(status = event.status)
    }
}

@Composable
fun DashboardCampaignItem(
    campaign: CampaignEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = campaign.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                CampaignStatusBadge(status = campaign.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = campaign.messageBody,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🎯 ${campaign.totalRecipients} targets",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "📱 SIM: ${campaign.simLabel}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "🚀 ${campaign.deliveredCount} delivered (${campaign.deliveryRate.toInt()}%)",
                        fontSize = 11.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(Icons.Default.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun RealTimeDeliveryStatusDashboardCard(
    sent: Int,
    pending: Int,
    failed: Int,
    scope: String,
    onScopeChange: (String) -> Unit,
    hasActiveDispatch: Boolean,
    activeCampaignTitle: String,
    intentSummary: DeliveryBroadcastSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header with real-time status beacon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REAL-TIME BROADCAST TELEMETRY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Delivery Status Dashboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visualizing sent, pending, & failed messages via Intent broadcast receipts",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scope filter chips (All Campaigns vs Active Blast)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = scope == "aggregate",
                    onClick = { onScopeChange("aggregate") },
                    label = { Text("All Campaigns Aggregate", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                if (hasActiveDispatch) {
                    FilterChip(
                        selected = scope == "active",
                        onClick = { onScopeChange("active") },
                        label = {
                            Text(
                                text = "Active Blast (${activeCampaignTitle.ifEmpty { "Dispatch" }})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFF047857)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // The Pie Chart visualization
            DeliveryStatusPieChart(
                sent = sent,
                pending = pending,
                failed = failed,
                title = null, // Header displayed in card
                showLegend = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Live Intent Broadcast Receiver Feedback Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CellTower,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Intent Broadcast Listener",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${intentSummary.totalAcksReceived} ACKs Received",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Broadcast ACK breakdown pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BroadcastStatPill(
                            label = "Sent ACK",
                            count = intentSummary.sentAcks,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f)
                        )
                        BroadcastStatPill(
                            label = "Delivered",
                            count = intentSummary.deliveredAcks,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        BroadcastStatPill(
                            label = "Carrier Err",
                            count = intentSummary.failedAcks,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Dual SIM Prefix Routing ACK telemetry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BroadcastStatPill(
                            label = "SIM 1 (Globe/TM)",
                            count = intentSummary.sim1Acks,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.weight(1f)
                        )
                        BroadcastStatPill(
                            label = "SIM 2 (Smart/TNT)",
                            count = intentSummary.sim2Acks,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Latest Broadcast Event Ticker
                    val last = intentSummary.lastEvent
                    if (last != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = last.timestamp,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val simLabel = if (last.carrierTag.isNotEmpty()) " [SIM ${last.simSlot} • ${last.carrierTag}]" else ""
                                Text(
                                    text = "${last.phoneNumber}$simLabel: ${last.statusDescription}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(status = last.status)
                        }
                    } else {
                        Text(
                            text = "Awaiting intent broadcast callbacks from com.example.SMS_SENT_ACTION",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastStatPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
            Text(text = "$count", fontSize = 11.sp, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}
