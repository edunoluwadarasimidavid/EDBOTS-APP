package com.example.data.remote

import com.example.data.local.EdBotsPreferences
import com.example.data.model.AppwriteSession
import com.example.data.model.AppwriteUser
import com.example.data.model.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class AppwriteApiClient(private val prefs: EdBotsPreferences) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildRequest(path: String, method: String = "GET", body: String? = null): Request {
        val url = "${prefs.appwriteEndpoint}$path"
        val builder = Request.Builder()
            .url(url)
            .addHeader("X-Appwrite-Project", prefs.appwriteProjectId)
            .addHeader("X-Appwrite-Response-Format", "1.6.0")
            .addHeader("Content-Type", "application/json")

        if (prefs.authToken.isNotBlank()) {
            builder.addHeader("X-Appwrite-Session", prefs.authToken)
        }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post((body ?: "{}").toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch((body ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete((body ?: "").toRequestBody(jsonMediaType))
        }

        return builder.build()
    }

    suspend fun register(name: String, email: String, pass: String): NetworkResult<AppwriteUser> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("userId", "user_${UUID.randomUUID().toString().replace("-", "").take(16)}")
                put("name", name)
                put("email", email)
                put("password", pass)
            }

            val request = buildRequest("/account", "POST", json.toString())
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val obj = JSONObject(responseBody)
                val user = AppwriteUser(
                    id = obj.optString("\$id", UUID.randomUUID().toString()),
                    name = obj.optString("name", name),
                    email = obj.optString("email", email),
                    registration = if (obj.has("registration")) obj.optString("registration") else null,
                    status = obj.optBoolean("status", true)
                )
                NetworkResult.Success(user)
            } else {
                val errMsg = parseErrorMessage(responseBody, "Registration failed (${response.code})")
                NetworkResult.Error(errMsg, response.code)
            }
        } catch (e: IOException) {
            NetworkResult.Error("Network error: Unable to connect to Appwrite. Please check your internet connection.")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "An unexpected error occurred during registration.")
        }
    }

    suspend fun login(email: String, pass: String): NetworkResult<AppwriteSession> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }

            val request = buildRequest("/account/sessions/email", "POST", json.toString())
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val obj = JSONObject(responseBody)
                val session = AppwriteSession(
                    id = obj.optString("\$id", ""),
                    userId = obj.optString("userId", ""),
                    expire = if (obj.has("expire")) obj.optString("expire") else null,
                    secret = obj.optString("secret", obj.optString("\$id", ""))
                )
                NetworkResult.Success(session)
            } else {
                val errMsg = parseErrorMessage(responseBody, "Invalid email or password (${response.code})")
                NetworkResult.Error(errMsg, response.code)
            }
        } catch (e: IOException) {
            NetworkResult.Error("Network error: Unable to reach Appwrite server.")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Authentication failed.")
        }
    }

    suspend fun getCurrentAccount(): NetworkResult<AppwriteUser> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/account", "GET")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val obj = JSONObject(responseBody)
                val user = AppwriteUser(
                    id = obj.optString("\$id", ""),
                    name = obj.optString("name", "User"),
                    email = obj.optString("email", ""),
                    registration = if (obj.has("registration")) obj.optString("registration") else null,
                    status = obj.optBoolean("status", true)
                )
                NetworkResult.Success(user)
            } else {
                NetworkResult.Error(parseErrorMessage(responseBody, "Session expired or invalid"), response.code)
            }
        } catch (e: IOException) {
            NetworkResult.Error("Network error: Unable to verify account.")
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Failed to retrieve account details.")
        }
    }

    suspend fun logout(): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("/account/sessions/current", "DELETE")
            val response = client.newCall(request).execute()
            NetworkResult.Success(response.isSuccessful)
        } catch (e: Exception) {
            NetworkResult.Success(true) // Treat as logged out locally
        }
    }

    suspend fun updatePreferences(prefsMap: Map<String, Any>): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            val prefsObj = JSONObject()
            prefsMap.forEach { (k, v) -> prefsObj.put(k, v) }
            json.put("prefs", prefsObj)

            val request = buildRequest("/account/prefs", "PATCH", json.toString())
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                NetworkResult.Success(true)
            } else {
                NetworkResult.Error("Failed to sync preferences with Appwrite", response.code)
            }
        } catch (e: Exception) {
            NetworkResult.Error("Could not sync preferences: ${e.message}")
        }
    }

    private fun parseErrorMessage(jsonStr: String, fallback: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            obj.optString("message", fallback)
        } catch (_: Exception) {
            fallback
        }
    }
}
