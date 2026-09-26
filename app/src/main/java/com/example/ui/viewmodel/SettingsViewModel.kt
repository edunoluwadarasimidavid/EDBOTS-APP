package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.EdBotsPreferences
import com.example.data.model.NetworkResult
import com.example.data.remote.EdBotsApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    val pairingToken: String = "",
    val appwriteEndpoint: String = "",
    val appwriteProjectId: String = "",
    val darkTheme: Boolean = true,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null,
    val isSuccess: Boolean = false
)

class SettingsViewModel(
    private val prefs: EdBotsPreferences,
    private val edbotsClient: EdBotsApiClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            apiBaseUrl = prefs.apiBaseUrl,
            apiKey = prefs.apiKey,
            pairingToken = prefs.pairingToken,
            appwriteEndpoint = prefs.appwriteEndpoint,
            appwriteProjectId = prefs.appwriteProjectId,
            darkTheme = prefs.darkTheme
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateApiBaseUrl(url: String) {
        prefs.apiBaseUrl = url
        _uiState.value = _uiState.value.copy(apiBaseUrl = url)
    }

    fun updateApiKey(key: String) {
        prefs.apiKey = key
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun updatePairingToken(token: String) {
        prefs.pairingToken = token
        _uiState.value = _uiState.value.copy(pairingToken = token)
    }

    fun updateAppwriteEndpoint(endpoint: String) {
        prefs.appwriteEndpoint = endpoint
        _uiState.value = _uiState.value.copy(appwriteEndpoint = endpoint)
    }

    fun updateAppwriteProjectId(id: String) {
        prefs.appwriteProjectId = id
        _uiState.value = _uiState.value.copy(appwriteProjectId = id)
    }

    fun setDarkTheme(enabled: Boolean) {
        prefs.darkTheme = enabled
        _uiState.value = _uiState.value.copy(darkTheme = enabled)
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionTestResult = null)
            val result = edbotsClient.checkPairingStatus()
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        connectionTestResult = "Connection successful! Server is online and responding.",
                        isSuccess = true
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        connectionTestResult = result.message,
                        isSuccess = false
                    )
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearTestResult() {
        _uiState.value = _uiState.value.copy(connectionTestResult = null)
    }

    class Factory(
        private val prefs: EdBotsPreferences,
        private val edbotsClient: EdBotsApiClient
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(prefs, edbotsClient) as T
        }
    }
}
