package com.example.data.model

enum class BotConnectionState {
    CONNECTED,
    CONNECTING,
    INITIALIZING,
    LOGGED_OUT,
    DISCONNECTED,
    ERROR;

    companion object {
        fun fromString(str: String?): BotConnectionState {
            return when (str?.uppercase()) {
                "CONNECTED" -> CONNECTED
                "CONNECTING" -> CONNECTING
                "INITIALIZING" -> INITIALIZING
                "LOGGED_OUT" -> LOGGED_OUT
                "DISCONNECTED" -> DISCONNECTED
                else -> DISCONNECTED
            }
        }
    }
}

data class BotDetails(
    val id: String = "edbot_1",
    val name: String = "EDBOTS Master",
    val phoneNumber: String? = "+1 (555) 019-2831",
    val state: BotConnectionState = BotConnectionState.DISCONNECTED,
    val uptimeSeconds: Long = 0L,
    val messagesProcessed: Long = 0L,
    val activeChats: Int = 0,
    val batteryLevel: Int = 92,
    val isCharging: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class PairingStatusResponse(
    val ok: Boolean = true,
    val authState: String? = null,
    val state: String? = null,
    val tokenValid: Boolean = true,
    val error: String? = null
)

data class PairingCodeResponse(
    val ok: Boolean = true,
    val code: String? = null,
    val expiresAt: Long = 0L,
    val error: String? = null
)

data class BotCommand(
    val id: String,
    val name: String,
    val description: String,
    val category: String = "General",
    val enabled: Boolean = true,
    val adminOnly: Boolean = false
)

data class AutoReplyRule(
    val id: String,
    val trigger: String,
    val response: String,
    val matchType: String = "EXACT", // EXACT, CONTAINS, STARTS_WITH
    val enabled: Boolean = true
)

data class AiConfig(
    val enabled: Boolean = true,
    val provider: String = "Gemini",
    val model: String = "gemini-1.5-flash",
    val systemPrompt: String = "You are EDBOTS, a helpful WhatsApp AI assistant.",
    val temperature: Float = 0.7f,
    val groupRepliesEnabled: Boolean = false
)

data class GroupSettings(
    val antiLink: Boolean = true,
    val welcomeMessage: Boolean = true,
    val welcomeText: String = "Welcome @user to the group! Please adhere to group rules.",
    val leaveOnAntiLink: Boolean = false,
    val adminOnlyCommands: Boolean = false
)

data class UsageStats(
    val dailyMessages: Long = 428L,
    val dailyLimit: Long = 1000L,
    val uptimeSeconds: Long = 34500L,
    val runtimeLimitSeconds: Long = 86400L,
    val tier: String = "Standard Free",
    val rewardedAdsAvailable: Boolean = true,
    val bannerAdsEnabled: Boolean = true
)
