package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object PlaylistRepository {

    private const val TAG = "PlaylistRepository"
    private const val FILE_PLAYLISTS = "user_playlists.json"
    private const val FILE_LIKED_TRACKS = "liked_tracks.json"
    private const val FILE_USER_PROFILE = "user_profile.json"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        isLenient = true
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var appContext: Context? = null
    private var isInitialized = false

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _likedTracks = MutableStateFlow<List<TrackDto>>(emptyList())
    val likedTracks: StateFlow<List<TrackDto>> = _likedTracks.asStateFlow()

    private val _userProfile = MutableStateFlow(
        UserProfile(
            id = "u1",
            name = "MRIDUL NAREDA",
            email = "mridul@spotiz.com",
            product = "Premium",
            followers = 24,
            following = 18
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun init(context: Context) {
        if (isInitialized && appContext != null) return
        appContext = context.applicationContext
        loadFromStorage()
        isInitialized = true
    }

    private fun loadFromStorage() {
        val ctx = appContext ?: return
        try {
            // 1. Load User Playlists
            val playlistsFile = File(ctx.filesDir, FILE_PLAYLISTS)
            if (playlistsFile.exists()) {
                val content = playlistsFile.readText()
                if (content.isNotBlank()) {
                    val loadedPlaylists = json.decodeFromString<List<Playlist>>(content)
                    if (loadedPlaylists.isNotEmpty()) {
                        _playlists.value = loadedPlaylists
                        Log.d(TAG, "Loaded ${loadedPlaylists.size} playlists from internal storage.")
                    }
                }
            }

            // Seed default "My top tracks playlist" from screenshot if empty
            if (_playlists.value.isEmpty()) {
                val defaultPlaylist = Playlist(
                    id = "my_top_tracks_playlist",
                    title = "My top tracks playlist",
                    description = "Playlist created by the tutorial on developer.spotify.com",
                    coverImage = "https://i.scdn.co/image/ab67616d0000b27389886a11e86095fb8c9f52f4",
                    trackCount = 2,
                    owner = "MRIDUL NAREDA",
                    tracks = listOf(
                        TrackDto(
                            id = "shikayat_aur",
                            title = "Shikayat",
                            artist = "AUR",
                            images = com.example.data.remote.models.ImagesDto(
                                small = "https://i.scdn.co/image/ab67616d0000b27389886a11e86095fb8c9f52f4",
                                medium = "https://i.scdn.co/image/ab67616d0000b27389886a11e86095fb8c9f52f4",
                                large = "https://i.scdn.co/image/ab67616d0000b27389886a11e86095fb8c9f52f4"
                            ),
                            duration = 210
                        ),
                        TrackDto(
                            id = "bilionera_otilia",
                            title = "Bilionera",
                            artist = "Otilia",
                            images = com.example.data.remote.models.ImagesDto(
                                small = "https://i.scdn.co/image/ab67616d0000b273614ba6924df02fd165507ee7",
                                medium = "https://i.scdn.co/image/ab67616d0000b273614ba6924df02fd165507ee7",
                                large = "https://i.scdn.co/image/ab67616d0000b273614ba6924df02fd165507ee7"
                            ),
                            duration = 185
                        )
                    ),
                    colorHex = 0xFF8B0000,
                    isCustom = true,
                    isPublic = false
                )
                _playlists.value = listOf(defaultPlaylist)
                persistPlaylists()
            }

            // 2. Load Liked Tracks
            val likedFile = File(ctx.filesDir, FILE_LIKED_TRACKS)
            if (likedFile.exists()) {
                val content = likedFile.readText()
                if (content.isNotBlank()) {
                    val loadedLiked = json.decodeFromString<List<TrackDto>>(content)
                    _likedTracks.value = loadedLiked
                    Log.d(TAG, "Loaded ${loadedLiked.size} liked tracks from internal storage.")
                }
            }

            // 3. Load User Profile
            val profileFile = File(ctx.filesDir, FILE_USER_PROFILE)
            if (profileFile.exists()) {
                val content = profileFile.readText()
                if (content.isNotBlank()) {
                    val loadedProfile = json.decodeFromString<UserProfile>(content)
                    _userProfile.value = loadedProfile
                    Log.d(TAG, "Loaded user profile for ${loadedProfile.name}.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading playlists/liked data from internal storage", e)
        }
    }

    private fun persistPlaylists() {
        val ctx = appContext ?: return
        val current = _playlists.value
        scope.launch {
            try {
                val jsonStr = json.encodeToString(current)
                val targetFile = File(ctx.filesDir, FILE_PLAYLISTS)
                targetFile.writeText(jsonStr)
                Log.d(TAG, "Saved ${current.size} playlists to internal storage.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist playlists to internal storage", e)
            }
        }
    }

    private fun persistLikedTracks() {
        val ctx = appContext ?: return
        val current = _likedTracks.value
        scope.launch {
            try {
                val jsonStr = json.encodeToString(current)
                val targetFile = File(ctx.filesDir, FILE_LIKED_TRACKS)
                val tempFile = File(ctx.filesDir, "$FILE_LIKED_TRACKS.tmp")
                tempFile.writeText(jsonStr)
                if (tempFile.renameTo(targetFile) || (!targetFile.exists() && tempFile.renameTo(targetFile))) {
                    Log.d(TAG, "Saved ${current.size} liked tracks to internal storage.")
                } else {
                    targetFile.writeText(jsonStr)
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist liked tracks to internal storage", e)
            }
        }
    }

    private fun persistUserProfile() {
        val ctx = appContext ?: return
        val current = _userProfile.value
        scope.launch {
            try {
                val jsonStr = json.encodeToString(current)
                val targetFile = File(ctx.filesDir, FILE_USER_PROFILE)
                targetFile.writeText(jsonStr)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist user profile to internal storage", e)
            }
        }
    }

    fun getPlaylistById(id: String): Playlist? {
        if (id == "liked_songs" || id == "liked-songs" || id == "liked") {
            val tracks = _likedTracks.value
            return Playlist(
                id = "liked-songs",
                title = "Liked Songs",
                description = "Your personal collection of loved tracks on Spotify.",
                coverImage = null,
                trackCount = tracks.size,
                owner = _userProfile.value.name,
                tracks = tracks,
                colorHex = 0xFF5E35B1,
                isCustom = false
            )
        }
        if (id == "downloaded" || id == "downloaded-tracks") {
            val ctx = appContext
            val tracks = if (ctx != null) AppDownloadManager.getDownloadedTracks(ctx) else AppDownloadManager.downloadedTracks.value
            return Playlist(
                id = "downloaded-tracks",
                title = "Downloaded Songs",
                description = "Your offline available tracks",
                coverImage = null,
                trackCount = tracks.size,
                owner = "You",
                tracks = tracks,
                colorHex = 0xFF1E3A8A,
                isCustom = false
            )
        }
        return _playlists.value.find { it.id == id }
    }

    fun toggleLikeTrack(track: TrackDto) {
        val current = _likedTracks.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == track.id }
        if (existingIndex != -1) {
            current.removeAt(existingIndex)
        } else {
            current.add(0, track)
        }
        _likedTracks.value = current
        persistLikedTracks()
    }

    fun isTrackLiked(trackId: String): Boolean {
        return _likedTracks.value.any { it.id == trackId }
    }

    fun createPlaylist(name: String, description: String = ""): Playlist {
        val id = "user_pl_" + System.currentTimeMillis()
        val newPlaylist = Playlist(
            id = id,
            title = if (name.isBlank()) "My Playlist #${_playlists.value.count { it.isCustom } + 1}" else name.trim(),
            description = if (description.isBlank()) "Created by ${_userProfile.value.name}" else description.trim(),
            coverImage = null,
            trackCount = 0,
            owner = _userProfile.value.name,
            tracks = emptyList(),
            colorHex = 0xFF607D8B,
            isCustom = true
        )
        _playlists.value = listOf(newPlaylist) + _playlists.value
        persistPlaylists()
        return newPlaylist
    }

    fun importExtractedPlaylist(name: String, description: String = "", coverImage: String? = null, tracks: List<TrackDto>): Playlist {
        val cleanTitle = if (name.isBlank()) "Imported Playlist" else name.trim()
        val id = "imported_pl_" + System.currentTimeMillis()
        val newPlaylist = Playlist(
            id = id,
            title = cleanTitle,
            description = if (description.isBlank()) "Imported via Spotify Extractor" else description.trim(),
            coverImage = coverImage,
            trackCount = tracks.size,
            owner = _userProfile.value.name,
            tracks = tracks,
            colorHex = 0xFF1DB954,
            isCustom = true
        )
        // If an imported playlist with identical title exists, remove old copy to avoid stale duplicates
        val filtered = _playlists.value.filterNot { it.id.startsWith("imported_pl_") && it.title.equals(cleanTitle, ignoreCase = true) }
        _playlists.value = listOf(newPlaylist) + filtered
        persistPlaylists()
        return newPlaylist
    }

    fun addTrackToPlaylist(playlistId: String, track: TrackDto) {
        var changed = false
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (!pl.tracks.any { it.id == track.id }) {
                    changed = true
                    val updatedTracks = pl.tracks + track
                    pl.copy(tracks = updatedTracks, trackCount = updatedTracks.size)
                } else pl
            } else pl
        }
        if (changed) {
            persistPlaylists()
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        var changed = false
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                changed = true
                val updatedTracks = pl.tracks.filterNot { it.id == trackId }
                pl.copy(tracks = updatedTracks, trackCount = updatedTracks.size)
            } else pl
        }
        if (changed) {
            persistPlaylists()
        }
    }

    fun deletePlaylist(playlistId: String) {
        val before = _playlists.value.size
        _playlists.value = _playlists.value.filterNot { it.id == playlistId }
        if (_playlists.value.size != before) {
            persistPlaylists()
        }
    }

    fun renamePlaylist(playlistId: String, newName: String, newDescription: String) {
        var changed = false
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                changed = true
                pl.copy(
                    title = newName.ifBlank { pl.title },
                    description = newDescription.ifBlank { pl.description }
                )
            } else pl
        }
        if (changed) {
            persistPlaylists()
        }
    }

    fun updatePlaylistDetails(playlistId: String, name: String, description: String, isPublic: Boolean) {
        var changed = false
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                changed = true
                pl.copy(
                    title = name.ifBlank { pl.title },
                    description = description,
                    isPublic = isPublic
                )
            } else pl
        }
        if (changed) {
            persistPlaylists()
        }
    }

    fun updatePlaylistCover(playlistId: String, coverImage: String?) {
        var changed = false
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                changed = true
                pl.copy(coverImage = coverImage)
            } else pl
        }
        if (changed) {
            persistPlaylists()
        }
    }

    fun reorderTracks(playlistId: String, newTracks: List<TrackDto>) {
        var changed = false
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                changed = true
                pl.copy(tracks = newTracks, trackCount = newTracks.size)
            } else pl
        }
        if (changed) {
            persistPlaylists()
        }
    }

    fun updateUserProfile(name: String? = null, product: String? = null, avatarUrl: String? = null) {
        val current = _userProfile.value
        _userProfile.value = current.copy(
            name = name ?: current.name,
            product = product ?: current.product,
            avatarUrl = avatarUrl ?: current.avatarUrl
        )
        persistUserProfile()
    }
}
