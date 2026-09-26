package com.example.data.repository

import com.example.data.local.EdBotsPreferences
import com.example.data.model.AppwriteUser
import com.example.data.model.NetworkResult
import com.example.data.remote.AppwriteApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(
    private val appwriteClient: AppwriteApiClient,
    private val prefs: EdBotsPreferences
) {
    private val _currentUser = MutableStateFlow<AppwriteUser?>(
        if (prefs.isUserLoggedIn()) {
            AppwriteUser(
                id = prefs.userId.ifBlank { "user_default" },
                name = prefs.userName.ifBlank { "User" },
                email = prefs.userEmail.ifBlank { "user@edbots.io" }
            )
        } else null
    )
    val currentUser: StateFlow<AppwriteUser?> = _currentUser.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = prefs.isLoggedIn

    suspend fun register(name: String, email: String, pass: String): NetworkResult<AppwriteUser> {
        val res = appwriteClient.register(name, email, pass)
        if (res is NetworkResult.Success) {
            // Auto login after registration
            val loginRes = appwriteClient.login(email, pass)
            if (loginRes is NetworkResult.Success) {
                prefs.saveAuthSession(
                    token = loginRes.data.secret ?: loginRes.data.id,
                    userId = res.data.id,
                    name = res.data.name,
                    email = res.data.email
                )
                _currentUser.value = res.data
            } else {
                prefs.saveAuthSession("local_session", res.data.id, res.data.name, res.data.email)
                _currentUser.value = res.data
            }
        }
        return res
    }

    suspend fun login(email: String, pass: String): NetworkResult<AppwriteUser> {
        val sessionRes = appwriteClient.login(email, pass)
        return when (sessionRes) {
            is NetworkResult.Success -> {
                val secret = sessionRes.data.secret ?: sessionRes.data.id
                prefs.authToken = secret
                // Fetch full account info
                val userRes = appwriteClient.getCurrentAccount()
                val user = if (userRes is NetworkResult.Success) {
                    userRes.data
                } else {
                    AppwriteUser(
                        id = sessionRes.data.userId,
                        name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = email
                    )
                }
                prefs.saveAuthSession(secret, user.id, user.name, user.email)
                _currentUser.value = user
                NetworkResult.Success(user)
            }
            is NetworkResult.Error -> {
                NetworkResult.Error(sessionRes.message, sessionRes.code)
            }
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    suspend fun refreshUser(): NetworkResult<AppwriteUser> {
        if (!prefs.isUserLoggedIn()) return NetworkResult.Error("Not authenticated")
        val res = appwriteClient.getCurrentAccount()
        if (res is NetworkResult.Success) {
            _currentUser.value = res.data
            prefs.userName = res.data.name
            prefs.userEmail = res.data.email
        }
        return res
    }

    suspend fun logout(): NetworkResult<Boolean> {
        val res = appwriteClient.logout()
        prefs.clearAuthSession()
        _currentUser.value = null
        return res
    }

    suspend fun syncPreferences(data: Map<String, Any>): NetworkResult<Boolean> {
        return appwriteClient.updatePreferences(data)
    }
}
