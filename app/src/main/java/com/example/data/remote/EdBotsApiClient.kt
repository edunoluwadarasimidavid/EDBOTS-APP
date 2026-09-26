package com.example.data.remote

import com.example.data.local.EdBotsPreferences
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Client for the EDBOTS bot REST API (docs/API.md, v1.0.0, base path /api).
 *
 * Auth model:
 *  - /api/* endpoints require an API key via X-API-Key or Authorization: Bearer.
 *  - /pair/* endpoints require a one-time pairing token via X-Pairing-Token
 *    (mobile apps cannot rely on the HttpOnly session cookie).
 *
 * Response envelope: { "ok": true, ...payload } or
 * { "ok": false, "error": { "code": "...", "message": "...", "details": [...] } }.
 */
class EdBotsApiClient(private val prefs: EdBotsPreferences) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ── Low-level helpers ───────────────────────────────────────────────────────

    private fun buildRequest(
        path: String,
        method: String = "GET",
        body: String? = null,
        usePairingToken: Boolean = false
    ): Request {
        val baseUrl = prefs.apiBaseUrl.trimEnd('/')
        // API surface lives under /api; pairing is a top-level surface.
        val url = if (path.startsWith("http")) {
            path
        } else if (path.startsWith("/pair")) {
            "$baseUrl$path"
        } else {
            "$baseUrl/api$path"
        }

        val builder = Request.Builder().url(url)
        builder.addHeader("Accept", "application/json")
        builder.addHeader("User-Agent", "EDBOTS-Android/1.0")

        if (usePairingToken) {
            if (prefs.pairingToken.isNotBlank()) {
                builder.addHeader("X-Pairing-Token", prefs.pairingToken)
            }
        } else if (prefs.apiKey.isNotBlank()) {
            builder.addHeader("X-API-Key", prefs.apiKey)
            builder.addHeader("Authorization", "Bearer ${prefs.apiKey}")
        }

        when (method.uppercase()) {
            "GET" -> builder.get()
            "POST" -> builder.post((body ?: "{}").toRequestBody(jsonMediaType))
            "PATCH" -> builder.patch((body ?: "{}").toRequestBody(jsonMediaType))
            "DELETE" -> builder.delete((body ?: "{}").toRequestBody(jsonMediaType))
        }

        return builder.build()
    }

    /** Executes a call and returns the raw body + success flag. */
    private data class RawResponse(val code: Int, val body: String, val isSuccessful: Boolean)

    private fun execute(req: Request): RawResponse {
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            return RawResponse(resp.code, body, resp.isSuccessful)
        }
    }

    /** Extracts the human message from the standard error envelope. */
    private fun parseErrorMessage(jsonStr: String, fallback: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            val errObj = obj.optJSONObject("error")
            errObj?.optString("message")?.takeIf { it.isNotBlank() }
                ?: obj.optString("message", fallback).ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun errorResult(code: Int, body: String, fallback: String): NetworkResult.Error =
        NetworkResult.Error(parseErrorMessage(body, fallback), code)

    private fun <T> handleException(e: Exception): NetworkResult<T> {
        val message = when (e) {
            is UnknownHostException -> "No internet connection or cannot resolve host: ${prefs.apiBaseUrl}."
            is ConnectException -> "Unable to connect to EDBOTS server. Server may be offline."
            is SocketTimeoutException -> "Connection timed out while communicating with EDBOTS server."
            is IOException -> "Network I/O error occurred: ${e.message}"
            else -> "Unexpected error: ${e.message}"
        }
        return NetworkResult.Error(message)
    }

    // ── Public API surface (docs/API.md §7) ─────────────────────────────────────

    /** GET /api/health — public liveness probe; the only endpoint needing no key. */
    suspend fun getHealth(): NetworkResult<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/health", "GET"))
            if (!resp.isSuccessful) {
                return@withContext NetworkResult.Error(
                    parseErrorMessage(resp.body, "Health check failed (${resp.code})"),
                    resp.code
                )
            }
            NetworkResult.Success(JSONObject(resp.body))
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** GET /api/status — bot online/offline status. Auth: read. */
    suspend fun getBotStatus(): NetworkResult<BotDetails> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/status", "GET"))
            if (!resp.isSuccessful) {
                if (resp.code == 401) {
                    return@withContext NetworkResult.Error(
                        "API key required or invalid. Configure your EDBOTS API key in Settings.",
                        401
                    )
                }
                return@withContext errorResult(resp.code, resp.body, "Failed to read bot status (${resp.code})")
            }

            val obj = JSONObject(resp.body)
            val bot = obj.optJSONObject("bot") ?: JSONObject()
            val statusStr = bot.optString("status", "offline")
            val user = bot.optJSONObject("user")

            val details = BotDetails(
                id = "edbot_master",
                name = user?.optString("name")?.takeIf { it.isNotBlank() } ?: "EDBOTS Bot",
                phoneNumber = user?.optString("id")
                    ?.takeIf { it.isNotBlank() }?.substringBefore('@'),
                state = BotConnectionState.fromString(statusStr),
                uptimeSeconds = 0L, // Real uptime comes from GET /api/stats
                messagesProcessed = 0L,
                activeChats = 0,
                batteryLevel = 0,
                isCharging = false,
                lastSeenTimestamp = System.currentTimeMillis(),
                lastDisconnectReason = bot.optString("lastDisconnectReason", "").ifBlank { null }
            )
            NetworkResult.Success(details)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** GET /api/stats — aggregated counters (uptime, groups, users). Auth: read. */
    suspend fun getBotStats(): NetworkResult<BotDetails> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/stats", "GET"))
            if (!resp.isSuccessful) return@withContext NetworkResult.Error(
                parseErrorMessage(resp.body, "Failed to read bot stats (${resp.code})"),
                resp.code
            )

            val obj = JSONObject(resp.body)
            val bot = obj.optJSONObject("bot") ?: JSONObject()
            val groups = obj.optJSONObject("groups") ?: JSONObject()

            NetworkResult.Success(
                BotDetails(
                    id = "edbot_master",
                    name = "EDBOTS Bot",
                    phoneNumber = null,
                    state = BotConnectionState.fromString(bot.optString("status", "offline")),
                    uptimeSeconds = bot.optLong("uptimeSec", 0L),
                    messagesProcessed = groups.optLong("totalMessagesTracked", 0L),
                    activeChats = groups.optInt("activeToday", 0),
                    batteryLevel = 0,
                    isCharging = false,
                    lastSeenTimestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** POST /api/bot/start — reconnect using the stored session. Auth: write. Returns 202 while connecting. */
    suspend fun startBot(): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/bot/start", "POST", "{}"))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to start bot (${resp.code})")
            }
            val obj = try { JSONObject(resp.body) } catch (_: Exception) { JSONObject() }
            NetworkResult.Success(obj.optString("message", "Start requested. Poll status for the connection result."))
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** POST /api/bot/stop — stops the bot, preserving the WhatsApp session. Auth: write. */
    suspend fun stopBot(): NetworkResult<String> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/bot/stop", "POST", "{}"))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to stop bot (${resp.code})")
            }
            val obj = try { JSONObject(resp.body) } catch (_: Exception) { JSONObject() }
            NetworkResult.Success(obj.optString("message", "Bot stopped. The WhatsApp session was preserved."))
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** GET /api/settings — full bot settings object. Auth: read. */
    suspend fun getSettings(): NetworkResult<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/settings", "GET"))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to read settings (${resp.code})")
            }
            val obj = JSONObject(resp.body)
            val settings = obj.optJSONObject("settings") ?: JSONObject()
            NetworkResult.Success(settings)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** PATCH /api/settings — update only the fields provided. Auth: write. */
    suspend fun updateSettings(patch: JSONObject): NetworkResult<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/settings", "PATCH", patch.toString()))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to update settings (${resp.code})")
            }
            val obj = JSONObject(resp.body)
            NetworkResult.Success(obj.optJSONObject("settings") ?: JSONObject())
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** GET /api/commands — canonical command list with global enabled flags. Auth: read. */
    suspend fun fetchCommands(): NetworkResult<List<BotCommand>> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/commands", "GET"))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to read commands (${resp.code})")
            }
            val obj = JSONObject(resp.body)
            val arr = obj.optJSONArray("commands") ?: JSONArray()
            val list = mutableListOf<BotCommand>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(
                    BotCommand(
                        id = item.optString("name", "cmd_$i"),
                        name = item.optString("name", ""),
                        description = item.optString("description", ""),
                        category = item.optString("category", "general").replaceFirstChar { it.uppercase() },
                        enabled = item.optBoolean("enabled", true),
                        adminOnly = item.optBoolean("adminOnly", false)
                    )
                )
            }
            NetworkResult.Success(list)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** POST /api/commands/{name}/enable|disable — global command toggle. Auth: write. */
    suspend fun setCommandEnabled(name: String, enabled: Boolean): NetworkResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val path = "/commands/$name/${if (enabled) "enable" else "disable"}"
                val resp = execute(buildRequest(path, "POST", "{}"))
                if (!resp.isSuccessful) {
                    return@withContext errorResult(resp.code, resp.body, "Failed to update command (${resp.code})")
                }
                NetworkResult.Success(true)
            } catch (e: Exception) {
                handleException(e)
            }
        }

    /** GET /api/autoreply — global switch + per-chat configs. Auth: read. */
    suspend fun getAutoReplyGlobal(): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/autoreply", "GET"))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to read auto-reply (${resp.code})")
            }
            val obj = JSONObject(resp.body)
            NetworkResult.Success(obj.optBoolean("globalEnabled", false))
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** PATCH /api/autoreply — global master switch. Auth: write. */
    suspend fun setAutoReplyGlobal(enabled: Boolean): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("enabled", enabled)
            val resp = execute(buildRequest("/autoreply", "PATCH", body.toString()))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to update auto-reply (${resp.code})")
            }
            NetworkResult.Success(true)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /**
     * POST /api/autoreply/chat/{chatId}/keywords — add a keyword rule. Auth: write.
     * chatId must be a WhatsApp JID, e.g. "2348012345678@s.whatsapp.net".
     */
    suspend fun addAutoReplyKeyword(
        chatId: String,
        keyword: String,
        response: String,
        caseSensitive: Boolean = false
    ): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("keyword", keyword)
                .put("response", response)
                .put("caseSensitive", caseSensitive)
            val path = "/autoreply/chat/${java.net.URLEncoder.encode(chatId, "UTF-8")}/keywords"
            val resp = execute(buildRequest(path, "POST", body.toString()))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to add keyword rule (${resp.code})")
            }
            NetworkResult.Success(true)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** DELETE /api/autoreply/chat/{chatId}/keywords/{keyword}. Auth: write. */
    suspend fun deleteAutoReplyKeyword(chatId: String, keyword: String): NetworkResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val path = "/autoreply/chat/${java.net.URLEncoder.encode(chatId, "UTF-8")}/keywords/" +
                    java.net.URLEncoder.encode(keyword, "UTF-8")
                val resp = execute(buildRequest(path, "DELETE"))
                if (!resp.isSuccessful) {
                    return@withContext errorResult(resp.code, resp.body, "Failed to delete keyword rule (${resp.code})")
                }
                NetworkResult.Success(true)
            } catch (e: Exception) {
                handleException(e)
            }
        }

    /** GET /api/ai — { enabled, personality }. Auth: read. */
    suspend fun getAiSettings(): NetworkResult<AiConfig> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/ai", "GET"))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to read AI settings (${resp.code})")
            }
            val obj = JSONObject(resp.body)
            val ai = obj.optJSONObject("ai") ?: JSONObject()
            NetworkResult.Success(
                AiConfig(
                    enabled = ai.optBoolean("enabled", true),
                    personality = ai.optString("personality", "friendly")
                )
            )
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** PATCH /api/ai — enabled + personality (friendly|professional|funny|concise|custom). Auth: write. */
    suspend fun updateAiSettings(enabled: Boolean, personality: String): NetworkResult<AiConfig> =
        withContext(Dispatchers.IO) {
            try {
                val valid = setOf("friendly", "professional", "funny", "concise", "custom")
                val p = if (personality.lowercase() in valid) personality.lowercase() else "friendly"
                val body = JSONObject().put("enabled", enabled).put("personality", p)
                val resp = execute(buildRequest("/ai", "PATCH", body.toString()))
                if (!resp.isSuccessful) {
                    return@withContext errorResult(resp.code, resp.body, "Failed to update AI settings (${resp.code})")
                }
                val ai = JSONObject(resp.body).optJSONObject("ai") ?: JSONObject()
                NetworkResult.Success(
                    AiConfig(
                        enabled = ai.optBoolean("enabled", enabled),
                        personality = ai.optString("personality", p)
                    )
                )
            } catch (e: Exception) {
                handleException(e)
            }
        }

    /** GET /api/groups — groups the bot participates in. Auth: read. */
    suspend fun getGroups(): NetworkResult<List<GroupSummary>> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/groups", "GET"))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to list groups (${resp.code})")
            }
            val obj = JSONObject(resp.body)
            val arr = obj.optJSONArray("groups") ?: JSONArray()
            val list = mutableListOf<GroupSummary>()
            for (i in 0 until arr.length()) {
                val g = arr.getJSONObject(i)
                list.add(
                    GroupSummary(
                        id = g.optString("id", ""),
                        name = g.optString("name", ""),
                        size = g.optInt("size", 0),
                        isBotAdmin = g.optBoolean("isBotAdmin", false)
                    )
                )
            }
            NetworkResult.Success(list)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** GET /api/groups/{groupId}/settings — moderation settings for one group. Auth: read. */
    suspend fun getGroupSettings(groupId: String): NetworkResult<GroupSettings> = withContext(Dispatchers.IO) {
        try {
            val path = "/groups/${java.net.URLEncoder.encode(groupId, "UTF-8")}/settings"
            val resp = execute(buildRequest(path, "GET"))
            if (!resp.isSuccessful) {
                return@withContext errorResult(resp.code, resp.body, "Failed to read group settings (${resp.code})")
            }
            val obj = JSONObject(resp.body)
            val s = obj.optJSONObject("settings") ?: JSONObject()
            NetworkResult.Success(s.toGroupSettings())
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** PATCH /api/groups/{groupId}/settings — update moderation fields. Auth: write. */
    suspend fun updateGroupSettings(groupId: String, settings: GroupSettings): NetworkResult<GroupSettings> =
        withContext(Dispatchers.IO) {
            try {
                val body = settings.toApiPatch()
                val path = "/groups/${java.net.URLEncoder.encode(groupId, "UTF-8")}/settings"
                val resp = execute(buildRequest(path, "PATCH", body.toString()))
                if (!resp.isSuccessful) {
                    return@withContext errorResult(resp.code, resp.body, "Failed to update group settings (${resp.code})")
                }
                val s = JSONObject(resp.body).optJSONObject("settings") ?: JSONObject()
                NetworkResult.Success(s.toGroupSettings())
            } catch (e: Exception) {
                handleException(e)
            }
        }

    private fun JSONObject.toGroupSettings(): GroupSettings = GroupSettings(
        antiLink = optBoolean("antilink", false),
        welcomeMessage = optBoolean("welcome", false),
        welcomeText = optString("welcomeMessage", ""),
        leaveOnAntiLink = optString("antilinkAction", "delete") == "kick"
    )

    private fun GroupSettings.toApiPatch(): JSONObject {
        val body = JSONObject()
        body.put("antilink", antiLink)
        body.put("antilinkAction", if (leaveOnAntiLink) "kick" else "delete")
        body.put("welcome", welcomeMessage)
        if (welcomeText.isNotBlank()) body.put("welcomeMessage", welcomeText)
        return body
    }

    // ── Web pairing surface (docs/API.md §8, top-level /pair, NOT under /api) ────

    /** GET /pair/status — auth snapshot incl. QR image and current pairing code. Token required. */
    suspend fun checkPairingStatus(token: String? = null): NetworkResult<PairingStatusResponse> =
        withContext(Dispatchers.IO) {
            val activeToken = token?.trim().takeUnless { it.isNullOrBlank() } ?: prefs.pairingToken
            if (activeToken.isBlank()) {
                return@withContext NetworkResult.Error(
                    "Pairing token required. Paste the token shown in your EDBOTS server console.",
                    401
                )
            }
            try {
                val path = "/pair/status?token=${java.net.URLEncoder.encode(activeToken, "UTF-8")}"
                val resp = execute(buildRequest(path, "GET", usePairingToken = true))
                if (resp.code == 401) {
                    return@withContext NetworkResult.Error(
                        "Invalid or expired pairing token. Get the current token from the server console.",
                        401
                    )
                }
                if (!resp.isSuccessful) {
                    return@withContext errorResult(resp.code, resp.body, "Pairing status check failed (${resp.code})")
                }

                val obj = JSONObject(resp.body)
                val auth = obj.optJSONObject("auth") ?: JSONObject()
                val state = auth.optString("state", "INITIALIZING")
                val codeExpiresIso = auth.optString("pairingCodeExpiresAt", "").ifBlank { null }

                NetworkResult.Success(
                    PairingStatusResponse(
                        ok = true,
                        authState = state,
                        state = state,
                        tokenValid = true,
                        qrImage = auth.optString("qrImage", "").ifBlank { null },
                        pairingCode = auth.optString("pairingCode", "").ifBlank { null },
                        pairingCodeExpiresAt = codeExpiresIso?.let { parseIsoToEpochMillis(it) } ?: 0L,
                        failureReason = auth.optString("failureReason", "").ifBlank { null }
                    )
                )
            } catch (e: Exception) {
                handleException(e)
            }
        }

    /**
     * POST /pair/request-code — request a phone-number pairing code. Token required.
     * Returns 202 with NO code in the body: the code arrives asynchronously via
     * the SSE stream or the next GET /pair/status poll (auth.pairingCode).
     */
    suspend fun requestPairingCode(phoneNumber: String): NetworkResult<PairingCodeResponse> =
        withContext(Dispatchers.IO) {
            val activeToken = prefs.pairingToken
            if (activeToken.isBlank()) {
                return@withContext NetworkResult.Error(
                    "Pairing token required. Paste the token shown in your EDBOTS server console.",
                    401
                )
            }
            try {
                val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
                if (cleanPhone.length !in 8..15) {
                    return@withContext NetworkResult.Error(
                        "Phone number must be 8–15 digits including country code.",
                        400
                    )
                }
                val json = JSONObject().put("phoneNumber", cleanPhone)
                val resp = execute(buildRequest("/pair/request-code", "POST", json.toString(), usePairingToken = true))

                when {
                    resp.code == 202 || resp.isSuccessful -> {
                        NetworkResult.Success(
                            PairingCodeResponse(
                                ok = true,
                                code = null, // arrives via /pair/status polling
                                expiresAt = System.currentTimeMillis() + 180_000L
                            )
                        )
                    }
                    resp.code == 400 -> NetworkResult.Error("Invalid phone number (must be 8–15 digits).", 400)
                    resp.code == 401 -> NetworkResult.Error("Invalid or expired pairing token.", 401)
                    resp.code == 409 -> NetworkResult.Error("WhatsApp is already linked on the server.", 409)
                    resp.code == 429 -> NetworkResult.Error("Too many pairing attempts. Try again later.", 429)
                    resp.code == 503 -> NetworkResult.Error("Server socket not ready. Retry in a few seconds.", 503)
                    else -> errorResult(resp.code, resp.body, "Failed to request pairing code (${resp.code})")
                }
            } catch (e: Exception) {
                handleException(e)
            }
        }

    /** POST /pair/reset — reset pairing state to WAITING_FOR_AUTH. Token required. */
    suspend fun resetPairing(): NetworkResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            val resp = execute(buildRequest("/pair/reset", "POST", "{}", usePairingToken = true))
            if (resp.code == 401) {
                return@withContext NetworkResult.Error("Invalid or expired pairing token.", 401)
            }
            NetworkResult.Success(resp.isSuccessful)
        } catch (e: Exception) {
            handleException(e)
        }
    }

    /** Parses "2026-09-23T12:02:00.000Z" without java.time (unavailable below API 26). */
    private fun parseIsoToEpochMillis(iso: String): Long = try {
        val cleaned = iso.trim().removeSuffix("Z")
        val parts = cleaned.split("T")
        val dateBits = parts[0].split("-").map { it.trim().toInt() }
        val timeBits = parts.getOrElse(1) { "0:0:0" }.split(":").map { it.trim().toDouble() }
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.clear()
        calendar.set(
            dateBits[0],
            dateBits[1] - 1,
            dateBits[2],
            timeBits.getOrElse(0) { 0.0 }.toInt(),
            timeBits.getOrElse(1) { 0.0 }.toInt(),
            timeBits.getOrElse(2) { 0.0 }.toInt()
        )
        calendar.timeInMillis + ((timeBits.getOrElse(2) { 0.0 } % 1.0) * 1000).toInt()
    } catch (_: Exception) {
        0L
    }

    // ── Local default fallbacks (offline-friendly UI seed data) ──────────────────

    fun getDefaultCommands(): List<BotCommand> {
        return listOf(
            BotCommand("menu", "menu", "Show bot main menu and available features", "General", true, false),
            BotCommand("ping", "ping", "Check bot responsiveness and latency", "General", true, false),
            BotCommand("sticker", "sticker", "Convert sent images or GIFs into WhatsApp stickers", "Media", true, false),
            BotCommand("ai", "ai", "Ask questions directly to the AI", "AI", true, false),
            BotCommand("tagall", "tagall", "Mention all members in a WhatsApp group", "Group", true, true),
            BotCommand("antilink", "antilink", "Auto-delete links from non-admins in group", "Group", true, true)
        )
    }
}
