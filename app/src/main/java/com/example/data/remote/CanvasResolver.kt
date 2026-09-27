package com.example.data.remote

import android.util.Log
import com.example.data.remote.models.CanvasDto
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Dedicated high-performance Spotify Canvas Video Resolver.
 * Connects directly to the official Spotify Canvas Video API endpoint:
 * https://spotify2.ai.studio/api/canvas
 */
object CanvasResolver {

    private const val TAG = "CanvasResolver"
    private const val PRIMARY_CANVAS_API = "https://spotify2.ai.studio/api/canvas"
    private const val FALLBACK_CANVAS_API = "https://spotify2.ai.studio/api/canvas/search"

    private val canvasCache = ConcurrentHashMap<String, String>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Resolves the Spotify Canvas 9:16 looping video URL for a given track.
     */
    suspend fun resolveCanvas(track: TrackDto): String? = withContext(Dispatchers.IO) {
        val title = track.title.trim()
        val artist = track.artist.trim()
        val trackId = track.id.trim()
        val spotifyId = track.spotifyId?.trim() ?: extractSpotifyIdFromUri(track.spotifyUri)
        val spotifyUri = track.spotifyUri?.trim()

        val cacheKey = "${title.lowercase()}_${artist.lowercase()}"

        // 1. Check in-memory cache
        canvasCache[cacheKey]?.let { return@withContext it }
        if (trackId.isNotBlank()) {
            canvasCache[trackId]?.let { return@withContext it }
        }
        if (!spotifyId.isNullOrBlank()) {
            canvasCache[spotifyId]?.let { return@withContext it }
        }

        // 2. Query the dedicated Spotify Canvas Video API endpoint
        val resolvedUrl = queryCanvasApi(
            title = title,
            artist = artist,
            trackId = trackId,
            spotifyId = spotifyId,
            spotifyUri = spotifyUri,
            album = track.album
        )

        if (!resolvedUrl.isNullOrBlank()) {
            Log.d(TAG, "Canvas resolved successfully for '$title - $artist': $resolvedUrl")
            cacheCanvas(track, cacheKey, resolvedUrl)
            return@withContext resolvedUrl
        }

        // 3. Fallback to title-only query if artist combination had no direct match
        if (artist.isNotBlank() && title.isNotBlank()) {
            val cleanSongTitle = cleanTitle(title)
            val fallbackUrl = queryCanvasApi(
                title = cleanSongTitle,
                artist = null,
                trackId = null,
                spotifyId = null,
                spotifyUri = null,
                album = null
            )
            if (!fallbackUrl.isNullOrBlank()) {
                Log.d(TAG, "Canvas resolved via clean title for '$cleanSongTitle': $fallbackUrl")
                cacheCanvas(track, cacheKey, fallbackUrl)
                return@withContext fallbackUrl
            }
        }

        return@withContext null
    }

    private fun queryCanvasApi(
        title: String?,
        artist: String?,
        trackId: String?,
        spotifyId: String?,
        spotifyUri: String?,
        album: String?
    ): String? {
        val endpoints = listOf(PRIMARY_CANVAS_API, FALLBACK_CANVAS_API)

        for (baseUrl in endpoints) {
            try {
                val queryParams = mutableListOf<String>()

                if (!title.isNullOrBlank()) {
                    queryParams.add("title=${URLEncoder.encode(title, "UTF-8")}")
                }
                if (!artist.isNullOrBlank()) {
                    queryParams.add("artist=${URLEncoder.encode(artist, "UTF-8")}")
                }
                if (!spotifyId.isNullOrBlank()) {
                    queryParams.add("spotifyId=${URLEncoder.encode(spotifyId, "UTF-8")}")
                }
                if (!trackId.isNullOrBlank()) {
                    queryParams.add("trackId=${URLEncoder.encode(trackId, "UTF-8")}")
                }
                if (!spotifyUri.isNullOrBlank()) {
                    queryParams.add("spotifyUri=${URLEncoder.encode(spotifyUri, "UTF-8")}")
                }
                if (!album.isNullOrBlank()) {
                    queryParams.add("album=${URLEncoder.encode(album, "UTF-8")}")
                }

                if (queryParams.isEmpty()) continue

                val fullUrl = "$baseUrl?${queryParams.joinToString("&")}"
                val request = Request.Builder()
                    .url(fullUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "SpotifyAndroidApp/8.9.0")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val json = JSONObject(body)
                        if (json.optBoolean("success", false) || json.has("data")) {
                            val data = json.optJSONObject("data")
                            val canvasUrl = data?.optString("canvasUrl", "")
                                ?.ifBlank { data.optString("videoUrl", "") }
                                ?.ifBlank { data.optString("url", "") }
                                ?: json.optString("canvasUrl", "")
                                    .ifBlank { json.optString("videoUrl", "") }

                            if (!canvasUrl.isNullOrBlank() && (canvasUrl.startsWith("http://") || canvasUrl.startsWith("https://"))) {
                                return canvasUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Canvas API query error on $baseUrl: ${e.message}")
            }
        }
        return null
    }

    private fun extractSpotifyIdFromUri(uri: String?): String? {
        if (uri.isNullOrBlank()) return null
        if (uri.startsWith("spotify:track:")) {
            return uri.removePrefix("spotify:track:").trim()
        }
        return null
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace(Regex("(?i)\\s*\\(From \".*?\"\\)"), "")
            .replace(Regex("(?i)\\s*\\(feat\\..*?\\)"), "")
            .replace(Regex("(?i)\\s*\\|.*$"), "")
            .replace(Regex("(?i)\\s*-.*$"), "")
            .trim()
    }

    private fun cacheCanvas(track: TrackDto, cacheKey: String, url: String) {
        canvasCache[cacheKey] = url
        if (track.id.isNotBlank()) {
            canvasCache[track.id] = url
        }
        track.spotifyId?.let { canvasCache[it] = url }
    }
}
