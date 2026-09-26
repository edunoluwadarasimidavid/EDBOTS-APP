package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BotConnectionState
import com.example.ui.components.EdBotsTopBar
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EdBotBlue
import com.example.ui.theme.EdBotGreen
import com.example.ui.viewmodel.BotUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectBotScreen(
    uiState: BotUiState,
    onBack: () -> Unit,
    onRequestPhoneCode: (phoneNumber: String) -> Unit,
    onResetPairing: () -> Unit,
    onSetPairingToken: (token: String) -> Unit,
    onRefreshStatus: () -> Unit,
    onClearError: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: QR Code, 1: Phone Number
    var countryCode by remember { mutableStateOf("+1") }
    var phoneInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf(uiState.pairingToken) }
    var showTokenConfig by remember { mutableStateOf(uiState.pairingToken.isBlank()) }
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Countdown timer for pairing code
    var secondsLeft by remember { mutableLongStateOf(0L) }
    LaunchedEffect(uiState.pairingCodeExpiresAt) {
        if (uiState.pairingCodeExpiresAt > System.currentTimeMillis()) {
            while (uiState.pairingCodeExpiresAt > System.currentTimeMillis()) {
                secondsLeft = (uiState.pairingCodeExpiresAt - System.currentTimeMillis()) / 1000
                delay(1000)
            }
            secondsLeft = 0L
        } else {
            secondsLeft = 0L
        }
    }

    Scaffold(
        topBar = {
            EdBotsTopBar(
                title = "Link WhatsApp",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onRefreshStatus) {
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
                .testTag("connect_bot_screen")
        ) {
            // Status and Reset Header
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
                        Text(
                            text = "EDBOTS Server Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusBadge(state = uiState.botDetails.state)
                    }

                    Row {
                        IconButton(
                            onClick = { showTokenConfig = !showTokenConfig },
                            modifier = Modifier.testTag("toggle_token_config_btn")
                        ) {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = "Pairing Token", tint = EdBotBlue)
                        }
                        IconButton(
                            onClick = onResetPairing,
                            modifier = Modifier.testTag("reset_pairing_btn")
                        ) {
                            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset Session", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Optional Pairing Token Input Card
            AnimatedVisibility(visible = showTokenConfig) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Server Pairing Token",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Found in your EDBOTS server console. If not required by your server, leave blank.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            placeholder = { Text("e.g. edbots_token_xyz") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onSetPairingToken(tokenInput)
                                showTokenConfig = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EdBotBlue),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Token")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.errorMessage != null) {
                ErrorBanner(
                    message = uiState.errorMessage,
                    onDismiss = onClearError
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Two Clearly Separated Connection Tabs: QR Code vs Phone Number
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("connection_tab_row"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("QR Code")
                        }
                    },
                    modifier = Modifier.testTag("tab_qr_code")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phone Number")
                        }
                    },
                    modifier = Modifier.testTag("tab_phone_number")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                0 -> {
                    // QR Code Option Panel
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth().testTag("qr_option_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Scan QR with WhatsApp",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Link EDBOTS WhatsApp session directly to server",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // QR Frame / Canvas
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(2.dp, EdBotGreen, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "QR Code",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(180.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Clear Step-by-Step Instructions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                InstructionStep(number = "1", text = "Open WhatsApp on your mobile phone")
                                Spacer(modifier = Modifier.height(8.dp))
                                InstructionStep(number = "2", text = "Tap Menu (⋮) or Settings > Linked Devices")
                                Spacer(modifier = Modifier.height(8.dp))
                                InstructionStep(number = "3", text = "Tap 'Link a Device' and point your camera at this QR")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onRefreshStatus,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EdBotGreen),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh QR Code")
                            }
                        }
                    }
                }
                1 -> {
                    // Phone Number Option Panel
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth().testTag("phone_option_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Pair with Phone Number",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Receive an 8-character pairing code on your WhatsApp app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Phone input row with Country Code
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = countryCode,
                                    onValueChange = { countryCode = it },
                                    label = { Text("Code") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.width(88.dp).testTag("country_code_input")
                                )

                                OutlinedTextField(
                                    value = phoneInput,
                                    onValueChange = { phoneInput = it },
                                    label = { Text("WhatsApp Phone Number") },
                                    placeholder = { Text("e.g. 5550192831") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("phone_number_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val fullNumber = countryCode + phoneInput
                                    onRequestPhoneCode(fullNumber)
                                },
                                enabled = !uiState.isPairingLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EdBotGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("request_pairing_code_btn")
                            ) {
                                if (uiState.isPairingLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Request WhatsApp Pairing Code")
                                }
                            }

                            // Display Generated Pairing Code
                            if (uiState.pairingCode != null) {
                                Spacer(modifier = Modifier.height(24.dp))

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EdBotGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "ENTER THIS CODE IN WHATSAPP",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EdBotGreen,
                                            letterSpacing = 1.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Large Readable Pairing Code Box
                                        Text(
                                            text = uiState.pairingCode,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 4.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.testTag("pairing_code_display")
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (secondsLeft > 0) {
                                            val minutes = secondsLeft / 60
                                            val seconds = secondsLeft % 60
                                            Text(
                                                text = "Expires in %d:%02d".format(minutes, seconds),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (secondsLeft < 30) MaterialTheme.colorScheme.error else EdBotBlue
                                            )
                                        } else {
                                            Text(
                                                text = "Code expired — please request a new code",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        OutlinedButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(uiState.pairingCode))
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy Code")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Instructions for Phone Pairing
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                InstructionStep(number = "1", text = "Enter your WhatsApp phone number with country code")
                                Spacer(modifier = Modifier.height(8.dp))
                                InstructionStep(number = "2", text = "Check your phone for a WhatsApp notification: 'Enter code to link a new device'")
                                Spacer(modifier = Modifier.height(8.dp))
                                InstructionStep(number = "3", text = "Type the 8-character code shown above into WhatsApp")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionStep(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = EdBotGreen.copy(alpha = 0.2f),
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EdBotGreen
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
