package com.example.data.model

enum class BotConnectionState {
    CONNECTED,
    CONNECTING,
    INITIALIZING,
    LOGGED_OUT,
    DISCONNECTED,
    ERROR;

    companion object {
        /**
         * Maps server status values to UI states. The EDBOTS API reports
         * lowercase `offline | connecting | online`; pairing auth states are
         * uppercase (WAITING_FOR_AUTH, QR_READY, CONNECTED, FAILED, ...).
         */
        fun fromString(str: String?): BotConnectionState {
            return when (str?.trim()?.uppercase()) {
                "ONLINE", "CONNECTED" -> CONNECTED
                "CONNECTING" -> CONNECTING
                "INITIALIZING", "WAITING_FOR_AUTH", "QR_READY", "PAIRING_CODE_REQUESTED" -> INITIALIZING
                "LOGGED_OUT" -> LOGGED_OUT
                "FAILED", "ERROR" -> ERROR
                "OFFLINE", "DISCONNECTED" -> DISCONNECTED
                else -> DISCONNECTED
            }
        }
    }
}

data class BotDetails(
    val id: String = "edbot_1",
    val name: String = "EDBOTS Master",
    val phoneNumber: String? = null,
    val state: BotConnectionState = BotConnectionState.DISCONNECTED,
    val uptimeSeconds: Long = 0L,
    val messagesProcessed: Long = 0L,
    val activeChats: Int = 0,
    /** The real API exposes no battery info; 0 means "not available". */
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val lastDisconnectReason: String? = null
)

data class PairingStatusResponse(
    val ok: Boolean = true,
    val authState: String? = null,
    val state: String? = null,
    val tokenValid: Boolean = true,
    /** PNG data-URL of the QR code, only present when state == QR_READY. */
    val qrImage: String? = null,
    /** Short-lived display code, e.g. "ABCD-EFGH", while a code pairing is active. */
    val pairingCode: String? = null,
    /** Epoch millis when [pairingCode] expires (0 = unknown). */
    val pairingCodeExpiresAt: Long = 0L,
    /** Machine reason string when state == FAILED. */
    val failureReason: String? = null,
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

/**
 * Keyword-based auto-reply rule. On the server, rules live per chat JID
 * (POST /api/autoreply/chat/{chatId}/keywords).
 */
data class AutoReplyRule(
    val id: String,
    val chatId: String = "",
    val trigger: String,
    val response: String,
    val matchType: String = "EXACT", // EXACT, CONTAINS, STARTS_WITH
    val enabled: Boolean = true
)

data class AiConfig(
    val enabled: Boolean = true,
    /** Server-supported personality: friendly | professional | funny | concise | custom. */
    val personality: String = "friendly",
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

/** Group summary from GET /api/groups. */
data class GroupSummary(
    val id: String,
    val name: String,
    val size: Int = 0,
    val isBotAdmin: Boolean = false
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
