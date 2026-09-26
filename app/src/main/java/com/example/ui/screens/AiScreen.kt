package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiConfig
import com.example.ui.components.EdBotsTopBar
import com.example.ui.theme.EdBotGreen

@Composable
fun AiScreen(
    currentConfig: AiConfig,
    onSaveConfig: (AiConfig) -> Unit,
    onBack: () -> Unit
) {
    var enabled by remember(currentConfig) { mutableStateOf(currentConfig.enabled) }
    var selectedModel by remember(currentConfig) { mutableStateOf(currentConfig.model) }
    var systemPrompt by remember(currentConfig) { mutableStateOf(currentConfig.systemPrompt) }
    var temperature by remember(currentConfig) { mutableFloatStateOf(currentConfig.temperature) }
    var groupReplies by remember(currentConfig) { mutableStateOf(currentConfig.groupRepliesEnabled) }
    var selectedPersonality by remember(currentConfig) { mutableStateOf(currentConfig.personality) }

    // Personalities supported by PATCH /api/ai on the EDBOTS server.
    val personalities = listOf("friendly", "professional", "funny", "concise", "custom")
    val models = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gpt-4o-mini", "claude-3-haiku")
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            EdBotsTopBar(
                title = "AI Configuration",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {                            onSaveConfig(
                                AiConfig(
                                    enabled = enabled,
                                    personality = selectedPersonality,
                                    provider = currentConfig.provider,
                                    model = selectedModel,
                                    systemPrompt = systemPrompt,
                                    temperature = temperature,
                                    groupRepliesEnabled = groupReplies
                                )
                            )
                        },
                        modifier = Modifier.testTag("save_ai_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = EdBotGreen)
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
                .testTag("ai_screen")
        ) {
            // Main AI Master Toggle
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EdBotGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "WhatsApp AI Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (enabled) "Active • Generating replies" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (enabled) EdBotGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        modifier = Modifier.testTag("ai_master_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Model & Intelligence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // AI Model Picker Chips
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Remote Inference Model",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    models.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedModel == model,
                                onClick = { selectedModel = model },
                                colors = RadioButtonDefaults.colors(selectedColor = EdBotGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = model, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Bot Personality",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Personality chips — saved to the server via PATCH /api/ai.
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Synced to your EDBOTS server",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        personalities.forEach { personality ->
                            FilterChip(
                                selected = selectedPersonality == personality,
                                onClick = { selectedPersonality = personality },
                                label = { Text(personality.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Personality & Prompt Instructions",
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
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("System Instructions") },
                        placeholder = { Text("Define how your bot should respond...") },
                        minLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ai_system_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Creativity (Temperature: ${String.format("%.2f", temperature)})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = EdBotGreen,
                            activeTrackColor = EdBotGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Reply in WhatsApp Groups", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Allow AI to reply when mentioned (@bot) inside group chats",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = groupReplies,
                            onCheckedChange = { groupReplies = it },
                            modifier = Modifier.testTag("ai_group_replies_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSaveConfig(
                        AiConfig(
                            enabled = enabled,
                            model = selectedModel,
                            systemPrompt = systemPrompt,
                            temperature = temperature,
                            groupRepliesEnabled = groupReplies
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EdBotGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_ai_config_btn")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save AI Settings")
            }
        }
    }
}
