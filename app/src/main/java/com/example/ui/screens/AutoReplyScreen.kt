package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutoReplyRule
import com.example.ui.components.EdBotsTopBar
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.EdBotBlue
import com.example.ui.theme.EdBotGreen

@Composable
fun AutoReplyScreen(
    rules: List<AutoReplyRule>,
    activeChatId: String?,
    onSetActiveChatId: (String) -> Unit,
    onAddRule: (trigger: String, response: String, matchType: String) -> Unit,
    onDeleteRule: (id: String) -> Unit,
    onToggleRule: (id: String, enabled: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var chatInput by remember(activeChatId) { mutableStateOf(activeChatId ?: "") }

    Scaffold(
        topBar = {
            EdBotsTopBar(
                title = "Auto Reply Rules",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EdBotGreen,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_auto_reply")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .testTag("auto_reply_screen")
        ) {
            // Chat scope card — the server stores keyword rules per chat JID.
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Target Chat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Keyword rules are stored per chat on the server (JID, e.g. 2348012345678@s.whatsapp.net or 123...@g.us).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        label = { Text("Chat JID") },
                        placeholder = { Text("2348012345678@s.whatsapp.net") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("chat_jid_input")
                    )
                    TextButton(
                        onClick = { onSetActiveChatId(chatInput.trim()) },
                        enabled = chatInput.trim().contains("@")
                    ) {
                        Text(
                            "Apply Chat Scope",
                            color = if (chatInput.trim().contains("@")) EdBotGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (rules.isEmpty()) {
                EmptyStateView(
                    icon = Icons.AutoMirrored.Filled.ReplyAll,
                    title = "No Auto-Replies Configured",
                    message = "Add smart automated replies to automatically answer incoming WhatsApp messages.",
                    actionText = "Create First Rule",
                    onAction = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Active Rules (${rules.count { it.enabled }} / ${rules.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(rules, key = { it.id }) { rule ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rule_card_${rule.id}")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Trigger: ",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "\"${rule.trigger}\"",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (rule.enabled) EdBotGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EdBotBlue.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = rule.matchType,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EdBotBlue,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = rule.enabled,
                                        onCheckedChange = { onToggleRule(rule.id, it) },
                                        modifier = Modifier.testTag("rule_switch_${rule.id}")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Response:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = rule.response,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { onDeleteRule(rule.id) },
                                        modifier = Modifier.testTag("delete_rule_${rule.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Rule",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { trigger, response, matchType ->
                onAddRule(trigger, response, matchType)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (trigger: String, response: String, matchType: String) -> Unit
) {
    var trigger by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf("CONTAINS") }

    val matchOptions = listOf("CONTAINS", "EXACT", "STARTS_WITH")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Auto-Reply Rule", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Trigger Keyword / Text") },
                    placeholder = { Text("e.g. pricing, hello") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_rule_trigger_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Match Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matchOptions.forEach { option ->
                        FilterChip(
                            selected = matchType == option,
                            onClick = { matchType = option },
                            label = { Text(option, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = response,
                    onValueChange = { response = it },
                    label = { Text("Bot Auto-Response") },
                    placeholder = { Text("Message to send back...") },
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("add_rule_response_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(trigger, response, matchType) },
                enabled = trigger.isNotBlank() && response.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EdBotGreen),
                modifier = Modifier.testTag("dialog_save_rule_btn")
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
