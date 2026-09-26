package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotConnectionState
import com.example.ui.components.*
import com.example.ui.theme.EdBotBlue
import com.example.ui.theme.EdBotCyan
import com.example.ui.theme.EdBotGreen
import com.example.ui.viewmodel.BotUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: BotUiState,
    onRefresh: () -> Unit,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onNavigateToConnect: () -> Unit,
    onNavigateToManagement: () -> Unit,
    onNavigateToCommands: () -> Unit,
    onNavigateToAutoReply: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onWatchBonusAd: () -> Unit,
    onClearError: () -> Unit,
    onClearSuccess: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "EDBOTS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(state = uiState.botDetails.state)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("dashboard_refresh_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.testTag("dashboard_profile_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .testTag("dashboard_scroll_list"),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Notification messages
            if (uiState.errorMessage != null) {
                item {
                    ErrorBanner(
                        message = uiState.errorMessage,
                        onRetry = onRefresh,
                        onDismiss = onClearError
                    )
                }
            }

            if (uiState.successMessage != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("success_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = EdBotGreen.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EdBotGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.successMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = EdBotGreen,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearSuccess) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = EdBotGreen)
                            }
                        }
                    }
                }
            }

            // Hero Bot Status Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("hero_status_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = uiState.botDetails.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.botDetails.phoneNumber ?: "Not linked yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(state = uiState.botDetails.state)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick metrics row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(
                                title = "Processed",
                                value = "${uiState.botDetails.messagesProcessed} msgs",
                                icon = Icons.AutoMirrored.Filled.Chat,
                                tint = EdBotGreen
                            )
                            MetricItem(
                                title = "Active Chats",
                                value = "${uiState.botDetails.activeChats}",
                                icon = Icons.Default.Group,
                                tint = EdBotCyan
                            )
                            MetricItem(
                                title = "Uptime",
                                value = "${uiState.botDetails.uptimeSeconds / 3600}h ${(uiState.botDetails.uptimeSeconds % 3600) / 60}m",
                                icon = Icons.Default.Timer,
                                tint = EdBotBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Action Button
                        if (uiState.botDetails.state != BotConnectionState.CONNECTED) {
                            Button(
                                onClick = onNavigateToConnect,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EdBotGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("dashboard_connect_whatsapp_btn")
                            ) {
                                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Connect WhatsApp (QR / Phone)", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = onStopBot,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("dashboard_stop_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stop Bot", color = MaterialTheme.colorScheme.error)
                                }

                                Button(
                                    onClick = onNavigateToManagement,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EdBotBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("dashboard_manage_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.SettingsRemote, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Control Panel")
                                }
                            }
                        }
                    }
                }
            }

            // Future Monetization Banner Placement (reusable, flexible, non-intrusive)
            item {
                AdBannerSlot(
                    isVisible = true,
                    onClick = { /* Reserved for future ad network click */ }
                )
            }

            // Quick Feature Hub Section
            item {
                Text(
                    text = "Bot Features & Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Commands",
                        subtitle = "Toggle & configure",
                        icon = Icons.Default.Terminal,
                        color = EdBotCyan,
                        onClick = onNavigateToCommands,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        title = "Auto Reply",
                        subtitle = "Smart response rules",
                        icon = Icons.AutoMirrored.Filled.ReplyAll,
                        color = EdBotGreen,
                        onClick = onNavigateToAutoReply,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "AI Chatbot",
                        subtitle = "Gemini integration",
                        icon = Icons.Default.SmartToy,
                        color = Color(0xFFA855F7),
                        onClick = onNavigateToAi,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        title = "Groups",
                        subtitle = "Anti-link & welcome",
                        icon = Icons.Default.Groups,
                        color = EdBotBlue,
                        onClick = onNavigateToGroups,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        title = "Bot Server",
                        subtitle = "Start / stop / restart",
                        icon = Icons.Default.Dns,
                        color = Color(0xFFF59E0B),
                        onClick = onNavigateToManagement,
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        title = "Settings",
                        subtitle = "API & Appwrite",
                        icon = Icons.Default.Settings,
                        color = Color(0xFF64748B),
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Server-controlled Usage Limits & Future Monetization bonus
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    UsageLimitsCard(
                        stats = uiState.usageStats,
                        onBonusClicked = onWatchBonusAd
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    title: String,
    value: String,
    icon: ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.15f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("feature_card_${title.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
