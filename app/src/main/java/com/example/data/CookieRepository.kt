package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class CookieConfig(
    val rawCookie: String = "",
    val customUserAgent: String = "",
    val isEnabled: Boolean = true,
    val speedBoosterEnabled: Boolean = true,
    val bufferChunkSizeKb: Int = 64,
    val lastTestedTime: Long = 0L,
    val lastTestStatus: String = "Not tested"
)

object CookieRepository {
    private const val TAG = "CookieRepository"
    private const val PREFS_NAME = "phoenix_cookies_prefs"
    private const val KEY_RAW_COOKIE = "key_raw_cookie"
    private const val KEY_CUSTOM_UA = "key_custom_ua"
    private const val KEY_ENABLED = "key_cookies_enabled"
    private const val KEY_SPEED_BOOSTER = "key_speed_booster"
    private const val KEY_BUFFER_KB = "key_buffer_chunk_kb"
    private const val KEY_LAST_TEST_TIME = "key_last_test_time"
    private const val KEY_LAST_TEST_STATUS = "key_last_test_status"

    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private val _cookieConfig = MutableStateFlow(CookieConfig())
    val cookieConfig: StateFlow<CookieConfig> = _cookieConfig.asStateFlow()

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadConfig()
        }
    }

    private fun encodeCookie(raw: String): String {
        if (raw.isBlank()) return ""
        val sanitized = raw.replace("\r", "").replace("\n", "").trim()
        return try {
            android.util.Base64.encodeToString(sanitized.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            sanitized
        }
    }

    private fun decodeCookie(encoded: String): String {
        if (encoded.isBlank()) return ""
        return try {
            val bytes = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            encoded
        }
    }

    private fun loadConfig() {
        val sp = prefs ?: return
        val rawStored = sp.getString(KEY_RAW_COOKIE, "") ?: ""
        val raw = decodeCookie(rawStored)
        val ua = sp.getString(KEY_CUSTOM_UA, "") ?: ""
        val enabled = sp.getBoolean(KEY_ENABLED, true)
        val booster = sp.getBoolean(KEY_SPEED_BOOSTER, true)
        val buffer = sp.getInt(KEY_BUFFER_KB, 64)
        val lastTime = sp.getLong(KEY_LAST_TEST_TIME, 0L)
        val lastStatus = sp.getString(KEY_LAST_TEST_STATUS, "Not tested") ?: "Not tested"

        _cookieConfig.value = CookieConfig(
            rawCookie = raw,
            customUserAgent = ua,
            isEnabled = enabled,
            speedBoosterEnabled = booster,
            bufferChunkSizeKb = buffer,
            lastTestedTime = lastTime,
            lastTestStatus = lastStatus
        )
    }

    fun saveCookies(
        rawCookie: String,
        customUserAgent: String? = null,
        isEnabled: Boolean = true,
        speedBooster: Boolean = true,
        bufferSizeKb: Int = 64
    ) {
        val sp = prefs ?: return
        val sanitizedCookie = rawCookie.replace("\r", "").replace("\n", "").trim()
        val sanitizedUa = customUserAgent?.replace("\r", "")?.replace("\n", "")?.trim() ?: ""
        val encoded = encodeCookie(sanitizedCookie)

        sp.edit()
            .putString(KEY_RAW_COOKIE, encoded)
            .putString(KEY_CUSTOM_UA, sanitizedUa)
            .putBoolean(KEY_ENABLED, isEnabled)
            .putBoolean(KEY_SPEED_BOOSTER, speedBooster)
            .putInt(KEY_BUFFER_KB, bufferSizeKb)
            .apply()

        _cookieConfig.value = _cookieConfig.value.copy(
            rawCookie = sanitizedCookie,
            customUserAgent = sanitizedUa,
            isEnabled = isEnabled,
            speedBoosterEnabled = speedBooster,
            bufferChunkSizeKb = bufferSizeKb
        )
        Log.i(TAG, "Saved cookie configuration (length: ${sanitizedCookie.length}, enabled: $isEnabled)")
    }

    fun clearCookies() {
        val sp = prefs ?: return
        sp.edit()
            .remove(KEY_RAW_COOKIE)
            .putString(KEY_LAST_TEST_STATUS, "Cookies cleared")
            .apply()

        _cookieConfig.value = _cookieConfig.value.copy(
            rawCookie = "",
            lastTestStatus = "Cookies cleared"
        )
    }

    fun getCookieHeader(): String? {
        val cfg = _cookieConfig.value
        return if (cfg.isEnabled && cfg.rawCookie.isNotBlank()) {
            cfg.rawCookie
        } else {
            null
        }
    }

    fun getUserAgent(): String {
        val cfg = _cookieConfig.value
        return if (cfg.customUserAgent.isNotBlank()) {
            cfg.customUserAgent
        } else {
            DEFAULT_USER_AGENT
        }
    }

    suspend fun testConnection(targetUrl: String = "https://www.google.com/generate_204"): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            val cfg = _cookieConfig.value
            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .build()

            val requestBuilder = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", getUserAgent())

            if (cfg.isEnabled && cfg.rawCookie.isNotBlank()) {
                requestBuilder.header("Cookie", cfg.rawCookie)
            }

            try {
                val startTime = System.currentTimeMillis()
                val response = client.newCall(requestBuilder.build()).execute()
                val latency = System.currentTimeMillis() - startTime
                val code = response.code

                val statusMessage = if (response.isSuccessful || code in 200..308) {
                    "Verified • HTTP $code (${latency}ms)"
                } else {
                    "HTTP $code • Server responded with error"
                }

                prefs?.edit()
                    ?.putLong(KEY_LAST_TEST_TIME, System.currentTimeMillis())
                    ?.putString(KEY_LAST_TEST_STATUS, statusMessage)
                    ?.apply()

                _cookieConfig.value = _cookieConfig.value.copy(
                    lastTestedTime = System.currentTimeMillis(),
                    lastTestStatus = statusMessage
                )

                Pair(response.isSuccessful || code in 200..308, statusMessage)
            } catch (e: Exception) {
                val errorMsg = "Failed: ${e.message ?: "Network error"}"
                prefs?.edit()
                    ?.putLong(KEY_LAST_TEST_TIME, System.currentTimeMillis())
                    ?.putString(KEY_LAST_TEST_STATUS, errorMsg)
                    ?.apply()

                _cookieConfig.value = _cookieConfig.value.copy(
                    lastTestedTime = System.currentTimeMillis(),
                    lastTestStatus = errorMsg
                )
                Pair(false, errorMsg)
            }
        }
}
