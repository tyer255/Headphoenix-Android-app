package com.example.data.remote

import android.util.Log
import com.example.data.remote.models.ExtractedError
import com.example.data.remote.models.ExtractedPlaylist
import com.example.data.remote.models.ExtractedTrack
import com.example.data.remote.models.PlaylistExtractResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Direct client-side extractor for public Spotify playlists.
 * Extracts playlist metadata (title, cover image, description) and track list
 * directly from Spotify's public embed interface without requiring an external proxy server.
 */
object SpotifyNativeExtractor {

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private val PLAYLIST_ID_PATTERN = Pattern.compile("playlist[/:]([a-zA-Z0-9]+)")
    private val NEXT_DATA_PATTERN = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>(.*?)</script>")

    // Safe JsonElement helpers that never throw ClassCastException or IllegalArgumentException on JsonNull
    private val JsonElement?.asJsonObjectOrNull: JsonObject?
        get() = this as? JsonObject

    private val JsonElement?.asJsonArrayOrNull: JsonArray?
        get() = this as? JsonArray

    private val JsonElement?.asStringOrNull: String?
        get() = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

    private val JsonElement?.asLongOrNull: Long?
        get() = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.longOrNull

    suspend fun extract(
        inputUrl: String,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<PlaylistExtractResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = inputUrl.trim()
            if (cleanUrl.isBlank()) {
                return@withContext Result.failure(Exception("Spotify playlist link cannot be empty."))
            }

            // 1. Resolve redirect if it's a short link (e.g. spotify.link)
            var resolvedUrl = cleanUrl
            if (cleanUrl.contains("spotify.link")) {
                try {
                    val headReq = Request.Builder()
                        .url(cleanUrl)
                        .header("User-Agent", USER_AGENT)
                        .head()
                        .build()
                    httpClient.newCall(headReq).execute().use { resp ->
                        resolvedUrl = resp.request.url.toString()
                    }
                } catch (e: Exception) {
                    try {
                        val getReq = Request.Builder()
                            .url(cleanUrl)
                            .header("User-Agent", USER_AGENT)
                            .get()
                            .build()
                        httpClient.newCall(getReq).execute().use { resp ->
                            resolvedUrl = resp.request.url.toString()
                        }
                    } catch (ignored: Exception) {}
                }
            }

            // 2. Extract Playlist ID
            val matcher = PLAYLIST_ID_PATTERN.matcher(resolvedUrl)
            if (!matcher.find()) {
                return@withContext Result.failure(
                    Exception("Could not extract Spotify playlist ID. Please check the URL format (e.g. https://open.spotify.com/playlist/...).")
                )
            }
            val playlistId = matcher.group(1) ?: return@withContext Result.failure(
                Exception("Invalid Spotify playlist ID.")
            )

            // 3. Fetch public Spotify embed page
            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"
            val request = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            val html = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    return@withContext Result.failure(
                        Exception(
                            when (code) {
                                404 -> "Playlist not found on Spotify. Make sure the playlist is public and active."
                                403 -> "Access restricted to this Spotify playlist."
                                else -> "Spotify server returned HTTP $code."
                            }
                        )
                    )
                }
                response.body?.string() ?: ""
            }

            if (html.isBlank()) {
                return@withContext Result.failure(Exception("Received empty response from Spotify."))
            }

            // 4. Parse __NEXT_DATA__ from embed page
            val dataMatcher = NEXT_DATA_PATTERN.matcher(html)
            if (!dataMatcher.find()) {
                return@withContext fetchOEmbedFallback(playlistId, cleanUrl)
            }

            val jsonContent = dataMatcher.group(1) ?: return@withContext Result.failure(
                Exception("Could not parse Spotify playlist structure.")
            )

            // Debug logging of raw response as required
            try {
                Log.d("PLAYLIST_SYNC_RAW_RESPONSE", jsonContent)
            } catch (ignored: Throwable) {}

            val rootElement = try {
                json.parseToJsonElement(jsonContent)
            } catch (e: Exception) {
                Log.e("PLAYLIST_SYNC", "Error parsing __NEXT_DATA__ JSON", e)
                return@withContext fetchOEmbedFallback(playlistId, cleanUrl)
            }

            val root = rootElement.asJsonObjectOrNull
            val props = root?.get("props").asJsonObjectOrNull
            val pageProps = props?.get("pageProps").asJsonObjectOrNull
            val state = pageProps?.get("state").asJsonObjectOrNull
            val data = state?.get("data").asJsonObjectOrNull
            val entity = data?.get("entity").asJsonObjectOrNull
                ?: return@withContext fetchOEmbedFallback(playlistId, cleanUrl)

            val playlistName = entity["name"].asStringOrNull
                ?: entity["title"].asStringOrNull
                ?: "Spotify Playlist"

            val description = entity["description"].asStringOrNull
                ?: entity["subtitle"].asStringOrNull
                ?: ""

            // Extract highest-quality playlist cover art URL
            var coverImage: String? = extractHighestQualityImage(entity["coverArt"])
                ?: extractHighestQualityImage(entity["visualIdentity"])
                ?: extractHighestQualityImage(entity["images"])
                ?: extractHighestQualityImage(entity["image"])

            // Extract tracks safely with their OWN individual artwork
            val rawTrackList = entity["trackList"].asJsonArrayOrNull ?: emptyList()
            val totalRawTracks = rawTrackList.size
            val tracks = mutableListOf<ExtractedTrack>()
            val seenUris = mutableSetOf<String>()

            for ((idx, itemElem) in rawTrackList.withIndex()) {
                onProgress?.invoke(idx + 1, totalRawTracks)
                val tObj = itemElem.asJsonObjectOrNull ?: continue
                val rawTrackId = tObj["id"].asStringOrNull
                    ?: tObj["trackId"].asStringOrNull
                val uri = tObj["uri"].asStringOrNull
                    ?: rawTrackId?.let { "spotify:track:$it" }
                    ?: "spotify:track:$playlistId-$idx"

                val spotifyTrackId = rawTrackId ?: uri.removePrefix("spotify:track:")

                if (seenUris.add(uri)) {
                    val trackTitle = tObj["title"].asStringOrNull
                        ?: tObj["name"].asStringOrNull
                        ?: "Track #${idx + 1}"

                    val artistName = tObj["subtitle"].asStringOrNull
                        ?: tObj["artist"].asStringOrNull
                        ?: run {
                            val artistsArr = tObj["artists"].asJsonArrayOrNull
                            artistsArr?.mapNotNull {
                                it.asJsonObjectOrNull?.get("name").asStringOrNull ?: it.asStringOrNull
                            }?.filter { it.isNotBlank() }?.joinToString(", ")?.takeIf { it.isNotBlank() }
                        }
                        ?: "Unknown Artist"

                    val albumObj = tObj["album"].asJsonObjectOrNull
                    val albumName = albumObj?.get("name").asStringOrNull
                        ?: albumObj?.get("title").asStringOrNull
                        ?: tObj["albumName"].asStringOrNull
                        ?: tObj["album"].asStringOrNull

                    val isrc = tObj["isrc"].asStringOrNull
                        ?: tObj["external_ids"].asJsonObjectOrNull?.get("isrc").asStringOrNull
                        ?: tObj["externalIds"].asJsonObjectOrNull?.get("isrc").asStringOrNull

                    val durationMs = tObj["duration"].asLongOrNull ?: 0L

                    // Safely extract audioPreview URL
                    val previewUrl = tObj["audioPreview"].asJsonObjectOrNull?.get("url").asStringOrNull
                        ?: tObj["previewUrl"].asStringOrNull

                    // Extract the track's OWN artwork (never the playlist's collage/cover)
                    val trackArtwork = extractHighestQualityImage(tObj["coverArt"])
                        ?: extractHighestQualityImage(albumObj?.get("coverArt"))
                        ?: extractHighestQualityImage(tObj["images"])
                        ?: extractHighestQualityImage(albumObj?.get("images"))
                        ?: extractHighestQualityImage(tObj["visualIdentity"])
                        ?: extractHighestQualityImage(albumObj?.get("visualIdentity"))
                        ?: extractHighestQualityImage(tObj["artwork"])
                        ?: extractHighestQualityImage(tObj["imageUrl"])
                        ?: extractHighestQualityImage(tObj["image"])
                        ?: extractHighestQualityImage(albumObj?.get("imageUrl"))
                        ?: extractHighestQualityImage(albumObj?.get("image"))

                    tracks.add(
                        ExtractedTrack(
                            name = trackTitle,
                            artist = artistName,
                            album = albumName,
                            duration = durationMs,
                            spotifyUri = uri,
                            spotifyTrackId = spotifyTrackId,
                            previewUrl = previewUrl,
                            imageUrl = trackArtwork,
                            artwork = trackArtwork,
                            isrc = isrc
                        )
                    )
                }
            }

            if (tracks.isEmpty()) {
                return@withContext Result.failure(
                    Exception("This playlist appears to be empty or has no accessible tracks.")
                )
            }

            val total = tracks.size
            val isTruncated = total >= 100

            Result.success(
                PlaylistExtractResponse(
                    success = true,
                    playlist = ExtractedPlaylist(
                        name = playlistName,
                        image = coverImage,
                        spotifyUrl = cleanUrl,
                        description = description
                    ),
                    totalExtracted = total,
                    tracks = tracks,
                    truncated = isTruncated,
                    note = if (isTruncated) "Imported first 100 tracks from this playlist." else null
                )
            )
        } catch (e: Exception) {
            Log.e("PLAYLIST_SYNC", "Extraction failed", e)
            val userMsg = when {
                e is java.net.UnknownHostException -> "No internet connection available. Please check your network."
                e is java.net.SocketTimeoutException -> "Spotify took too long to respond. Please try again."
                e.message?.contains("is not a Json", ignoreCase = true) == true -> "Unable to extract this playlist. Please check the playlist link and try again."
                e.message?.contains("Serialization", ignoreCase = true) == true -> "Unable to extract this playlist. Please check the playlist link and try again."
                else -> e.localizedMessage ?: "Unable to extract this playlist. Please check the playlist link and try again."
            }
            Result.failure(Exception(userMsg))
        }
    }

    private fun fetchOEmbedFallback(playlistId: String, originalUrl: String): Result<PlaylistExtractResponse> {
        try {
            val oembedUrl = "https://open.spotify.com/oembed?url=https://open.spotify.com/playlist/$playlistId"
            val req = Request.Builder()
                .url(oembedUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return Result.failure(Exception("Unable to extract this playlist. Please check the playlist link and try again."))
                val body = resp.body?.string() ?: ""
                try {
                    Log.d("PLAYLIST_SYNC_RAW_RESPONSE", "OEMBED: $body")
                } catch (ignored: Throwable) {}
                val elem = json.parseToJsonElement(body).asJsonObjectOrNull
                val title = elem?.get("title").asStringOrNull ?: "Spotify Playlist"
                val thumbnail = elem?.get("thumbnail_url").asStringOrNull?.let { upgradeSpotifyImageUrl(it) }
                return Result.success(
                    PlaylistExtractResponse(
                        success = true,
                        playlist = ExtractedPlaylist(
                            name = title,
                            image = thumbnail,
                            spotifyUrl = originalUrl,
                            description = null
                        ),
                        totalExtracted = 0,
                        tracks = emptyList(),
                        error = ExtractedError(
                            code = "TRACKS_UNAVAILABLE",
                            message = "Playlist metadata loaded, but individual track extraction was unavailable."
                        )
                    )
                )
            }
        } catch (e: Exception) {
            return Result.failure(Exception("Unable to extract this playlist. Please check the playlist link and try again."))
        }
    }

    fun extractHighestQualityImage(element: JsonElement?): String? {
        if (element == null || element is JsonNull) return null

        // 1. Direct string primitive
        val directUrl = element.asStringOrNull
        if (!directUrl.isNullOrBlank()) {
            return upgradeSpotifyImageUrl(directUrl)
        }

        // 2. Object with sources, images, or image array / url property
        val obj = element.asJsonObjectOrNull
        if (obj != null) {
            val sourcesArray = obj["sources"].asJsonArrayOrNull
                ?: obj["images"].asJsonArrayOrNull
                ?: obj["image"].asJsonArrayOrNull
            if (sourcesArray != null && sourcesArray.isNotEmpty()) {
                val fromSources = extractFromSourcesArray(sourcesArray)
                if (fromSources != null) return fromSources
            }

            val directObjUrl = obj["url"].asStringOrNull ?: obj["src"].asStringOrNull
            if (!directObjUrl.isNullOrBlank()) {
                return upgradeSpotifyImageUrl(directObjUrl)
            }
        }

        // 3. Array of image objects or strings
        val arr = element.asJsonArrayOrNull
        if (arr != null && arr.isNotEmpty()) {
            val fromArray = extractFromSourcesArray(arr)
            if (fromArray != null) return fromArray
        }

        return null
    }

    private fun extractFromSourcesArray(array: JsonArray): String? {
        data class ImageSource(val url: String, val width: Int, val height: Int)
        val candidates = mutableListOf<ImageSource>()

        for (item in array) {
            val str = item.asStringOrNull
            if (!str.isNullOrBlank()) {
                candidates.add(ImageSource(upgradeSpotifyImageUrl(str), 0, 0))
                continue
            }
            val itemObj = item.asJsonObjectOrNull ?: continue
            val url = itemObj["url"].asStringOrNull ?: itemObj["src"].asStringOrNull ?: continue
            if (url.isBlank()) continue
            val width = itemObj["width"].asLongOrNull?.toInt() ?: 0
            val height = itemObj["height"].asLongOrNull?.toInt() ?: 0
            candidates.add(ImageSource(upgradeSpotifyImageUrl(url), width, height))
        }

        if (candidates.isEmpty()) return null

        // Prioritize highest dimensions / area
        return candidates.maxByOrNull { 
            val area = it.width * it.height
            if (area > 0) area else it.width.coerceAtLeast(it.height)
        }?.url ?: candidates.first().url
    }

    fun upgradeSpotifyImageUrl(url: String): String {
        var result = url.trim()
        if (result.contains("mosaic.scdn.co")) {
            result = result.replace(Regex("/(60|300)/"), "/640/")
        }
        if (result.contains("ab67616d00004851")) {
            result = result.replace("ab67616d00004851", "ab67616d0000b273")
        } else if (result.contains("ab67616d00001e02")) {
            result = result.replace("ab67616d00001e02", "ab67616d0000b273")
        } else if (result.contains("ab67706f00000003")) {
            result = result.replace("ab67706f00000003", "ab67706f00000002")
        }
        return result
    }
}
