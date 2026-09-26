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
            name = "EDBOTS Master",
            phoneNumber = null,
            state = BotConnectionState.DISCONNECTED
        )
    )
    val botDetails: StateFlow<BotDetails> = _botDetails.asStateFlow()

    private val _pairingCodeState = MutableStateFlow<PairingCodeResponse?>(null)
    val pairingCodeState: StateFlow<PairingCodeResponse?> = _pairingCodeState.asStateFlow()

    private val _aiConfig = MutableStateFlow(AiConfig())
    val aiConfig: StateFlow<AiConfig> = _aiConfig.asStateFlow()

    private val _groupSettings = MutableStateFlow(GroupSettings())
    val groupSettings: StateFlow<GroupSettings> = _groupSettings.asStateFlow()

    private val _usageStats = MutableStateFlow(UsageStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()

    // Room DB backed flows
    val commands: StateFlow<List<BotCommand>> = db.commandDao().getAllCommands()
        .map { entities ->
            if (entities.isEmpty()) {
                apiClient.getDefaultCommands()
            } else {
                entities.map {
                    BotCommand(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        category = it.category,
                        enabled = it.enabled,
                        adminOnly = it.adminOnly
                    )
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, apiClient.getDefaultCommands())

    val autoReplies: StateFlow<List<AutoReplyRule>> = db.autoReplyDao().getAllRules()
        .map { entities ->
            if (entities.isEmpty()) {
                getDefaultRules()
            } else {
                entities.map {
                    AutoReplyRule(
                        id = it.id,
                        trigger = it.trigger,
                        response = it.response,
                        matchType = it.matchType,
                        enabled = it.enabled
                    )
                }
            }
        }.stateIn(scope, SharingStarted.Eagerly, getDefaultRules())

    init {
        // Initialize local cache with default data if empty
        scope.launch {
            val initial = apiClient.getDefaultCommands()
            db.commandDao().insertAll(initial.map {
                CommandEntity(it.id, it.name, it.description, it.category, it.enabled, it.adminOnly)
            })

            val initialRules = getDefaultRules()
            initialRules.forEach {
                db.autoReplyDao().insertRule(
                    AutoReplyEntity(it.id, it.trigger, it.response, it.matchType, it.enabled)
                )
            }
        }
    }

    suspend fun refreshBotStatus(): NetworkResult<BotDetails> {
        val result = apiClient.getBotStatus()
        if (result is NetworkResult.Success) {
            _botDetails.value = result.data
            // Cache in Room
            db.botDao().insertOrUpdate(
                BotEntity(
                    id = result.data.id,
                    name = result.data.name,
                    phoneNumber = result.data.phoneNumber,
                    state = result.data.state.name,
                    uptimeSeconds = result.data.uptimeSeconds,
                    messagesProcessed = result.data.messagesProcessed,
                    activeChats = result.data.activeChats,
                    batteryLevel = result.data.batteryLevel,
                    isCharging = result.data.isCharging,
                    lastSeenTimestamp = result.data.lastSeenTimestamp
                )
            )
        }
        return result
    }

    suspend fun requestPairingCode(phone: String): NetworkResult<PairingCodeResponse> {
        val res = apiClient.requestPairingCode(phone)
        if (res is NetworkResult.Success) {
            _pairingCodeState.value = res.data
        }
        return res
    }

    suspend fun resetPairing(): NetworkResult<Boolean> {
        _pairingCodeState.value = null
        return apiClient.resetPairing()
    }

    suspend fun checkPairingStatus(token: String? = null): NetworkResult<PairingStatusResponse> {
        val res = apiClient.checkPairingStatus(token)
        if (res is NetworkResult.Success) {
            val st = BotConnectionState.fromString(res.data.state)
            _botDetails.value = _botDetails.value.copy(state = st)
        }
        return res
    }

    suspend fun performAction(action: String): NetworkResult<String> {
        val res = apiClient.performBotAction(action)
        if (res is NetworkResult.Success) {
            when (action.lowercase()) {
                "start" -> _botDetails.value = _botDetails.value.copy(state = BotConnectionState.CONNECTED)
                "stop", "logout", "disconnect" -> _botDetails.value = _botDetails.value.copy(state = BotConnectionState.DISCONNECTED)
                "restart" -> _botDetails.value = _botDetails.value.copy(state = BotConnectionState.INITIALIZING)
            }
        }
        return res
    }

    suspend fun toggleCommand(id: String, enabled: Boolean) {
        db.commandDao().updateCommandState(id, enabled)
        apiClient.toggleCommand(id, enabled)
    }

    suspend fun addAutoReply(trigger: String, response: String, matchType: String) {
        val rule = AutoReplyEntity(
            id = "rule_${UUID.randomUUID().toString().take(8)}",
            trigger = trigger,
            response = response,
            matchType = matchType,
            enabled = true
        )
        db.autoReplyDao().insertRule(rule)
    }

    suspend fun deleteAutoReply(id: String) {
        db.autoReplyDao().deleteById(id)
    }

    suspend fun toggleAutoReply(id: String, enabled: Boolean) {
        db.autoReplyDao().updateRuleEnabled(id, enabled)
    }

    fun updateAiConfig(config: AiConfig) {
        _aiConfig.value = config
    }

    fun updateGroupSettings(settings: GroupSettings) {
        _groupSettings.value = settings
    }

    fun addUnlockedMessages(messagesCount: Long) {
        _usageStats.value = _usageStats.value.copy(
            dailyLimit = _usageStats.value.dailyLimit + messagesCount
        )
    }

    private fun getDefaultRules(): List<AutoReplyRule> {
        return listOf(
            AutoReplyRule("rule_1", "pricing", "Hi! Check out our services at https://edbots.mnz.dom.my.id", "CONTAINS", true),
            AutoReplyRule("rule_2", "hours", "Our support hours are 24/7 automated by EDBOTS WhatsApp AI.", "EXACT", true),
            AutoReplyRule("rule_3", "help", "Type /menu to view all interactive commands.", "STARTS_WITH", true)
        )
    }
}
