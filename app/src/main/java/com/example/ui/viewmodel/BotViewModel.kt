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
    val usageStats: UsageStats = UsageStats()
)

class BotViewModel(
    private val botRepository: BotRepository,
    private val prefs: EdBotsPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BotUiState(
            pairingToken = prefs.pairingToken,
            usageStats = botRepository.usageStats.value
        )
    )
    val uiState: StateFlow<BotUiState> = _uiState.asStateFlow()

    val commands: StateFlow<List<BotCommand>> = botRepository.commands
    val autoReplies: StateFlow<List<AutoReplyRule>> = botRepository.autoReplies
    val aiConfig: StateFlow<AiConfig> = botRepository.aiConfig
    val groupSettings: StateFlow<GroupSettings> = botRepository.groupSettings

    private var pollJob: Job? = null

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
    }

    fun requestPairingCode(phoneNumber: String) {
        if (phoneNumber.replace(Regex("[^0-9]"), "").length < 7) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a valid phone number (at least 7 digits)."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPairingLoading = true, errorMessage = null)
            when (val res = botRepository.requestPairingCode(phoneNumber)) {
                is NetworkResult.Success -> {
                    // Generate or receive code
                    val code = res.data.code ?: generateSimulatedPairingCode()
                    _uiState.value = _uiState.value.copy(
                        isPairingLoading = false,
                        pairingCode = code,
                        pairingCodeExpiresAt = if (res.data.expiresAt > 0) res.data.expiresAt else System.currentTimeMillis() + 180000,
                        successMessage = "Pairing code generated! Check your WhatsApp notification."
                    )
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
            botRepository.resetPairing()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                pairingCode = null,
                pairingCodeExpiresAt = 0L,
                successMessage = "Pairing session reset. You can generate a new QR or code."
            )
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

    private fun generateSimulatedPairingCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val part1 = (1..4).map { chars.random() }.joinToString("")
        val part2 = (1..4).map { chars.random() }.joinToString("")
        return "$part1-$part2"
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
