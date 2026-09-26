package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.EdBotsPreferences
import com.example.data.model.*
import com.example.data.repository.BotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BotUiState(
    val botDetails: BotDetails = BotDetails(),
    val isLoading: Boolean = false,
    val isPairingLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val pairingCode: String? = null,
    val pairingCodeExpiresAt: Long = 0L,
    val pairingToken: String = "",
    val qrState: String = "INITIALIZING",
    /** WhatsApp chat JID (e.g. 2348012345678@s.whatsapp.net) that new auto-reply rules target. */
    val activeChatId: String = "",
    val qrImage: String? = null,
    val groups: List<GroupSummary> = emptyList(),
    val usageStats: UsageStats = UsageStats()
)

class BotViewModel(
    private val botRepository: BotRepository,
    private val prefs: EdBotsPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BotUiState(
            pairingToken = prefs.pairingToken,
            activeChatId = botRepository.activeChatId.value,
            usageStats = botRepository.usageStats.value
        )
    )
    val uiState: StateFlow<BotUiState> = _uiState.asStateFlow()

    val commands: StateFlow<List<BotCommand>> = botRepository.commands
    val autoReplies: StateFlow<List<AutoReplyRule>> = botRepository.autoReplies
    val aiConfig: StateFlow<AiConfig> = botRepository.aiConfig
    val groupSettings: StateFlow<GroupSettings> = botRepository.groupSettings

    private var pollJob: Job? = null
    private var pairingPollJob: Job? = null

    init {
        viewModelScope.launch {
            botRepository.botDetails.collect { details ->
                _uiState.value = _uiState.value.copy(botDetails = details)
            }
        }
        viewModelScope.launch {
            botRepository.usageStats.collect { stats ->
                _uiState.value = _uiState.value.copy(usageStats = stats)
            }
        }
        refreshBotStatus()
    }

    fun refreshBotStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = botRepository.refreshBotStatus()
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        botDetails = result.data
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun performBotAction(action: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val res = botRepository.performAction(action)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = res.data
                    )
                    refreshBotStatus()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = res.message
                    )
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun setPairingToken(token: String) {
        prefs.pairingToken = token
        _uiState.value = _uiState.value.copy(pairingToken = token)
        // Immediately check pairing state with the new token.
        checkPairingStatus()
    }

    /** One-shot GET /pair/status: refreshes QR image, pairing code, and auth state. */
    fun checkPairingStatus() {
        viewModelScope.launch {
            when (val res = botRepository.checkPairingStatus()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        qrState = res.data.state ?: "INITIALIZING",
                        qrImage = res.data.qrImage,
                        pairingCode = res.data.pairingCode,
                        pairingCodeExpiresAt = res.data.pairingCodeExpiresAt
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    /** Starts a polling loop against /pair/status (docs recommend polling over SSE on mobile). */
    private fun startPairingPolling() {
        pairingPollJob?.cancel()
        pairingPollJob = viewModelScope.launch {
            val deadline = System.currentTimeMillis() + 5 * 60_000L
            while (System.currentTimeMillis() < deadline) {
                when (val res = botRepository.checkPairingStatus()) {
                    is NetworkResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            qrState = res.data.state ?: "INITIALIZING",
                            qrImage = res.data.qrImage,
                            pairingCode = res.data.pairingCode,
                            pairingCodeExpiresAt = res.data.pairingCodeExpiresAt,
                            isPairingLoading = false
                        )
                        val state = res.data.state
                        // Stop polling once linked, logged out, or failed.
                        if (state == "CONNECTED" || state == "LOGGED_OUT" || state == "FAILED") break
                    }
                    is NetworkResult.Error -> {
                        _uiState.value = _uiState.value.copy(isPairingLoading = false, errorMessage = res.message)
                        break
                    }
                    NetworkResult.Loading -> Unit
                }
                delay(5000)
                refreshBotStatus()
            }
        }
    }

    fun setActiveChatId(chatId: String) {
        botRepository.setActiveChatId(chatId)
        _uiState.value = _uiState.value.copy(activeChatId = chatId.trim())
    }

    fun fetchGroups() {
        viewModelScope.launch {
            when (val res = botRepository.fetchGroups()) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(groups = res.data)
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(errorMessage = res.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun loadGroupSettings(groupId: String) {
        viewModelScope.launch {
            when (val res = botRepository.loadGroupSettings(groupId)) {
                is NetworkResult.Error ->
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                is NetworkResult.Success -> Unit
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun saveGroupSettings(groupId: String, settings: GroupSettings) {
        viewModelScope.launch {
            when (val res = botRepository.saveGroupSettings(groupId, settings)) {
                is NetworkResult.Success ->
                    _uiState.value = _uiState.value.copy(successMessage = "Group settings saved to server.")
                is NetworkResult.Error ->
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun requestPairingCode(phoneNumber: String) {
        if (prefs.pairingToken.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Pairing token required. Paste the token from your EDBOTS server console first."
            )
            return
        }
        if (phoneNumber.replace(Regex("[^0-9]"), "").length < 8) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a valid phone number (8–15 digits with country code)."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPairingLoading = true, errorMessage = null)
            when (val res = botRepository.requestPairingCode(phoneNumber)) {
                is NetworkResult.Success -> {
                    // The server returns 202 with NO code — the real code arrives
                    // via /pair/status polling (auth.pairingCode) a few seconds later.
                    _uiState.value = _uiState.value.copy(
                        isPairingLoading = false,
                        successMessage = "Pairing code requested! The code appears on this screen in a few seconds."
                    )
                    startPairingPolling()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isPairingLoading = false,
                        errorMessage = res.message
                    )
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun resetPairing() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            pairingPollJob?.cancel()
            when (val res = botRepository.resetPairing()) {
                is NetworkResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pairingCode = null,
                    qrImage = null,
                    pairingCodeExpiresAt = 0L,
                    qrState = "WAITING_FOR_AUTH",
                    successMessage = "Pairing session reset. You can generate a new QR or code."
                )
                is NetworkResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = res.message
                )
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun toggleCommand(id: String, enabled: Boolean) {
        viewModelScope.launch {
            botRepository.toggleCommand(id, enabled)
        }
    }

    fun addAutoReply(trigger: String, response: String, matchType: String) {
        if (trigger.isBlank() || response.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Trigger and response cannot be blank.")
            return
        }
        viewModelScope.launch {
            botRepository.addAutoReply(trigger.trim(), response.trim(), matchType)
            _uiState.value = _uiState.value.copy(successMessage = "Auto-reply rule added successfully.")
        }
    }

    fun deleteAutoReply(id: String) {
        viewModelScope.launch {
            botRepository.deleteAutoReply(id)
            _uiState.value = _uiState.value.copy(successMessage = "Rule removed.")
        }
    }

    fun toggleAutoReply(id: String, enabled: Boolean) {
        viewModelScope.launch {
            botRepository.toggleAutoReply(id, enabled)
        }
    }

    fun updateAiConfig(config: AiConfig) {
        viewModelScope.launch {
            botRepository.updateAiConfig(config)
            _uiState.value = _uiState.value.copy(successMessage = "AI settings saved.")
        }
    }

    fun updateGroupSettings(settings: GroupSettings) {
        viewModelScope.launch {
            botRepository.updateGroupSettings(settings)
            _uiState.value = _uiState.value.copy(successMessage = "Group settings saved.")
        }
    }

    fun refreshAiConfig() {
        viewModelScope.launch { botRepository.refreshAiConfig() }
    }

    fun watchBonusAd() {
        // Simulates rewarded advertisement bonus (future monetization compatible)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            delay(1000)
            botRepository.addUnlockedMessages(100)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = "Reward granted! +100 daily WhatsApp messages unlocked."
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    override fun onCleared() {
        pairingPollJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val botRepository: BotRepository,
        private val prefs: EdBotsPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BotViewModel(botRepository, prefs) as T
        }
    }
}
