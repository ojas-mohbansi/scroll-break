package com.cascade26.scrollbreak.Activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cascade26.scrollbreak.Security.SensitiveAppPolicy
import com.cascade26.scrollbreak.Services.AppUsageAccessibilityService
import com.cascade26.scrollbreak.Services.UsageStatsTracker
import com.cascade26.scrollbreak.Utils.AppOperationalModeManager

class BankingProtectionSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                BankingProtectionScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BankingProtectionScreen(onBackClick: () -> Unit) {
        val context = this
        var currentMode by remember {
            mutableStateOf(AppOperationalModeManager.getOperationalMode(context))
        }

        val sensitivePolicy = remember { SensitiveAppPolicy.getInstance(context) }
        val protectedApps = remember { sensitivePolicy.getAllSensitivePackages().toList().sorted() }

        val hasUsageStats = UsageStatsTracker.hasUsageStatsPermission(context)
        val isAccessibilityEnabled = AppUsageAccessibilityService.serviceStatus

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Banking & Sensitive Apps Protection", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        TextButton(onClick = onBackClick) {
                            Text("Back", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Guaranteed Financial Security",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ScrollBreak NEVER reads screen text, inspects views, or displays overlays on banking, UPI, payment, or password apps.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Operational Mode Selector
                item {
                    Text(
                        text = "Operational Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = currentMode == AppOperationalModeManager.OperationalMode.LEVEL_1_BASIC_WELLBEING,
                                    onClick = {
                                        currentMode = AppOperationalModeManager.OperationalMode.LEVEL_1_BASIC_WELLBEING
                                        AppOperationalModeManager.setOperationalMode(context, currentMode)
                                    }
                                )
                                Column {
                                    Text("Level 1: Basic Digital Wellbeing Mode", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Uses UsageStatsManager only. Requires NO Accessibility Service. 100% Banking App Compatible.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = currentMode == AppOperationalModeManager.OperationalMode.LEVEL_2_ADVANCED_ACCESSIBILITY,
                                    onClick = {
                                        currentMode = AppOperationalModeManager.OperationalMode.LEVEL_2_ADVANCED_ACCESSIBILITY
                                        AppOperationalModeManager.setOperationalMode(context, currentMode)
                                    }
                                )
                                Column {
                                    Text("Level 2: Advanced Least-Privilege Mode", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Uses Accessibility Service with dynamic package filtering strictly for selected social media & browser targets.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Status Indicators
                item {
                    Text(text = "System Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                item {
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusRow(
                                label = "UsageStats Permission",
                                isOk = hasUsageStats,
                                okText = "Granted",
                                errorText = "Not Granted"
                            )
                            StatusRow(
                                label = "Accessibility Service",
                                isOk = isAccessibilityEnabled,
                                okText = "Active (Filtered)",
                                errorText = "Disabled"
                            )
                        }
                    }
                }

                // Protected Packages List
                item {
                    Text(
                        text = "Protected Applications (${protectedApps.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                items(protectedApps) { pkg ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(pkg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "EXCLUDED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StatusRow(label: String, isOk: Boolean, okText: String, errorText: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp)
            Text(
                text = if (isOk) okText else errorText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOk) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
        }
    }
}
