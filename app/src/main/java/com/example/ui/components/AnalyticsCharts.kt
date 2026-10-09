package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CampaignStatus
import com.example.data.model.DeliveryStatus
import kotlin.math.atan2

enum class DeliveryPieSegment {
    SENT,
    PENDING,
    FAILED
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    trailingBadge: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (trailingBadge != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = trailingBadge,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun StatusBadge(status: DeliveryStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text) = when (status) {
        DeliveryStatus.DELIVERED -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "Delivered")
        DeliveryStatus.SENT -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), "Sent via SIM")
        DeliveryStatus.SENDING -> Triple(Color(0xFFE0E7FF), Color(0xFF3730A3), "Transmitting")
        DeliveryStatus.QUEUED -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "Queued")
        DeliveryStatus.PENDING -> Triple(Color(0xFFF3F4F6), Color(0xFF4B5563), "Pending")
        DeliveryStatus.FAILED -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "Failed")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CampaignStatusBadge(status: CampaignStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        CampaignStatus.COMPLETED -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "COMPLETED")
        CampaignStatus.SENDING -> Triple(Color(0xFFE0E7FF), Color(0xFF3730A3), "DISPATCHING")
        CampaignStatus.SCHEDULED -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "SCHEDULED")
        CampaignStatus.PAUSED -> Triple(Color(0xFFFFFBEB), Color(0xFFB45309), "PAUSED")
        CampaignStatus.CANCELLED -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "CANCELLED")
        CampaignStatus.DRAFT -> Triple(Color(0xFFF3F4F6), Color(0xFF4B5563), "DRAFT")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Real-time delivery status dashboard pie chart to visualize sent, pending, and failed SMS messages
 * based on intent broadcast feedback.
 */
