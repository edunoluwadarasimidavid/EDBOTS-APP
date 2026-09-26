package com.example.data.remote

import com.example.data.local.EdBotsPreferences
import com.example.data.model.AppwriteSession
import com.example.data.model.AppwriteUser
import com.example.data.model.NetworkResult
import com.example.data.model.PremiumStatus
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

    // ── Databases: documents CRUD (premium/backend integration) ─────────────────

    /**
     * Appwrite Databases documents endpoints. Requires databaseId + collectionId
     * (Settings → Appwrite, or APPWRITE_DATABASE_ID / APPWRITE_COLLECTION_ID).
     * Calls are authenticated with the user session (X-Appwrite-Session), so the
     * collection's permissions must grant access to the `users` team.
     */
    private fun requireDatabaseIds(): Pair<String, String>? {
        val dbId = prefs.appwriteDatabaseId
        val colId = prefs.appwriteCollectionId
        return if (dbId.isNotBlank() && colId.isNotBlank()) dbId to colId else null
    }

    private fun documentPath(documentId: String? = null): String {
        val (dbId, colId) = requireDatabaseIds()!!
        val base = "/databases/$dbId/collections/$colId/documents"
        return if (documentId != null) "$base/$documentId" else base
    }

    /**
     * Lists documents in the configured collection, optionally filtered with
     * Appwrite query strings (e.g. "equal(\"userId\",\"<uid>\")").
     */
    suspend fun listDocuments(queries: List<String>? = null): NetworkResult<org.json.JSONArray> =
        withContext(Dispatchers.IO) {
            try {
                if (requireDatabaseIds() == null) {
                    return@withContext NetworkResult.Error(
                        "Appwrite database/collection IDs are not configured. Set them in Settings.",
                        400
                    )
                }
                var url = documentPath()
                if (!queries.isNullOrEmpty()) {
                    val encoded = java.net.URLEncoder.encode(
                        org.json.JSONArray(queries).toString(), "UTF-8"
                    )
                    url += "?queries[]=$encoded"
                }
                val response = client.newCall(buildRequest(url, "GET")).execute()
                response.body?.string().orEmpty().let { bodyStr ->
                    if (!response.isSuccessful) {
                        return@withContext NetworkResult.Error(
                            parseErrorMessage(bodyStr, "Failed to list documents (${response.code})"),
                            response.code
                        )
                    }
                    val obj = JSONObject(bodyStr)
                    NetworkResult.Success(obj.optJSONArray("documents") ?: org.json.JSONArray())
                }
            } catch (e: Exception) {
                NetworkResult.Error("Failed to load documents: ${e.message}")
            }
        }

    /** Creates a document. documentId "unique()" lets Appwrite generate the ID. */
    suspend fun createDocument(data: JSONObject, documentId: String = "unique()"): NetworkResult<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                if (requireDatabaseIds() == null) {
                    return@withContext NetworkResult.Error(
                        "Appwrite database/collection IDs are not configured. Set them in Settings.",
                        400
                    )
                }
                val body = JSONObject().put("documentId", documentId).put("data", data)
                val response = client.newCall(buildRequest(documentPath(), "POST", body.toString())).execute()
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Error(
                        parseErrorMessage(bodyStr, "Failed to create document (${response.code})"),
                        response.code
                    )
                }
                NetworkResult.Success(JSONObject(bodyStr))
            } catch (e: Exception) {
                NetworkResult.Error("Failed to create document: ${e.message}")
            }
        }

    /** Fetches one document by ID. */
    suspend fun getDocument(documentId: String): NetworkResult<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                if (requireDatabaseIds() == null) {
                    return@withContext NetworkResult.Error(
                        "Appwrite database/collection IDs are not configured. Set them in Settings.",
                        400
                    )
                }
                val response = client.newCall(buildRequest(documentPath(documentId), "GET")).execute()
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Error(
                        parseErrorMessage(bodyStr, "Failed to read document (${response.code})"),
                        response.code
                    )
                }
                NetworkResult.Success(JSONObject(bodyStr))
            } catch (e: Exception) {
                NetworkResult.Error("Failed to read document: ${e.message}")
            }
        }

    /** Deletes one document by ID. */
    suspend fun deleteDocument(documentId: String): NetworkResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (requireDatabaseIds() == null) {
                    return@withContext NetworkResult.Error(
                        "Appwrite database/collection IDs are not configured. Set them in Settings.",
                        400
                    )
                }
                val response = client.newCall(buildRequest(documentPath(documentId), "DELETE")).execute()
                if (!response.isSuccessful) {
                    return@withContext NetworkResult.Error(
                        "Failed to delete document (${response.code})",
                        response.code
                    )
                }
                NetworkResult.Success(true)
            } catch (e: Exception) {
                NetworkResult.Error("Failed to delete document: ${e.message}")
            }
        }

    /**
     * Looks up the signed-in user's premium membership document in the configured
     * collection (expects attributes: userId, plan/tier, expiresAt/active flags —
     * the exact attribute names stay flexible).
     */
    suspend fun getPremiumStatus(userId: String): NetworkResult<PremiumStatus> =
        withContext(Dispatchers.IO) {
            when (val res = listDocuments(listOf("equal(\"userId\",\"$userId\")"))) {
                is NetworkResult.Success -> {
                    val docs = res.data
                    if (docs.length() == 0) {
                        NetworkResult.Success(PremiumStatus(isPremium = false))
                    } else {
                        val doc = docs.optJSONObject(0) ?: JSONObject()
                        val tier = doc.optString("plan", doc.optString("tier", "Free")).ifBlank { "Free" }
                        val expiresRaw = doc.optString("expiresAt", "")
                        val expires = if (expiresRaw.isBlank()) null else runCatching {
                            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }.parse(expiresRaw)?.time
                        }.getOrNull()
                        val notExpired = expires == null || expires > System.currentTimeMillis()
                        NetworkResult.Success(
                            PremiumStatus(
                                isPremium = notExpired && tier.isNotBlank() && !tier.equals("Free", ignoreCase = true),
                                tier = tier,
                                expiresAt = expires
                            )
                        )
                    }
                }
                is NetworkResult.Error -> res
                NetworkResult.Loading -> NetworkResult.Loading
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
