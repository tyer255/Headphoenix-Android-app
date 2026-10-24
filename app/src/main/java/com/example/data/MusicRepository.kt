package com.example.data

import com.example.data.remote.ApiClient
import com.example.data.remote.SpotifyNativeExtractor
import com.example.data.remote.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

class MusicRepository {
    private val api = ApiClient.apiService

    companion object {
        // Cache to store artist data from search results so we can display real names and images
        // when navigating to ArtistScreen with a spotify-artist ID.
        val artistCache = mutableMapOf<String, ArtistDto>()
        val searchCache = ConcurrentHashMap<String, SearchDto>()
    }

    suspend fun checkHealth(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.checkHealth()
                response.isSuccessful && response.body()?.data?.status == "healthy"
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun getHome(): HomeDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getHome()
                if (response.isSuccessful) {
                    response.body()?.data
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun search(query: String): SearchDto? = withContext(Dispatchers.IO) {
        val key = query.trim().lowercase()
        if (key.isEmpty()) return@withContext null
        searchCache[key]?.let { return@withContext it }

        coroutineScope {
            val backendDeferred = async {
                withTimeoutOrNull(3200L) {
                    try {
                        val response = api.search(query)
                        if (response.isSuccessful) response.body()?.data else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            val saavnDeferred = async {
                withTimeoutOrNull(2500L) {
                    fetchFastSaavnTracks(query)
                } ?: emptyList()
            }

            val itunesDeferred = async {
                withTimeoutOrNull(2500L) {
                    fetchFastItunesTracks(query)
                } ?: emptyList()
            }

            val backendRes = backendDeferred.await()
            val saavnSongs = saavnDeferred.await()
            val itunesSongs = itunesDeferred.await()

            val mergedSongs = ((backendRes?.songs ?: emptyList()) + saavnSongs + itunesSongs)
                .distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }

            val mergedArtists = if (!backendRes?.artists.isNullOrEmpty()) {
                backendRes!!.artists
            } else {
                mergedSongs.map { song ->
                    ArtistDto(
                        id = "artist_${song.artist.lowercase().replace(" ", "_")}",
                        name = song.artist.split(",").firstOrNull()?.trim() ?: song.artist,
                        image = song.images?.large ?: song.images?.medium
                    )
                }.distinctBy { it.name.lowercase() }.take(6)
            }

            val finalResult = SearchDto(
                songs = mergedSongs,
                artists = mergedArtists,
                albums = backendRes?.albums ?: emptyList(),
                playlists = backendRes?.playlists ?: emptyList()
            )

            if (mergedSongs.isNotEmpty()) {
                searchCache[key] = finalResult
            }
            finalResult
        }
    }

    private fun fetchFastSaavnTracks(query: String): List<TrackDto> {
        val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (_: Exception) { return emptyList() }
        val req = Request.Builder()
            .url("https://saavn.dev/api/search/songs?query=$encoded&limit=12")
            .header("User-Agent", CookieRepository.getUserAgent())
            .build()
        return try {
            ApiClient.fastHttpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return emptyList()
                val body = res.body?.string() ?: return emptyList()
                val root = JSONObject(body)
                val results = root.optJSONObject("data")?.optJSONArray("results") ?: return emptyList()
                val list = mutableListOf<TrackDto>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val id = item.optString("id", "saavn_$i")
                    val title = item.optString("name", "").replace("&quot;", "\"").replace("&amp;", "&")
                    val artistsArr = item.optJSONObject("artists")?.optJSONArray("primary")
                    val artistName = if (artistsArr != null && artistsArr.length() > 0) {
                        artistsArr.getJSONObject(0).optString("name", "Unknown Artist")
                    } else {
                        item.optString("primaryArtists", "Unknown Artist")
                    }
                    val duration = item.optInt("duration", 200)
                    val imgArr = item.optJSONArray("image")
                    val imgUrl = if (imgArr != null && imgArr.length() > 0) {
                        imgArr.getJSONObject(imgArr.length() - 1).optString("url")
                    } else ""
                    val dlArr = item.optJSONArray("downloadUrl")
                    val streamUrl = if (dlArr != null && dlArr.length() > 0) {
                        dlArr.getJSONObject(dlArr.length() - 1).optString("url")
                    } else null

                    if (title.isNotBlank()) {
                        list.add(
                            TrackDto(
                                id = "saavn_$id",
                                title = title,
                                artist = artistName,
                                album = item.optJSONObject("album")?.optString("name"),
                                duration = duration,
                                streamUrl = streamUrl,
                                images = ImagesDto(large = imgUrl, medium = imgUrl, small = imgUrl)
                            )
                        )
                    }
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchFastItunesTracks(query: String): List<TrackDto> {
        val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (_: Exception) { return emptyList() }
        val req = Request.Builder()
            .url("https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=10")
            .header("User-Agent", CookieRepository.getUserAgent())
            .build()
        return try {
            ApiClient.fastHttpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return emptyList()
                val body = res.body?.string() ?: return emptyList()
                val root = JSONObject(body)
                val results = root.optJSONArray("results") ?: return emptyList()
                val list = mutableListOf<TrackDto>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val id = item.optLong("trackId", i.toLong()).toString()
                    val title = item.optString("trackName", "")
                    val artist = item.optString("artistName", "")
                    val album = item.optString("collectionName", "")
                    val art100 = item.optString("artworkUrl100", "")
                    val artLarge = art100.replace("100x100bb", "600x600bb")
                    val previewUrl = item.optString("previewUrl", "").takeIf { it.isNotBlank() }
                    val durationSec = (item.optLong("trackTimeMillis", 200000L) / 1000L).toInt()
                    if (title.isNotBlank()) {
                        list.add(
                            TrackDto(
                                id = "itunes_$id",
                                title = title,
                                artist = artist,
                                album = album,
                                duration = durationSec,
                                streamUrl = previewUrl,
                                images = ImagesDto(large = artLarge, medium = artLarge, small = art100)
                            )
                        )
                    }
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getSearchSuggestions(query: String): List<SearchSuggestionDto> {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(1800L) {
                try {
                    val response = api.getSearchSuggestions(query)
                    if (response.isSuccessful) {
                        response.body()?.data ?: emptyList()
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    }

    suspend fun getTrack(id: String): TrackDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getTrack(id)
                if (response.isSuccessful) {
                    response.body()?.data
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getArtist(id: String): ArtistDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getArtist(id)
                if (response.isSuccessful) response.body()?.data else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getAlbum(id: String): AlbumDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getAlbum(id)
                if (response.isSuccessful) {
                    response.body()?.data
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getCanvas(id: String, track: TrackDto? = null): CanvasDto? {
        return withContext(Dispatchers.IO) {
            try {
                if (track != null) {
                    val resolvedUrl = com.example.data.remote.CanvasResolver.resolveCanvas(track)
                    if (!resolvedUrl.isNullOrBlank()) {
                        return@withContext CanvasDto(requestedTrackId = id, canvasUrl = resolvedUrl, videoUrl = resolvedUrl)
                    }
                }
                val response = api.getCanvas(id)
                if (response.isSuccessful && response.body()?.data != null) {
                    response.body()?.data
                } else if (track != null) {
                    val fallbackUrl = com.example.data.remote.CanvasResolver.resolveCanvas(track)
                    fallbackUrl?.let { CanvasDto(requestedTrackId = id, canvasUrl = it, videoUrl = it) }
                } else null
            } catch (e: Exception) {
                if (track != null) {
                    val fallbackUrl = com.example.data.remote.CanvasResolver.resolveCanvas(track)
                    fallbackUrl?.let { CanvasDto(requestedTrackId = id, canvasUrl = it, videoUrl = it) }
                } else null
            }
        }
    }

    suspend fun getRecommendations(trackId: String, title: String? = null, artist: String? = null): List<TrackDto> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<TrackDto>()
            try {
                // 1. If artist or title is provided, search songs
                val query = if (!artist.isNullOrBlank()) artist else title
                if (!query.isNullOrBlank()) {
                    val searchResult = search(query)
                    val foundTracks = searchResult?.songs ?: emptyList()
                    results.addAll(foundTracks.filter { it.id != trackId })
                }

                // 2. If results are still few, fetch popular songs / quick picks from Home
                if (results.size < 5) {
                    val homeData = getHome()
                    val homeTracks = (homeData?.quickPicks ?: emptyList()) +
                            (homeData?.popularSongs ?: emptyList()) +
                            (homeData?.trending ?: emptyList()) +
                            (homeData?.madeForYou ?: emptyList())
                    results.addAll(homeTracks.filter { it.id != trackId })
                }
            } catch (e: Exception) {
                // Ignore error and return accumulated results
            }
            results.distinctBy { it.id }
        }
    }

    suspend fun getPlaylist(id: String): PlaylistDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getPlaylist(id)
                if (response.isSuccessful) {
                    response.body()?.data
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getLyrics(id: String, title: String? = null, artist: String? = null): LyricsDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getLyrics(id = id, title = title, artist = artist)
                if (response.isSuccessful) {
                    response.body()?.data
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getRadio(seedId: String, seedTitle: String? = null): List<TrackDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getRadio(seedType = "song", seedId = seedId, seedTitle = seedTitle)
                if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun resolvePlayback(trackId: String, title: String? = null, artist: String? = null, duration: Int? = null): PlaybackResolveDataDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.resolvePlayback(PlaybackResolveRequest(trackId, title, artist, duration))
                if (response.isSuccessful) {
                    response.body()?.data
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun extractPlaylist(
        url: String,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<PlaylistExtractResponse> {
        return withContext(Dispatchers.IO) {
            // First attempt: High-speed native extraction directly from public Spotify embed
            val nativeResult = SpotifyNativeExtractor.extract(url, onProgress)
            if (nativeResult.isSuccess) {
                return@withContext nativeResult
            }

            // Fallback: Attempt remote API if available
            try {
                val response = api.extractPlaylist(PlaylistExtractRequest(url = url.trim()))
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Result.success(body)
                    } else {
                        nativeResult
                    }
                } else {
                    nativeResult
                }
            } catch (e: Exception) {
                nativeResult
            }
        }
    }
}
