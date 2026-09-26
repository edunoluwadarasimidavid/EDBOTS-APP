package com.example.data.model

data class AppwriteUser(
    val id: String,
    val name: String,
    val email: String,
    val registration: String? = null,
    val status: Boolean = true,
    val prefs: Map<String, Any?> = emptyMap()
)

data class AppwriteSession(
    val id: String,
    val userId: String,
    val expire: String? = null,
    val secret: String? = null
)

data class AppwriteError(
    val message: String,
    val code: Int = 0,
    val type: String? = null
)

/** Premium membership state resolved from the Appwrite premium collection. */
data class PremiumStatus(
    val isPremium: Boolean = false,
    val tier: String = "Free",
    val expiresAt: Long? = null
)
