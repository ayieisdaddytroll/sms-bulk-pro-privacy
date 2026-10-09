package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DispatchProgress

@Composable
fun BulkOperationProgressBanner(
    progress: DispatchProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completionRatio = if (progress.totalCount > 0) {
        progress.processedCount.toFloat() / progress.totalCount.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = completionRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "bulk_progress"
    )

    // Pulsing indicator dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A) // Sleek slate navy
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Status Indicator Badge & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when {
                        !progress.isRunning -> Color(0xFF10B981)
                        progress.isPaused -> Color(0xFFF59E0B)
                        else -> Color(0xFF10B981)
                    }

                    // Pulsing Status Dot
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                        if (progress.isRunning && !progress.isPaused) {
                            Surface(
                                shape = CircleShape,
                                color = statusColor.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(pulseScale)
                            ) {}
                        }
                        Surface(
                            shape = CircleShape,
                            color = statusColor,
                            modifier = Modifier.size(10.dp)
                        ) {}
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = when {
                            !progress.isRunning -> "TRANSMISSION COMPLETE"
                            progress.isPaused -> "DISPATCH PAUSED"
                            else -> "SENDING VIA LOCAL SIM"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                // Action Controls (Pause / Resume / Stop)
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

            // Campaign Title
            Text(
                text = progress.campaignTitle.ifEmpty { "Bulk SMS Broadcast" },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Cellular Radio Route: ${progress.simCardLabel}",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress numbers: Sent vs Total recipients
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${progress.processedCount}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " / ${progress.totalCount} recipients sent",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                    )
                }

                Text(
                    text = "${(completionRatio * 100).toInt()}%",
                    color = Color(0xFF38BDF8), // Cyan accent
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF38BDF8))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Detail telemetry chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(
                    icon = Icons.Default.CheckCircle,
                    label = "Delivered",
                    value = "${progress.deliveredCount}",
                    color = Color(0xFF10B981)
                )
                MetricPill(
                    icon = Icons.Default.Error,
                    label = "Bounced",
                    value = "${progress.failedCount}",
                    color = Color(0xFFEF4444)
                )
                MetricPill(
                    icon = Icons.Default.Speed,
                    label = "Speed",
                    value = "${String.format("%.0f", progress.currentRatePerSecond)}/s",
                    color = Color(0xFF38BDF8)
                )
                MetricPill(
                    icon = Icons.Default.SimCard,
                    label = "ETA",
                    value = if (progress.isRunning) "${progress.estimatedRemainingSeconds}s" else "0s",
                    color = Color(0xFFA78BFA)
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Compact sticky banner that docks at top of screens during active operations
 */
@Composable
fun CompactBulkProgressBar(
    progress: DispatchProgress,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completionRatio = if (progress.totalCount > 0) {
        progress.processedCount.toFloat() / progress.totalCount.toFloat()
    } else 0f

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0F172A),
        shadowElevation = 6.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (progress.isPaused) Color(0xFFF59E0B) else Color(0xFF10B981),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${progress.processedCount}/${progress.totalCount} sent",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${(completionRatio * 100).toInt()}%)",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = if (progress.isPaused) "Paused" else "${progress.simCardLabel} • ${String.format("%.0f", progress.currentRatePerSecond)} msg/s",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (progress.isPaused) {
                        IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        }
                    } else {
                        IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Stop, contentDescription = "Cancel", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Visual linear progress indicator along bottom of bar
            LinearProgressIndicator(
                progress = { completionRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color(0xFF38BDF8),
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }
    }
}
