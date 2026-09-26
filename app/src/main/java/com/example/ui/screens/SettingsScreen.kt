package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.EdBotsTopBar
import com.example.ui.theme.EdBotBlue
import com.example.ui.theme.EdBotGreen
import com.example.ui.viewmodel.SettingsUiState

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUpdateApiBaseUrl: (String) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onUpdatePairingToken: (String) -> Unit,
    onUpdateAppwriteEndpoint: (String) -> Unit,
    onUpdateAppwriteProjectId: (String) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onTestConnection: () -> Unit,
    onClearTestResult: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var apiUrl by remember(uiState.apiBaseUrl) { mutableStateOf(uiState.apiBaseUrl) }
    var apiKey by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var pairingToken by remember(uiState.pairingToken) { mutableStateOf(uiState.pairingToken) }
    var appwriteEndpoint by remember(uiState.appwriteEndpoint) { mutableStateOf(uiState.appwriteEndpoint) }
    var appwriteProjectId by remember(uiState.appwriteProjectId) { mutableStateOf(uiState.appwriteProjectId) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            EdBotsTopBar(
                title = "Settings",
                onBack = onBack
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
                .testTag("settings_screen")
        ) {
            // Server & API Configuration Section
            Text(
                text = "EDBOTS Server API Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EdBotBlue
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = {
                            apiUrl = it
                            onUpdateApiBaseUrl(it)
                        },
                        label = { Text("EDBOTS API Base URL") },
                        placeholder = { Text("https://edbots.mnz.dom.my.id") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_api_url_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            onUpdateApiKey(it)
                        },
                        label = { Text("EDBOTS API Key (Header: X-API-Key)") },
                        placeholder = { Text("Bearer / X-API-Key token") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pairingToken,
                        onValueChange = {
                            pairingToken = it
                            onUpdatePairingToken(it)
                        },
                        label = { Text("Default Pairing Token") },
                        placeholder = { Text("Server console pairing token") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_pairing_token_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !uiState.isTestingConnection,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("test_connection_btn")
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Server Connection")
                        }
                    }

                    if (uiState.connectionTestResult != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = uiState.connectionTestResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isSuccess) EdBotGreen else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appwrite Backend Configuration Section
            Text(
                text = "Appwrite Auth & Cloud Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EdBotGreen
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = appwriteEndpoint,
                        onValueChange = {
                            appwriteEndpoint = it
                            onUpdateAppwriteEndpoint(it)
                        },
                        label = { Text("Appwrite Endpoint") },
                        placeholder = { Text("https://cloud.appwrite.io/v1") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_appwrite_endpoint_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = appwriteProjectId,
                        onValueChange = {
                            appwriteProjectId = it
                            onUpdateAppwriteProjectId(it)
                        },
                        label = { Text("Appwrite Project ID") },
                        placeholder = { Text("edbots_app") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("settings_appwrite_project_id_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Appearance & App Preferences
            Text(
                text = "App Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Dark Mode Theme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Optimized high-contrast dark palette",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.darkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        modifier = Modifier.testTag("dark_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Logout Button
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("settings_logout_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out of Appwrite Account",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to log out of EDBOTS? You will need to log in again to manage your WhatsApp bot.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
