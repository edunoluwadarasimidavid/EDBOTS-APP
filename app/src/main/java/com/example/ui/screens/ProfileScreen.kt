package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppwriteUser
import com.example.data.model.PremiumStatus
import com.example.ui.components.EdBotsTopBar
import com.example.ui.theme.EdBotBlue
import com.example.ui.theme.EdBotGreen

@Composable
fun ProfileScreen(
    user: AppwriteUser?,
    premiumStatus: PremiumStatus?,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val displayName = user?.name?.ifBlank { "User" } ?: "User"
    val email = user?.email?.ifBlank { "No email associated" } ?: "No email associated"
    val userId = user?.id ?: "local_guest"

    Scaffold(
        topBar = {
            EdBotsTopBar(
                title = "Account Profile",
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
                .padding(20.dp)
                .testTag("profile_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // User Avatar Initial Circle
            Surface(
                shape = CircleShape,
                color = EdBotGreen,
                modifier = Modifier.size(90.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Verified, contentDescription = null, tint = EdBotGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Appwrite Verified Account",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (premiumStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (premiumStatus.isPremium) EdBotBlue.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (premiumStatus.isPremium) Icons.Default.WorkspacePremium else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (premiumStatus.isPremium) EdBotBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (premiumStatus.isPremium) "${premiumStatus.tier} Member" else "Free Tier",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (premiumStatus.isPremium) EdBotBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account Metadata Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(text = "Appwrite Identity Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileItem(label = "User ID", value = userId)
                    ProfileItem(label = "Email Address", value = email)
                    ProfileItem(label = "Session Security", value = "Isolated from WhatsApp Session")
                    ProfileItem(label = "Cloud Sync", value = "Enabled (Appwrite Preferences API)")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Security & Privacy Notice
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = EdBotBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your WhatsApp credentials and bot runtime data are hosted exclusively on the remote EDBOTS node server. Appwrite only stores your user profile and application settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_logout_btn")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out of Account")
            }
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
