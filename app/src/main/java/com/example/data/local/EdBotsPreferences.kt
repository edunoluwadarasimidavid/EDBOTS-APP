package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EdBotsPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("edbots_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PAIRING_TOKEN = "pairing_token"
        private const val KEY_APPWRITE_ENDPOINT = "appwrite_endpoint"
        private const val KEY_APPWRITE_PROJECT_ID = "appwrite_project_id"
        private const val KEY_DARK_THEME = "dark_theme"

        const val DEFAULT_API_BASE_URL = "https://edbots.mnz.dom.my.id"
        const val DEFAULT_APPWRITE_ENDPOINT = "https://cloud.appwrite.io/v1"
        const val DEFAULT_APPWRITE_PROJECT_ID = "edbots_app"
    }

    private val _isLoggedIn = MutableStateFlow(isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun isUserLoggedIn(): Boolean {
        return !prefs.getString(KEY_AUTH_TOKEN, "").isNullOrBlank() ||
                !prefs.getString(KEY_USER_ID, "").isNullOrBlank()
    }

    fun saveAuthSession(token: String, userId: String, name: String, email: String) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .apply()
        _isLoggedIn.value = true
    }

    fun clearAuthSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .apply()
        _isLoggedIn.value = false
    }

    var authToken: String
        get() = prefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
            _isLoggedIn.value = isUserLoggedIn()
        }

    var userId: String
        get() = prefs.getString(KEY_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "User") ?: "User"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
        set(value) = prefs.edit().putString(KEY_API_BASE_URL, value.trimEnd('/')).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var pairingToken: String
        get() = prefs.getString(KEY_PAIRING_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PAIRING_TOKEN, value.trim()).apply()

    var appwriteEndpoint: String
        get() = prefs.getString(KEY_APPWRITE_ENDPOINT, DEFAULT_APPWRITE_ENDPOINT) ?: DEFAULT_APPWRITE_ENDPOINT
        set(value) = prefs.edit().putString(KEY_APPWRITE_ENDPOINT, value.trimEnd('/')).apply()

    var appwriteProjectId: String
        get() = prefs.getString(KEY_APPWRITE_PROJECT_ID, DEFAULT_APPWRITE_PROJECT_ID) ?: DEFAULT_APPWRITE_PROJECT_ID
        set(value) = prefs.edit().putString(KEY_APPWRITE_PROJECT_ID, value.trim()).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()
}
