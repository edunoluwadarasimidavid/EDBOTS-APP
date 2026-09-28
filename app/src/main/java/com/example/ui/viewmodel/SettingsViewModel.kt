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
import org.json.JSONObject

data class SettingsUiState(
    val apiBaseUrl: String = "",
    val apiKey: String = "",
    val pairingToken: String = "",
    val appwriteEndpoint: String = "",
    val appwriteProjectId: String = "",
    val appwriteDatabaseId: String = "",
    val appwriteCollectionId: String = "",
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
            appwriteDatabaseId = prefs.appwriteDatabaseId,
            appwriteCollectionId = prefs.appwriteCollectionId,
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

    fun updateAppwriteDatabaseId(id: String) {
        prefs.appwriteDatabaseId = id
        _uiState.value = _uiState.value.copy(appwriteDatabaseId = id)
    }

    fun updateAppwriteCollectionId(id: String) {
        prefs.appwriteCollectionId = id
        _uiState.value = _uiState.value.copy(appwriteCollectionId = id)
    }

    fun setDarkTheme(enabled: Boolean) {
        prefs.darkTheme = enabled
        _uiState.value = _uiState.value.copy(darkTheme = enabled)
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionTestResult = null)
            // /api/health is public — it verifies the server is reachable without needing a key.
            val result = edbotsClient.getHealth()
            when (result) {
                is NetworkResult.Success<*> -> {
                    val body = result.data as JSONObject
                    val version = body.optString("version", "?")
                    val botVersion = body.optString("botVersion", "?")
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        connectionTestResult = "Connected! EDBOTS API v$version (bot v$botVersion) is online.",
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
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return SettingsViewModel(prefs, edbotsClient) as T
        }
    }
}
