package com.cascade26.scrollbreak.Activities

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import com.cascade26.scrollbreak.Utils.SessionTelemetryManager
import java.util.concurrent.TimeUnit

// App Theme Color Palette matching Theme.NoScroll
private val AppBg = Color(0xFF000000)
private val SurfaceCard = Color(0xFF101820)
private val SurfaceVariant = Color(0xFF101820)
private val BrandAccent = Color(0xFF10B981)
private val BrandAccentLight = Color(0xFF34D399)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextTertiary = Color(0x80FFFFFF)
private val GlassStroke = Color(0x0DFFFFFF)

@Composable
fun AnalyticsScreen(
    telemetryMetrics: SessionTelemetryManager.Metrics?,
    onBack: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Habits", "Focus", "Trends")

    val timeSavedMin = telemetryMetrics?.let { TimeUnit.MILLISECONDS.toMinutes(it.timeSavedMs) } ?: 0L
    val successRate = telemetryMetrics?.successPercent ?: 0
    val goalAdherence = telemetryMetrics?.goalAdherencePercent ?: 0
    val sessionCount = telemetryMetrics?.interventionCount ?: 0
    val purposeCompletion = telemetryMetrics?.purposeCompletionPercent ?: 0
    val overrunCount = telemetryMetrics?.overrunCount ?: 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    fontSize = 32.sp,
                    color = TextPrimary,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Focus Report",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Real-time on-device telemetry",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) BrandAccent else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) AppBg else TextTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Donut / Ring Chart Card (Time Reclaimed)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(190.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(170.dp)) {
                            val strokeWidth = 26f
                            drawArc(
                                color = GlassStroke,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            val progressSweep = if (successRate > 0) 360f * (successRate / 100f) else 0f
                            drawArc(
                                color = BrandAccentLight,
                                startAngle = -90f,
                                sweepAngle = progressSweep,
                                useCenter = false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$timeSavedMin min",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandAccentLight
                            )
                            Text(
                                text = "Time Reclaimed",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        LegendItem("Pause Success", "$successRate%", BrandAccentLight)
                        LegendItem("Goal Adherence", "$goalAdherence%", Color(0xFF3B82F6))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Stat Summary Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatSummaryCard(modifier = Modifier.weight(1f), value = sessionCount.toString(), label = "Sessions")
                StatSummaryCard(modifier = Modifier.weight(1f), value = "$purposeCompletion%", label = "Goal Success")
                StatSummaryCard(modifier = Modifier.weight(1f), value = "$overrunCount", label = "Overruns")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Horizontal Progress Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Goal Follow-Through & Success",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    val successFraction = if (successRate > 0) successRate / 100f else 0f
                    val adherenceFraction = if (goalAdherence > 0) goalAdherence / 100f else 0f
                    val purposeFraction = if (purposeCompletion > 0) purposeCompletion / 100f else 0f

                    HorizontalBarRow("Pause Success Rate", successFraction, "$successRate%", BrandAccentLight)
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalBarRow("Goal Follow-Through", adherenceFraction, "$goalAdherence%", Color(0xFF3B82F6))
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalBarRow("Goal Completion Rate", purposeFraction, "$purposeCompletion%", Color(0xFFF59E0B))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Summary Info Card
            val context = LocalContext.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://styli26.github.io/ScrollBrake/privacy.html"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            // ignore
                        }
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Telemetry & Privacy (Tap to view Privacy Policy)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All focus metrics, session logs, and pause telemetry remain 100% on your device. Click to read our full privacy policy.",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = TextTertiary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun StatSummaryCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BrandAccentLight)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, color = TextTertiary)
        }
    }
}

@Composable
fun HorizontalBarRow(label: String, fraction: Float, value: String, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, color = TextPrimary)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(GlassStroke, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}