@Composable
fun DeliveryStatusPieChart(
    sent: Int,
    pending: Int,
    failed: Int,
    modifier: Modifier = Modifier,
    title: String? = "Live Delivery Telemetry",
    subtitle: String? = "Visualized from cellular Intent broadcast receipts",
    showLegend: Boolean = true
) {
    val total = (sent + pending + failed).coerceAtLeast(0)
    var selectedSegment by remember { mutableStateOf<DeliveryPieSegment?>(null) }

    val sentColor = Color(0xFF2563EB)      // Vibrant Primary Blue
    val pendingColor = Color(0xFFF59E0B)   // Amber Gold
    val failedColor = Color(0xFFEF4444)    // Crimson Red
    val emptyRingColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    // Calculate proportions safely
    val sentFraction = if (total > 0) sent.toFloat() / total.toFloat() else 0f
    val pendingFraction = if (total > 0) pending.toFloat() / total.toFloat() else 0f
    val failedFraction = if (total > 0) failed.toFloat() / total.toFloat() else 0f

    // Animated sweeps for smooth transitions when broadcasts arrive in real time
    val animSentSweep by animateFloatAsState(
        targetValue = sentFraction * 360f,
        animationSpec = tween(durationMillis = 750),
        label = "animSentSweep"
    )
    val animPendingSweep by animateFloatAsState(
        targetValue = pendingFraction * 360f,
        animationSpec = tween(durationMillis = 750),
        label = "animPendingSweep"
    )
    val animFailedSweep by animateFloatAsState(
        targetValue = failedFraction * 360f,
        animationSpec = tween(durationMillis = 750),
        label = "animFailedSweep"
    )

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
            // Header
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Intent Broadcast Live Indicator Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "BROADCAST SYNC",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF047857),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Pie Chart Donut and Center Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .testTag("delivery_status_pie_chart"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(180.dp)
                            .pointerInput(total) {
                                detectTapGestures { offset ->
                                    if (total <= 0) return@detectTapGestures
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = offset.x - centerX
                                    val dy = offset.y - centerY
                                    // Calculate angle in degrees [0, 360), starting at -90deg (12 o'clock)
                                    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                    if (angle < 0f) angle += 360f

                                    val sentAngle = animSentSweep
                                    val pendingAngle = sentAngle + animPendingSweep

                                    selectedSegment = when {
                                        angle < sentAngle -> {
                                            if (selectedSegment == DeliveryPieSegment.SENT) null else DeliveryPieSegment.SENT
                                        }
                                        angle < pendingAngle -> {
                                            if (selectedSegment == DeliveryPieSegment.PENDING) null else DeliveryPieSegment.PENDING
                                        }
                                        else -> {
                                            if (selectedSegment == DeliveryPieSegment.FAILED) null else DeliveryPieSegment.FAILED
                                        }
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 24.dp.toPx()
                        val selectedStrokeWidth = 30.dp.toPx()
                        val arcSize = Size(size.width - selectedStrokeWidth, size.height - selectedStrokeWidth)
                        val arcOffset = Offset(selectedStrokeWidth / 2f, selectedStrokeWidth / 2f)

                        if (total == 0) {
                            // Empty state track
                            drawArc(
                                color = emptyRingColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = arcOffset,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                        } else {
                            var currentStartAngle = -90f // Start at 12 o'clock

                            // 1. Sent Slice
                            if (animSentSweep > 0f) {
                                val isSelected = selectedSegment == DeliveryPieSegment.SENT
                                val width = if (isSelected) selectedStrokeWidth else strokeWidth
                                drawArc(
                                    color = sentColor,
                                    startAngle = currentStartAngle,
                                    sweepAngle = (animSentSweep - 1.5f).coerceAtLeast(0.1f),
                                    useCenter = false,
                                    topLeft = arcOffset,
                                    size = arcSize,
                                    style = Stroke(width = width, cap = StrokeCap.Round)
                                )
                                currentStartAngle += animSentSweep
                            }

                            // 2. Pending Slice
                            if (animPendingSweep > 0f) {
                                val isSelected = selectedSegment == DeliveryPieSegment.PENDING
                                val width = if (isSelected) selectedStrokeWidth else strokeWidth
                                drawArc(
                                    color = pendingColor,
                                    startAngle = currentStartAngle,
                                    sweepAngle = (animPendingSweep - 1.5f).coerceAtLeast(0.1f),
                                    useCenter = false,
                                    topLeft = arcOffset,
                                    size = arcSize,
                                    style = Stroke(width = width, cap = StrokeCap.Round)
                                )
                                currentStartAngle += animPendingSweep
                            }

                            // 3. Failed Slice
                            if (animFailedSweep > 0f) {
                                val isSelected = selectedSegment == DeliveryPieSegment.FAILED
                                val width = if (isSelected) selectedStrokeWidth else strokeWidth
                                drawArc(
                                    color = failedColor,
                                    startAngle = currentStartAngle,
                                    sweepAngle = (animFailedSweep - 1.5f).coerceAtLeast(0.1f),
                                    useCenter = false,
                                    topLeft = arcOffset,
                                    size = arcSize,
                                    style = Stroke(width = width, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    // Center Donut Readout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(110.dp)
                            .clickable {
                                // Toggle selection or reset to total
                                selectedSegment = null
                            }
                    ) {
                        when (selectedSegment) {
                            DeliveryPieSegment.SENT -> {
                                Text(
                                    text = "SENT VIA SIM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = sentColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$sent",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(sentFraction * 100).toInt()}% of total",
                                    fontSize = 10.sp,
                                    color = sentColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DeliveryPieSegment.PENDING -> {
                                Text(
                                    text = "PENDING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pendingColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$pending",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(pendingFraction * 100).toInt()}% of total",
                                    fontSize = 10.sp,
                                    color = pendingColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DeliveryPieSegment.FAILED -> {
                                Text(
                                    text = "FAILED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = failedColor,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$failed",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(failedFraction * 100).toInt()}% of total",
                                    fontSize = 10.sp,
                                    color = failedColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            null -> {
                                Text(
                                    text = "TOTAL SMS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "$total",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (total > 0) "Recipients" else "No Data",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Interactive Segment Legend Chips
            if (showLegend) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PieLegendRow(
                        label = "Sent via Local SIM",
                        count = sent,
                        percentage = (sentFraction * 100).toInt(),
                        color = sentColor,
                        icon = Icons.Default.CheckCircleOutline,
                        isSelected = selectedSegment == DeliveryPieSegment.SENT,
                        onClick = {
                            selectedSegment = if (selectedSegment == DeliveryPieSegment.SENT) null else DeliveryPieSegment.SENT
                        },
                        tag = "pie_chart_sent_chip",
                        feedbackNote = "Intent ACK: RESULT_OK"
                    )

                    PieLegendRow(
                        label = "Pending in Queue",
                        count = pending,
                        percentage = (pendingFraction * 100).toInt(),
                        color = pendingColor,
                        icon = Icons.Default.Schedule,
                        isSelected = selectedSegment == DeliveryPieSegment.PENDING,
                        onClick = {
                            selectedSegment = if (selectedSegment == DeliveryPieSegment.PENDING) null else DeliveryPieSegment.PENDING
                        },
                        tag = "pie_chart_pending_chip",
                        feedbackNote = "Awaiting Radio Dispatch"
                    )

                    PieLegendRow(
                        label = "Carrier Failed / Bounced",
                        count = failed,
                        percentage = (failedFraction * 100).toInt(),
                        color = failedColor,
                        icon = Icons.Default.ErrorOutline,
                        isSelected = selectedSegment == DeliveryPieSegment.FAILED,
                        onClick = {
                            selectedSegment = if (selectedSegment == DeliveryPieSegment.FAILED) null else DeliveryPieSegment.FAILED
                        },
                        tag = "pie_chart_failed_chip",
                        feedbackNote = "Broadcast Error / Reject"
                    )
                }
            }
        }
    }
}

@Composable
private fun PieLegendRow(
    label: String,
    count: Int,
    percentage: Int,
    color: Color,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    tag: String,
    feedbackNote: String
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = color,
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = feedbackNote,
                        fontSize = 10.sp,
                        color = if (isSelected) color else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$count",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$percentage%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryBreakdownChart(
    total: Int,
    sent: Int,
    delivered: Int,
    failed: Int,
    modifier: Modifier = Modifier
) {
    val maxBase = if (total > 0) total.toFloat() else 1f

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BreakdownRow(
            label = "Total Target Recipients",
            count = total,
            ratio = 1f,
            color = Color(0xFF3B82F6)
        )
        BreakdownRow(
            label = "Sent via Local SIM Radio",
            count = sent,
            ratio = sent / maxBase,
            color = Color(0xFF6366F1)
        )
        BreakdownRow(
            label = "Carrier Confirmed Delivered",
            count = delivered,
            ratio = delivered / maxBase,
            color = Color(0xFF10B981)
        )
        if (failed > 0) {
            BreakdownRow(
                label = "Carrier Failed / Bounced",
                count = failed,
                ratio = failed / maxBase,
                color = Color(0xFFEF4444)
            )
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    count: Int,
    ratio: Float,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "$count (${(ratio * 100).toInt()}%)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
    }
}

// Backward compatibility alias
@Composable
fun DeliveryFunnelChart(
    total: Int,
    sent: Int,
    delivered: Int,
    opened: Int = 0,
    clicks: Int = 0,
    modifier: Modifier = Modifier
) {
    DeliveryBreakdownChart(total, sent, delivered, total - delivered, modifier)
}
