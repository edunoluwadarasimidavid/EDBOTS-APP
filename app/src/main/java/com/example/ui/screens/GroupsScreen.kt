package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GroupSettings
import com.example.data.model.GroupSummary
import com.example.ui.components.EdBotsTopBar
import com.example.ui.theme.EdBotGreen

@Composable
fun GroupsScreen(
    currentSettings: GroupSettings,
    groups: List<GroupSummary>,
    selectedGroupId: String?,
    onFetchGroups: () -> Unit,
    onSelectGroup: (String) -> Unit,
    onSaveSettings: (GroupSettings) -> Unit,
    onBack: () -> Unit
) {
    // Reset local edit state whenever the settings object is replaced (e.g. loaded from server).
    var antiLink by remember(currentSettings) { mutableStateOf(currentSettings.antiLink) }
    var welcomeMessage by remember(currentSettings) { mutableStateOf(currentSettings.welcomeMessage) }
    var welcomeText by remember(currentSettings) { mutableStateOf(currentSettings.welcomeText) }
    var leaveOnAntiLink by remember(currentSettings) { mutableStateOf(currentSettings.leaveOnAntiLink) }
    var adminOnlyCommands by remember(currentSettings) { mutableStateOf(currentSettings.adminOnlyCommands) }
    var groupMenuExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            EdBotsTopBar(
                title = "Group Management",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            onSaveSettings(
                                GroupSettings(
                                    antiLink = antiLink,
                                    welcomeMessage = welcomeMessage,
                                    welcomeText = welcomeText,
                                    leaveOnAntiLink = leaveOnAntiLink,
                                    adminOnlyCommands = adminOnlyCommands
                                )
                            )
                        },
                        modifier = Modifier.testTag("save_groups_top_btn")
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
                .testTag("groups_screen")
        ) {
            // Group Picker Card — settings apply to the selected WhatsApp group.
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Target Group",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedGroupId == null)
                            "Pick a group to manage its moderation settings on the server."
                        else
                            "Editing settings for the selected group",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box {
                        OutlinedButton(
                            onClick = {
                                if (groups.isEmpty()) onFetchGroups() else groupMenuExpanded = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("group_picker_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Group, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = groups.find { it.id == selectedGroupId }?.name
                                    ?: (selectedGroupId ?: "Select / refresh groups")
                            )
                        }
                        DropdownMenu(
                            expanded = groupMenuExpanded,
                            onDismissRequest = { groupMenuExpanded = false }
                        ) {
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text("${group.name} (${group.size})") },
                                    onClick = {
                                        groupMenuExpanded = false
                                        onSelectGroup(group.id)
                                    }
                                )
                            }
                        }
                    }

                    TextButton(onClick = onFetchGroups) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = EdBotGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh group list from server", color = EdBotGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Group Moderation & Anti-Link Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = EdBotGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Group Protection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = "Anti-Link Protection",
                        description = "Automatically delete links sent in group by non-admins",
                        checked = antiLink,
                        onCheckedChange = { antiLink = it },
                        tag = "switch_antilink"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "Warn & Kick on Violation",
                        description = "Auto-remove member if link policy is breached repeatedly",
                        checked = leaveOnAntiLink,
                        onCheckedChange = { leaveOnAntiLink = it },
                        tag = "switch_kick_antilink"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "Admin Only Commands",
                        description = "Restrict high-privilege commands like /tagall to group admins",
                        checked = adminOnlyCommands,
                        onCheckedChange = { adminOnlyCommands = it },
                        tag = "switch_admin_commands"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Welcome Message Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Member Greetings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingToggleRow(
                        title = "Welcome New Members",
                        description = "Send customized welcome message when a user joins the group",
                        checked = welcomeMessage,
                        onCheckedChange = { welcomeMessage = it },
                        tag = "switch_welcome_msg"
                    )

                    if (welcomeMessage) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = welcomeText,
                            onValueChange = { welcomeText = it },
                            label = { Text("Welcome Template") },
                            placeholder = { Text("Welcome @user to the group!") },
                            minLines = 3,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("welcome_text_input")
                        )
                        Text(
                            text = "Tip: Use @user to mention the newly joined member.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSaveSettings(
                        GroupSettings(
                            antiLink = antiLink,
                            welcomeMessage = welcomeMessage,
                            welcomeText = welcomeText,
                            leaveOnAntiLink = leaveOnAntiLink,
                            adminOnlyCommands = adminOnlyCommands
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EdBotGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_groups_btn")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedGroupId != null) "Save To Server" else "Save Locally")
            }
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
