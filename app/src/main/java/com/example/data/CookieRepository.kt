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

    const val USER_TEST_NETSCAPE_COOKIE = "# Netscape HTTP Cookie File\n" +
        "# https://curl.haxx.se/rfc/cookie_spec.html\n" +
        "# This is a generated file! Do not edit.\n\n" +
        ".mozilla.org\tTRUE\t/\tFALSE\t1827392441\t_ga_B9CY1C9VBC\tGS2.1.s1792832346\$o1\$g1\$t1792832441\$j25\$l0\$h0\n" +
        ".mozilla.org\tTRUE\t/\tFALSE\t1827392346\t_ga\tGA1.1.736568089.1792832346\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1808384441\t__Secure-YNID\tCtsBCAUSjQEB8QZuZjAPCrh2VOX1uhREiSMB34E7cUuxUmsm_Nc-qlNYiCxnSCNwFAJ1JgnjoVSIQR8eXS5UG7cKpdKWG4ptsKo1xJKQQfbovBLTEFLp0mkSljQN8vzVh2Qh5hF_UdGej0XILcIE2iqDyT7gG7axZcaCt9_dHT8CMtuUu9-Hw0dxWnxX8Y_0VIulVzYoBTJFAd6pnU-Id-Ko0ZQ2Tqzqkrp9X6IAVpntJxQl1dpQOtq5BjWNE8Lv1S1j71RhBITnkh0YA_mbR2NvMT2_Dr_ySxnJmb8O\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1792834241\tGPS\t1\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t0\tYSC\tmf_i2wzNBCU\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1808384555\tVISITOR_INFO1_LIVE\trs2cD-pnRjw\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1808384555\tVISITOR_PRIVACY_METADATA\tCgJJThIEGgAgEw%3D%3D\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1827392556\tPREF\tf6=40000000&tz=Asia.Kolkata\n" +
        "accounts.google.com\tFALSE\t/\tTRUE\t1827392491\t__Host-GAPS\t1:3-oOyiPpgmGr3fw-usNmArM55L0vWxDFskuXP77P5aeQiwWpYGz8iPCUKmHcMa3uLv1DiIAApWZzXnf9Uidn8yo5vwzqlA:slE1_IwnUvdYtH2e\n" +
        "accounts.google.com\tFALSE\t/\tTRUE\t1795424448\tOTZ\t8842141_34_34__34_\n" +
        ".google.com\tTRUE\t/\tTRUE\t1808643648\tNID\tCs8CCAESgQIBOxGDSHbz1zYgCm5SO-cmXNAbJtQCNm7pPd2BuBW5iKgZZtU8FILVP-DSjT6YmOi3XuoOMfGIMRzXyEm7yoE_00fgqLrset86fID2zm2QeRE17vv7vbNkTaECCt_1untcYvzlLdPwhF60zVxRnvDUbo3F-YnQNCEx6nkEdEMB0j5MdQh6rfQMx0gaHqcYhwQgWKb3De_NzTMxW8PmJ0rAswB4GYtsINf4b9TQ_lVzLJN6_liSMDMrptOLJtsEbLJ9RfPYA8p4mSUOrguvzGVAlWdJVPx1_zqISSSFVvg9HCoIOWCS3mtUbrB6w8S2QCVOiOkuaoItZ2ue-tE25kH-fygBMkUB0qwHzzvG8fbOc6PamKuG1iz-b2eXS9ljpK27fnrL_ReTPdXc30z9Sh9cUmHcNArDA3mGpptsj7CIpmjeE2KmV4sBMwY\n" +
        ".google.com\tTRUE\t/\tFALSE\t1827392491\tSID\tg.a000EAk7Tn71VufBxmO42b3cYPO_tGTGB4JG4eud_jraDiddydblU86r0DdJx3UqCPmHW-Mg7QACgYKAf0SARISFQHGX2MiC9Rt1L9KDTdpHztjfsycRBoVAUF8yKrYHteMt4CBSwwRQU91_lrl0076\n" +
        ".google.com\tTRUE\t/\tTRUE\t1827392491\t__Secure-1PSID\tg.a000EAk7Tn71VufBxmO42b3cYPO_tGTGB4JG4eud_jraDiddydblZm0zU8iqtM9CV52IcFydpQACgYKAfsSARISFQHGX2MiW2g3YhamrTKv6Lp5S17u0xoVAUF8yKpetrwrjqa7YA_YlloxKBeR0076\n" +
        ".google.com\tTRUE\t/\tTRUE\t1827392491\t__Secure-3PSID\tg.a000EAk7Tn71VufBxmO42b3cYPO_tGTGB4JG4eud_jraDiddydbl2oyzyECqw4DgyDdiSOgLMgACgYKAeMSARISFQHGX2MiZGkRB_6D2-wQc7LuVCyIpxoVAUF8yKp-hZ37V5S4cAR5yPNvTHCr0076\n" +
        "accounts.google.com\tFALSE\t/\tTRUE\t1827392554\tLSID\to.myaccount.google.com|s.youtube:g.a000EAk7TiYWplmYFvBe4aBS-LsyJBLayXTESJIZjkz2nUCDOnMujRfJOe7TswAIp2h9sz47dgACgYKAb0SARISFQHGX2MilgU8KzX7CawL2mhPWDmAXxoVAUF8yKr8OK5DQNgveP0DG5zu3mFu0076\n" +
        "accounts.google.com\tFALSE\t/\tTRUE\t1827392554\t__Host-1PLSID\to.myaccount.google.com|s.youtube:g.a000EAk7TiYWplmYFvBe4aBS-LsyJBLayXTESJIZjkz2nUCDOnMu_v-JNfj-RG3H2h9zKMa1LQACgYKAUYSARISFQHGX2MixQ6pRIzNV5tqQdxpNvnyxBoVAUF8yKoBcz1411Q4E5fWfBruu4e20076\n" +
        "accounts.google.com\tFALSE\t/\tTRUE\t1827392554\t__Host-3PLSID\to.myaccount.google.com|s.youtube:g.a000EAk7TiYWplmYFvBe4aBS-LsyJBLayXTESJIZjkz2nUCDOnMuJZJWf8SdNAXBbePlZGIQXQACgYKAcESARISFQHGX2Mic2_8Tav9UTn--SwUWNPjNhoVAUF8yKo7dzruYR_mDrKrEsz8WSvE0076\n" +
        ".google.com\tTRUE\t/\tFALSE\t1827392491\tHSID\tAcQKlXd5vnYbB-EEL\n" +
        ".google.com\tTRUE\t/\tTRUE\t1827392491\tSSID\tA_ygwXHi2bafQQBQZ\n" +
        ".google.com\tTRUE\t/\tFALSE\t1827392491\tAPISID\tufTtRnFzwV-6ZyP_/AeZlO85RJyxKlc2z2\n" +
        ".google.com\tTRUE\t/\tTRUE\t1827392491\tSAPISID\t-9kQSEdY1Tyha0Uc/Axwc9Z9Yr8nxbMM3K\n" +
        ".google.com\tTRUE\t/\tTRUE\t1827392491\t__Secure-1PAPISID\t-9kQSEdY1Tyha0Uc/Axwc9Z9Yr8nxbMM3K\n" +
        ".google.com\tTRUE\t/\tTRUE\t1827392491\t__Secure-3PAPISID\t-9kQSEdY1Tyha0Uc/Axwc9Z9Yr8nxbMM3K\n" +
        "accounts.google.com\tFALSE\t/\tTRUE\t1827392493\tACCOUNT_CHOOSER\tAFx_qI6l-bHn1hHlvaNyWF7zdwZtHm4VXQcBcWztlidni_xi1k_Oix34Qg3UgqyFTrzPReVbU_Ax5SRtHogBlmWJelDRGiY0lLgFNhod7h2Cobm88JNEAJMP2L044-xB83T69mWjrOtOa1NZA1Q3ZlGv313KTbLJMw\n" +
        "accounts.google.com\tFALSE\t/\tTRUE\t1827392491\tSMSV\tADHTe-Bi0f2hEaI5ciAbRSot09RlRkXTI0_0Kf4PGliDp1rL3e5xXQHYg0kSj9JWVsp7zeIuO1Hh_CRHM72L5SKXEgtCHhNG9xacT_Thsy2Wz1ac5yVPye0\n" +
        ".google.com\tTRUE\t/\tFALSE\t1824368556\tSIDCC\tAPD_SKHUo-ysx2f6_M8DLczZ29iW3bKwBYWxuPTw8wWYUEzuV945bHY9X0iEN6VSN3Tye4fKfA\n" +
        ".google.com\tTRUE\t/\tTRUE\t1824368556\t__Secure-1PSIDCC\tAPD_SKEbYFzXTr1N6OU4ykO4hJWdLZtUMoOoTj2V6TtTgqcDD_rPw0j587s8VdQwnfbBhEvecw\n" +
        ".google.com\tTRUE\t/\tTRUE\t1824368556\t__Secure-3PSIDCC\tAPD_SKExFr1UuI_Z6DT5F7qb9zd5CozK3S47PjwnmQFkhI3w0Va8o_XrlWaxgfbP3f1yqj5IDw\n" +
        "myaccount.google.com\tFALSE\t/\tTRUE\t1827392493\tOSID\tg.a000EAk7TpLOFCvzjAHXfbEzMyILQkXPXKI5hl4F3Pu64p65BC0n0gv0gya6DoQIYlNU-udQZQACgYKAdsSARISFQHGX2Mi1tFUIcdMqhspcg0-5AdNexoVAUF8yKoP_m561x2Q46fRop1Cnx8P0076\n" +
        "myaccount.google.com\tFALSE\t/\tTRUE\t1827392493\t__Secure-OSID\tg.a000EAk7TpLOFCvzjAHXfbEzMyILQkXPXKI5hl4F3Pu64p65BC0nbbqsWEbbw6RcvDVcVRvJsgACgYKAe8SARISFQHGX2MikHyZ6R24rmGPYVsEmvnc-hoVAUF8yKqY6ATY29A4N_baECLF9k8G0076\n" +
        "myaccount.google.com\tFALSE\t/\tTRUE\t1795424500\tOTZ\t8842142_34_34__34_\n" +
        "www.google.com\tFALSE\t/\tTRUE\t1795424511\tOTZ\t8842142_34_34__34_\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1824368555\t__Secure-1PSIDTS\tsidts-CjUBkldj_5CnJ8eTc8Ueovwza42m8cR2MxZJhEvZDdIX8cK2mF-6YbvQxmNjLypuSfHpV7e1oRAA\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1824368555\t__Secure-3PSIDTS\tsidts-CjUBkldj_5CnJ8eTc8Ueovwza42m8cR2MxZJhEvZDdIX8cK2mF-6YbvQxmNjLypuSfHpV7e1oRAA\n" +
        ".youtube.com\tTRUE\t/\tFALSE\t1827392555\tHSID\tAUC2CcDNfiB_SZDEr\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1827392555\tSSID\tA39J8vJDYoxJ1uYwm\n" +
        ".youtube.com\tTRUE\t/\tFALSE\t1827392555\tAPISID\tufTtRnFzwV-6ZyP_/AeZlO85RJyxKlc2z2\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1827392555\tSAPISID\t-9kQSEdY1Tyha0Uc/Axwc9Z9Yr8nxbMM3K\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1827392555\t__Secure-1PAPISID\t-9kQSEdY1Tyha0Uc/Axwc9Z9Yr8nxbMM3K\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1827392555\t__Secure-3PAPISID\t-9kQSEdY1Tyha0Uc/Axwc9Z9Yr8nxbMM3K\n" +
        ".youtube.com\tTRUE\t/\tFALSE\t1827392555\tSID\tg.a000EAk7Tn71VufBxmO42b3cYPO_tGTGB4JG4eud_jraDiddydblU86r0DdJx3UqCPmHW-Mg7QACgYKAf0SARISFQHGX2MiC9Rt1L9KDTdpHztjfsycRBoVAUF8yKrYHteMt4CBSwwRQU91_lrl0076\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1827392555\t__Secure-1PSID\tg.a000EAk7Tn71VufBxmO42b3cYPO_tGTGB4JG4eud_jraDiddydblZm0zU8iqtM9CV52IcFydpQACgYKAfsSARISFQHGX2MiW2g3YhamrTKv6Lp5S17u0xoVAUF8yKpetrwrjqa7YA_YlloxKBeR0076\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1827392555\t__Secure-3PSID\tg.a000EAk7Tn71VufBxmO42b3cYPO_tGTGB4JG4eud_jraDiddydbl2oyzyECqw4DgyDdiSOgLMgACgYKAeMSARISFQHGX2MiZGkRB_6D2-wQc7LuVCyIpxoVAUF8yKp-hZ37V5S4cAR5yPNvTHCr0076\n" +
        ".youtube.com\tTRUE\t/\tFALSE\t1824368559\tSIDCC\tAPD_SKHJtJzXs7CuXv3u0fIQuaPw9GF05AEYYc85F3xhgvEhOLtrlvWRFTcZ8dvWMk5hNzJI\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1824368559\t__Secure-1PSIDCC\tAPD_SKH0HD4KTiRa1rWzdbAp8jXqs08HEwx7dzgY2lOpf2z9UlbYt-s2OXqTy4au6m_Ex4eBqQ\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1824368559\t__Secure-3PSIDCC\tAPD_SKF3254J6tUYZzTxB_FaTdIkR9p60jcVGT0hBoRylO9DZc0032XnvikEk4stqt1bvEtq\n" +
        ".youtube.com\tTRUE\t/\tTRUE\t1827392555\tLOGIN_INFO\tAFmmF2swRQIhAJVRJkK81u4qLfyj6tCKWDlGYZgp2tJl53vCf8CupTc1AiAZhCJ9qoiLj6nKHJwE8YFkN_zNtMgvL52otbI6ytShXw:QUQ3MjNmemxxbzFPRVMyYUJiRjhUNXptaGpxR2pvckdDSnljVnRTRG9nT28tbm1mZTVCVEVHRTBYeTRSLXdpUUNmdnI4QldhbXl3RkFHVnlnOEt2enRFNlpZWTl6dlh1UUFCd01JNkZmOUJhVlBvUkVxd1RSZlVZc2M1dUc2Q1ZVeGxYZTBRckRScEVPSXlhdVpUYXlucThUUUhqZ1VqdzBB"

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
        val normalized = raw.replace("\r\n", "\n").trim()
        return try {
            android.util.Base64.encodeToString(normalized.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            normalized
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
        val sanitizedCookie = rawCookie.replace("\r\n", "\n").trim()
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

    /**
     * Returns raw cookie text (which may be Netscape multi-line format or semicolon header format)
     */
    fun getRawCookieText(): String? {
        val cfg = _cookieConfig.value
        return if (cfg.isEnabled && cfg.rawCookie.isNotBlank()) {
            cfg.rawCookie
        } else {
            null
        }
    }

    /**
     * Returns single-line HTTP Cookie header (name=value; name2=value2) safe for OkHttp / Media3 headers.
     * Automatically converts Netscape HTTP Cookie File format into HTTP Cookie header pairs.
     */
    fun getCookieHeader(): String? {
        val cfg = _cookieConfig.value
        if (!cfg.isEnabled || cfg.rawCookie.isBlank()) return null

        val raw = cfg.rawCookie.trim()
        if (raw.contains("\t") || raw.startsWith("#")) {
            val cookieMap = linkedMapOf<String, String>()
            for (line in raw.lines()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val parts = trimmed.split("\t")
                if (parts.size >= 7) {
                    val domain = parts[0]
                    val name = parts[5].trim()
                    val value = parts[6].trim()
                    if (name.isNotEmpty()) {
                        // Prefer youtube.com cookies if duplicate keys exist across domains
                        if (!cookieMap.containsKey(name) || domain.contains("youtube.com")) {
                            cookieMap[name] = value
                        }
                    }
                }
            }
            if (cookieMap.isNotEmpty()) {
                return cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
            }
        }
        return raw.replace("\r", "").replace("\n", "; ").trim()
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

            val headerCookie = getCookieHeader()
            if (!headerCookie.isNullOrBlank()) {
                requestBuilder.header("Cookie", headerCookie)
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
