package com.example.data

import com.example.data.remote.ApiClient
import com.example.data.remote.SpotifyNativeExtractor
import com.example.data.remote.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository {
    private val api = ApiClient.apiService

    companion object {
        // Cache to store artist data from search results so we can display real names and images
        // when navigating to ArtistScreen with a spotify-artist ID.
        val artistCache = mutableMapOf<String, ArtistDto>()
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

    suspend fun search(query: String): SearchDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.search(query)
                if (response.isSuccessful) {
                    response.body()?.data
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getSearchSuggestions(query: String): List<SearchSuggestionDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getSearchSuggestions(query)
                if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
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
