package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotConnectionState
import com.example.ui.components.EdBotsTopBar
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EdBotBlue
import com.example.ui.theme.EdBotCyan
import com.example.ui.theme.EdBotGreen
import com.example.ui.viewmodel.BotUiState

@Composable
fun BotManagementScreen(
    uiState: BotUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onRestartBot: () -> Unit,
    onDisconnectBot: () -> Unit,
    onClearError: () -> Unit
) {
    var showDisconnectDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            EdBotsTopBar(
                title = "Bot Management",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("manage_refresh_btn")) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
                .testTag("bot_management_screen")
        ) {
            if (uiState.errorMessage != null) {
                ErrorBanner(
                    message = uiState.errorMessage,
                    onRetry = onRefresh,
                    onDismiss = onClearError
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Connection Status & Basic Info Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = uiState.botDetails.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "ID: ${uiState.botDetails.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(state = uiState.botDetails.state)
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(18.dp))

                    DetailRow(label = "WhatsApp Number", value = uiState.botDetails.phoneNumber ?: "Not connected")
                    DetailRow(label = "Uptime", value = "${uiState.botDetails.uptimeSeconds / 3600} hours ${(uiState.botDetails.uptimeSeconds % 3600) / 60} mins")
                    DetailRow(label = "Messages Handled", value = "${uiState.botDetails.messagesProcessed}")
                    DetailRow(label = "Active Conversations", value = "${uiState.botDetails.activeChats}")
                    DetailRow(label = "Battery / Power", value = "${uiState.botDetails.batteryLevel}% (Charging)")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Remote Server Operations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onStartBot,
                    enabled = !uiState.isLoading && uiState.botDetails.state != BotConnectionState.CONNECTED,
                    colors = ButtonDefaults.buttonColors(containerColor = EdBotGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("mgmt_start_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Bot")
                }

                Button(
                    onClick = onStopBot,
                    enabled = !uiState.isLoading && uiState.botDetails.state == BotConnectionState.CONNECTED,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("mgmt_stop_btn")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Bot")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRestartBot,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("mgmt_restart_btn")
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = EdBotBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restart Session")
                }

                OutlinedButton(
                    onClick = { showDisconnectDialog = true },
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("mgmt_disconnect_btn")
                ) {
                    Icon(imageVector = Icons.Default.LinkOff, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Disconnect")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Server Telemetry Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = EdBotCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Server Telemetry & Session Health",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "• Node Engine: Baileys Multi-Device Client\n• Session Storage: Independent Remote Server\n• Transport: Secure TLS WebSocket\n• Latency: ~34ms (Direct Node Ping)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }

    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Disconnect WhatsApp Session?") },
            text = { Text("This will log the bot out of WhatsApp on the EDBOTS server. You will need to scan QR or pair via phone number again.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectDialog = false
                        onDisconnectBot()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
