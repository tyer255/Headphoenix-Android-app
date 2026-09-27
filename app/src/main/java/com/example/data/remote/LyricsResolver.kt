package com.example.data.remote

import android.util.Log
import com.example.data.remote.models.LyricsDto
import com.example.data.remote.models.LyricsLineDto
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Serializable
data class LrcLibResponse(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

object LyricsResolver {
    private const val TAG = "LyricsResolver"
    private val cache = ConcurrentHashMap<String, LyricsDto>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun resolveLyrics(track: TrackDto): LyricsDto? = withContext(Dispatchers.IO) {
        val cacheKey = "${track.title.trim().lowercase()}__${track.artist.trim().lowercase()}"
        cache[cacheKey]?.let { return@withContext it }
        cache[track.id]?.let { return@withContext it }

        // Step 1: Clean title and artist
        val cleanTitle = cleanTrackTitle(track.title)
        val cleanArtist = cleanArtistName(track.artist)

        Log.d(TAG, "Resolving lyrics for: '$cleanTitle' by '$cleanArtist' (original: '${track.title}', '${track.artist}')")

        // Step 1: Query backend lyrics API directly
        var result: LyricsDto? = null
        try {
            val apiResponse = ApiClient.apiService.getLyrics(id = track.id, title = cleanTitle, artist = cleanArtist)
            if (apiResponse.isSuccessful) {
                val body = apiResponse.body()?.data
                if (body != null && body.lines.isNotEmpty()) {
                    result = body
                } else if (body != null && !body.plainLyrics.isNullOrBlank() && !body.plainLyrics.contains("unavailable", ignoreCase = true)) {
                    result = parsePlainLyrics(track, body.plainLyrics)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Backend lyrics endpoint query note: ${e.message}")
        }

        // Step 2: Try LRCLIB exact get if backend didn't return lines
        if (result == null || result.lines.isEmpty()) {
            result = fetchLrcLibGet(cleanTitle, cleanArtist, track.duration)
        }
        
        // Step 3: If not found, try LRCLIB search
        if (result == null || result.lines.isEmpty()) {
            result = fetchLrcLibSearch("$cleanTitle $cleanArtist")
        }

        // Step 4: If not found, try search with original title
        if ((result == null || result.lines.isEmpty()) && cleanTitle != track.title) {
            result = fetchLrcLibSearch("${track.title} ${track.artist}")
        }

        // Step 6: If lyrics found, populate track metadata and cache
        if (result != null && result.lines.isNotEmpty()) {
            val finalLyrics = result.copy(
                trackId = track.id,
                title = track.title,
                artist = track.artist
            )
            cache[cacheKey] = finalLyrics
            cache[track.id] = finalLyrics
            return@withContext finalLyrics
        }

        null
    }

    private fun fetchLrcLibGet(title: String, artist: String, duration: Int?): LyricsDto? {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            var url = "https://lrclib.net/api/get?track_name=$encodedTitle&artist_name=$encodedArtist"
            if (duration != null && duration > 30) {
                url += "&duration=$duration"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HeadphonixMusicApp/1.0 (Android)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                val lrcData = jsonParser.decodeFromString<LrcLibResponse>(bodyString)
                processLrcData(lrcData)
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchLrcLibGet error: ${e.message}")
            null
        }
    }

    private fun fetchLrcLibSearch(query: String): LyricsDto? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://lrclib.net/api/search?q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "HeadphonixMusicApp/1.0 (Android)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyString = response.body?.string() ?: return null
                val list = jsonParser.decodeFromString<List<LrcLibResponse>>(bodyString)
                val best = list.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                    ?: list.firstOrNull { !it.plainLyrics.isNullOrBlank() }
                if (best != null) {
                    processLrcData(best)
                } else null
            }
        } catch (e: Exception) {
            Log.d(TAG, "fetchLrcLibSearch error: ${e.message}")
            null
        }
    }

    private fun processLrcData(data: LrcLibResponse): LyricsDto? {
        if (!data.syncedLyrics.isNullOrBlank()) {
            val parsedLines = parseLrc(data.syncedLyrics)
            if (parsedLines.isNotEmpty()) {
                return LyricsDto(
                    title = data.trackName ?: "",
                    artist = data.artistName ?: "",
                    synced = true,
                    lines = parsedLines,
                    plainLyrics = data.plainLyrics
                )
            }
        }

        if (!data.plainLyrics.isNullOrBlank()) {
            val rawLines = data.plainLyrics.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("[") }
            if (rawLines.isNotEmpty()) {
                val durMs = ((data.duration ?: 180.0) * 1000L).toLong()
                val step = (durMs / (rawLines.size + 1)).coerceAtLeast(3500L)
                val lines = rawLines.mapIndexed { i, txt ->
                    val tMs = (i * step) + 1500L
                    LyricsLineDto(
                        time = tMs / 1000.0,
                        startTimeMs = tMs,
                        text = txt
                    )
                }
                return LyricsDto(
                    title = data.trackName ?: "",
                    artist = data.artistName ?: "",
                    synced = false,
                    lines = lines,
                    plainLyrics = data.plainLyrics
                )
            }
        }

        return null
    }

    fun parseLrc(lrcText: String): List<LyricsLineDto> {
        val pattern = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")
        val lines = mutableListOf<LyricsLineDto>()

        lrcText.lines().forEach { rawLine ->
            val match = pattern.find(rawLine.trim())
            if (match != null) {
                val (minStr, secStr, fracStr, text) = match.destructured
                val cleanText = text.trim()
                if (cleanText.isNotEmpty() && !cleanText.startsWith("[") && cleanText != "♫" && cleanText != "♫♫♫") {
                    val mins = minStr.toLongOrNull() ?: 0L
                    val secs = secStr.toLongOrNull() ?: 0L
                    val frac = if (fracStr.isNotEmpty()) {
                        when (fracStr.length) {
                            1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                            else -> fracStr.take(3).toLongOrNull() ?: 0L
                        }
                    } else 0L
                    val timeMs = (mins * 60 + secs) * 1000L + frac
                    lines.add(
                        LyricsLineDto(
                            time = timeMs / 1000.0,
                            startTimeMs = timeMs,
                            text = cleanText
                        )
                    )
                }
            }
        }
        return lines.sortedBy { it.startTimeMs }
    }

    private fun parsePlainLyrics(track: TrackDto, plain: String): LyricsDto {
        val rawLines = plain.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val durMs = (track.duration.coerceAtLeast(30) * 1000L)
        val step = (durMs / (rawLines.size + 1)).coerceAtLeast(3500L)
        val lines = rawLines.mapIndexed { i, txt ->
            val tMs = (i * step) + 1500L
            LyricsLineDto(
                time = tMs / 1000.0,
                startTimeMs = tMs,
                text = txt
            )
        }
        return LyricsDto(
            trackId = track.id,
            title = track.title,
            artist = track.artist,
            synced = false,
            lines = lines,
            plainLyrics = plain
        )
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("""\|.*$"""), "")
            .replace(Regex("""\(From\s+["'].*?["']\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(feat\..*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\[feat\..*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""-\s*Single""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Official.*?\)|\[Official.*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Remix\)|\[Remix\]""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .split(",", "&", "feat.", "ft.", ";")
            .firstOrNull()?.trim() ?: artist.trim()
    }

    private fun isValidSpotifyId(id: String): Boolean {
        val clean = id.removePrefix("spotify:track:")
        return clean.length == 22 && clean.all { it.isLetterOrDigit() }
    }
}
