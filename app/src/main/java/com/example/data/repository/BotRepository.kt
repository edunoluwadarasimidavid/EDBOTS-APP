package com.example.data.repository

import com.example.data.local.EdBotsDatabase
import com.example.data.local.EdBotsPreferences
import com.example.data.local.entity.AutoReplyEntity
import com.example.data.local.entity.BotEntity
import com.example.data.local.entity.CommandEntity
import com.example.data.model.*
import com.example.data.remote.EdBotsApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class BotRepository(
    private val apiClient: EdBotsApiClient,
    private val db: EdBotsDatabase,
    private val prefs: EdBotsPreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _botDetails = MutableStateFlow(
        BotDetails(
            id = "edbot_master",
            name = "EDBOTS Bot",
            phoneNumber = null,
            state = BotConnectionState.DISCONNECTED
        )
    )
    val botDetails: StateFlow<BotDetails> = _botDetails.asStateFlow()

    private val _pairingState = MutableStateFlow<PairingStatusResponse?>(null)
    val pairingState: StateFlow<PairingStatusResponse?> = _pairingState.asStateFlow()

    private val _aiConfig = MutableStateFlow(AiConfig())
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    private val _groupSettings = MutableStateFlow(GroupSettings())
    val groupSettings: StateFlow<GroupSettings> = _groupSettings.asStateFlow()

    private val _usageStats = MutableStateFlow(UsageStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()

    /** The WhatsApp chat JID new auto-reply rules are created for. */
    private val _activeChatId = MutableStateFlow("")
    val activeChatId: StateFlow<String> = _activeChatId.asStateFlow()

    // ── Commands (Room cache + POST /api/commands/{name}/enable|disable) ─────────

    val commands: StateFlow<List<BotCommand>> = db.commandDao().getAllCommands()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ── Auto-reply rules (Room cache + per-chat keyword API) ─────────────────────

    val autoReplies: StateFlow<List<AutoReplyRule>> = db.autoReplyDao().getAllRules()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        // Seed the local caches from the server on startup (best-effort).
        scope.launch { refreshCommandsFromServer() }
        scope.launch { refreshAiConfig() }
    }

    private fun CommandEntity.toDomain() = BotCommand(
        id = id,
        name = name,
        description = description,
        category = category,
        enabled = enabled,
        adminOnly = adminOnly
    )

    private fun AutoReplyEntity.toDomain() = AutoReplyRule(
        id = id,
        chatId = chatId,
        trigger = trigger,
        response = response,
        matchType = matchType,
        enabled = enabled
    )

    // ── Status & lifecycle ───────────────────────────────────────────────────────

    suspend fun refreshBotStatus(): NetworkResult<BotDetails> {
        val statusResult = apiClient.getBotStatus()
        if (statusResult is NetworkResult.Success) {
            _botDetails.value = statusResult.data
            cacheBot(statusResult.data)

            // Enrich with live stats (uptime, messages, active groups). Non-fatal on error.
            when (val stats = apiClient.getBotStats()) {
                is NetworkResult.Success -> {
                    _botDetails.value = statusResult.data.copy(
                        uptimeSeconds = stats.data.uptimeSeconds,
                        messagesProcessed = stats.data.messagesProcessed,
                        activeChats = stats.data.activeChats,
                        state = stats.data.state.takeIf { it != BotConnectionState.DISCONNECTED }
                            ?: statusResult.data.state
                    )
                    cacheBot(_botDetails.value)
                }
                else -> Unit
            }
        }
        return statusResult
    }

    private suspend fun cacheBot(details: BotDetails) {
        db.botDao().insertOrUpdate(
            BotEntity(
                id = details.id,
                name = details.name,
                phoneNumber = details.phoneNumber,
                state = details.state.name,
                uptimeSeconds = details.uptimeSeconds,
                messagesProcessed = details.messagesProcessed,
                activeChats = details.activeChats,
                batteryLevel = details.batteryLevel,
                isCharging = details.isCharging,
                lastSeenTimestamp = details.lastSeenTimestamp
            )
        )
    }

    /**
     * Bot lifecycle actions. The real API only exposes start and stop:
     *  - start: reconnect in-process with the stored session (202 while connecting).
     *  - stop: graceful stop, WhatsApp session preserved.
     * restart is composed as stop → start; logout is NOT an API endpoint, so
     * disconnecting from the app means stopping the bot (session preserved).
     */
    suspend fun performAction(action: String): NetworkResult<String> {
        return when (action.lowercase()) {
            "start" -> apiClient.startBot()
            "stop" -> apiClient.stopBot()
            "restart" -> {
                val stopRes = apiClient.stopBot()
                if (stopRes is NetworkResult.Error) return stopRes
                val startRes = apiClient.startBot()
                when (startRes) {
                    is NetworkResult.Success ->
                        NetworkResult.Success("Bot restarted. ${startRes.data}")
                    else -> startRes
                }
            }
            "logout", "disconnect" -> apiClient.stopBot()
            else -> NetworkResult.Error("Unsupported bot action: $action", 400)
        }
    }

    // ── Pairing (web pairing surface /pair/*, token-gated) ───────────────────────

    /** Polls GET /pair/status: auth state, QR image, and any issued pairing code. */
    suspend fun checkPairingStatus(token: String? = null): NetworkResult<PairingStatusResponse> {
        val res = apiClient.checkPairingStatus(token)
        if (res is NetworkResult.Success) {
            _pairingState.value = res.data
            val st = BotConnectionState.fromString(res.data.state ?: "")
            _botDetails.value = _botDetails.value.copy(state = st)
        }
        return res
    }

    /**
     * Requests a phone-number pairing code. The server returns 202 with no code;
     * the actual code appears on the next /pair/status poll (auth.pairingCode).
     */
    suspend fun requestPairingCode(phone: String): NetworkResult<PairingCodeResponse> {
        return apiClient.requestPairingCode(phone)
    }

    suspend fun resetPairing(): NetworkResult<Boolean> {
        _pairingState.value = null
        return apiClient.resetPairing()
    }

    // ── Commands ─────────────────────────────────────────────────────────────────

    private suspend fun refreshCommandsFromServer() {
        when (val res = apiClient.fetchCommands()) {
            is NetworkResult.Success -> {
                db.commandDao().clear()
                db.commandDao().insertAll(
                    res.data.map {
                        CommandEntity(it.id, it.name, it.description, it.category, it.enabled, it.adminOnly)
                    }
                )
            }
            else -> Unit // keep cached commands; UI already shows defaults on first run
        }
    }

    suspend fun toggleCommand(id: String, enabled: Boolean) {
        db.commandDao().updateCommandState(id, enabled)
        val apiRes = apiClient.setCommandEnabled(id, enabled)
        if (apiRes is NetworkResult.Error) {
            // Revert optimistic update when the server rejects it.
            db.commandDao().updateCommandState(id, !enabled)
        }
    }

    // ── Auto-reply (keyword rules per chat) ──────────────────────────────────────

    fun setActiveChatId(chatId: String) {
        _activeChatId.value = chatId.trim()
    }

    suspend fun addAutoReply(trigger: String, response: String, matchType: String) {
        val chatId = _activeChatId.value
        val rule = AutoReplyEntity(
            id = "rule_${UUID.randomUUID().toString().take(8)}",
            chatId = chatId,
            trigger = trigger,
            response = response,
            matchType = matchType,
            enabled = true
        )
        db.autoReplyDao().insertRule(rule)
        if (chatId.isNotBlank()) {
            apiClient.addAutoReplyKeyword(chatId, trigger, response, caseSensitive = false)
        }
    }

    suspend fun deleteAutoReply(id: String) {
        val rule = db.autoReplyDao().getAllRules().firstOrNull()
            ?.find { it.id == id }
        db.autoReplyDao().deleteById(id)
        if (rule != null && rule.chatId.isNotBlank()) {
            apiClient.deleteAutoReplyKeyword(rule.chatId, rule.trigger)
        }
    }

    suspend fun toggleAutoReply(id: String, enabled: Boolean) {
        db.autoReplyDao().updateRuleEnabled(id, enabled)
    }

    /** Global auto-reply master switch (PATCH /api/autoreply). */
    suspend fun setAutoReplyGlobal(enabled: Boolean): NetworkResult<Boolean> =
        apiClient.setAutoReplyGlobal(enabled)

    // ── AI settings (GET/PATCH /api/ai) ──────────────────────────────────────────

    suspend fun refreshAiConfig() {
        when (val res = apiClient.getAiSettings()) {
            is NetworkResult.Success -> _aiConfig.value = res.data
            else -> Unit
        }
    }

    /** Persists AI enabled + personality on the server; other fields stay local-only. */
    suspend fun updateAiConfig(config: AiConfig) {
        _aiConfig.value = config
        apiClient.updateAiSettings(config.enabled, config.personality)
    }

    // ── Group settings (GET/PATCH /api/groups/{id}/settings) ─────────────────────

    /** Fetches the list of groups the bot is in (GET /api/groups). */
    suspend fun fetchGroups(): NetworkResult<List<GroupSummary>> = apiClient.getGroups()

    fun updateGroupSettings(settings: GroupSettings) {
        _groupSettings.value = settings
    }

    /** Loads moderation settings for a group from the server. */
    suspend fun loadGroupSettings(groupId: String): NetworkResult<GroupSettings> {
        val res = apiClient.getGroupSettings(groupId)
        if (res is NetworkResult.Success) _groupSettings.value = res.data
        return res
    }

    /** Saves moderation settings for a group on the server. */
    suspend fun saveGroupSettings(groupId: String, settings: GroupSettings): NetworkResult<GroupSettings> {
        val res = apiClient.updateGroupSettings(groupId, settings)
        if (res is NetworkResult.Success) _groupSettings.value = res.data
        return res
    }

    // ── Monetization placeholder (kept; server has no usage-limit endpoint) ──────

    fun addUnlockedMessages(messagesCount: Long) {
        _usageStats.value = _usageStats.value.copy(
            dailyLimit = _usageStats.value.dailyLimit + messagesCount
        )
    }
}
