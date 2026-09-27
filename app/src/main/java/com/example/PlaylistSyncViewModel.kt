package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.Playlist
import com.example.data.PlaylistRepository
import com.example.data.remote.TrackArtworkResolver
import com.example.data.remote.models.ExtractedTrack
import com.example.data.remote.models.ImagesDto
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlaylistSyncState {
    object Idle : PlaylistSyncState
    
    data class Syncing(
        val message: String = "Syncing playlist...",
        val processedCount: Int = 0,
        val totalCount: Int = 0
    ) : PlaylistSyncState
    
    data class Synced(
        val originalPlaylistName: String,
        val description: String?,
        val coverImage: String?,
        val tracks: List<TrackDto>,
        val syncedTrackCount: Int
    ) : PlaylistSyncState
    
    data class Creating(
        val originalPlaylistName: String,
        val syncedTrackCount: Int,
        val message: String = "Creating Playlist..."
    ) : PlaylistSyncState
    
    data class Success(
        val playlist: Playlist,
        val trackCount: Int,
        val message: String = "Playlist created successfully"
    ) : PlaylistSyncState
    
    data class Error(val message: String) : PlaylistSyncState
}

class PlaylistSyncViewModel(
    private val musicRepository: MusicRepository = MusicRepository()
) : ViewModel() {

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _syncState = MutableStateFlow<PlaylistSyncState>(PlaylistSyncState.Idle)
    val syncState: StateFlow<PlaylistSyncState> = _syncState.asStateFlow()

    private var syncJob: Job? = null
    private var createJob: Job? = null

    fun onUrlChange(newUrl: String) {
        _url.value = newUrl
        if (_syncState.value is PlaylistSyncState.Error || _syncState.value is PlaylistSyncState.Synced) {
            _syncState.value = PlaylistSyncState.Idle
        }
    }

    fun pasteFromClipboard(clipboardText: String) {
        val clean = clipboardText.trim()
        if (clean.isNotBlank()) {
            _url.value = clean
            if (_syncState.value is PlaylistSyncState.Error || _syncState.value is PlaylistSyncState.Synced) {
                _syncState.value = PlaylistSyncState.Idle
            }
        }
    }

    fun clearUrl() {
        _url.value = ""
        _syncState.value = PlaylistSyncState.Idle
    }

    fun dismissError() {
        if (_syncState.value is PlaylistSyncState.Error) {
            _syncState.value = PlaylistSyncState.Idle
        }
    }

    /**
     * Phase 1: Starts the real extraction/sync process for the given Spotify playlist URL.
     * Fetches metadata, cover image, and tracks with progress updates.
     * Transitions to [PlaylistSyncState.Synced] when complete.
     */
    fun clonePlaylist() {
        val input = _url.value.trim()

        if (input.isBlank()) {
            _syncState.value = PlaylistSyncState.Error("Please paste or enter a Spotify playlist link.")
            return
        }

        if (!input.contains("spotify.com") && !input.contains("spotify.link")) {
            _syncState.value = PlaylistSyncState.Error("Please enter a valid Spotify link (e.g. https://open.spotify.com/playlist/...).")
            return
        }

        if (!input.contains("/playlist/") && !input.contains("spotify.link")) {
            _syncState.value = PlaylistSyncState.Error("The provided link is not a Spotify playlist. Please check and retry.")
            return
        }

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _syncState.value = PlaylistSyncState.Syncing(
                message = "Syncing playlist...",
                processedCount = 0,
                totalCount = 0
            )

            val result = musicRepository.extractPlaylist(input) { current, total ->
                _syncState.value = PlaylistSyncState.Syncing(
                    message = "Syncing playlist...",
                    processedCount = current,
                    totalCount = total
                )
            }

            result.onSuccess { response ->
                val pl = response.playlist
                val originalTitle = pl?.name?.takeIf { it.isNotBlank() } ?: "Spotify Playlist"
                val desc = pl?.description?.takeIf { it.isNotBlank() }
                val cover = pl?.image

                // Map tracks preserving their individual artwork
                val rawTrackDtos = response.tracks.mapIndexed { idx, t ->
                    t.toTrackDto(idx)
                }

                if (rawTrackDtos.isEmpty()) {
                    _syncState.value = PlaylistSyncState.Error("This playlist has no accessible tracks or is private.")
                    return@onSuccess
                }

                // Resolve any missing track artworks using the strict authentic music resolver
                val resolvedTrackDtos = TrackArtworkResolver.resolveTrackListArtworks(rawTrackDtos) { current, total ->
                    _syncState.value = PlaylistSyncState.Syncing(
                        message = "Syncing playlist...",
                        processedCount = current,
                        totalCount = total
                    )
                }

                // Transition to Synced state with resolved authentic track artworks - user can now review and tap "Create Playlist"
                _syncState.value = PlaylistSyncState.Synced(
                    originalPlaylistName = originalTitle,
                    description = desc,
                    coverImage = cover,
                    tracks = resolvedTrackDtos,
                    syncedTrackCount = resolvedTrackDtos.size
                )
            }.onFailure { err ->
                val rawMsg = err.localizedMessage ?: ""
                val cleanMsg = when {
                    rawMsg.contains("is not a Json", ignoreCase = true) ||
                    rawMsg.contains("serialization", ignoreCase = true) ||
                    rawMsg.contains("NullPointerException", ignoreCase = true) ||
                    rawMsg.contains("IndexOutOfBounds", ignoreCase = true) ||
                    rawMsg.isBlank() -> "Unable to extract this playlist. Please check the playlist link and try again."
                    else -> rawMsg
                }
                _syncState.value = PlaylistSyncState.Error(cleanMsg)
            }
        }
    }

    /**
     * Phase 2: Creates the new playlist in the user's Library using the exact original Spotify name
     * and adding all successfully synced tracks. Protected against duplicate taps.
     */
    fun createPlaylist(onSuccessCallback: ((Playlist) -> Unit)? = null) {
        val currentState = _syncState.value
        if (currentState !is PlaylistSyncState.Synced) {
            return
        }

        if (createJob?.isActive == true) {
            return
        }

        createJob = viewModelScope.launch {
            _syncState.value = PlaylistSyncState.Creating(
                originalPlaylistName = currentState.originalPlaylistName,
                syncedTrackCount = currentState.syncedTrackCount,
                message = "Creating Playlist..."
            )

            val savedPlaylist = PlaylistRepository.importExtractedPlaylist(
                name = currentState.originalPlaylistName,
                description = currentState.description ?: "Synced from Spotify",
                coverImage = currentState.coverImage,
                tracks = currentState.tracks
            )

            _syncState.value = PlaylistSyncState.Success(
                playlist = savedPlaylist,
                trackCount = currentState.tracks.size,
                message = "Playlist created successfully"
            )

            onSuccessCallback?.invoke(savedPlaylist)
        }
    }

    fun reset() {
        _url.value = ""
        _syncState.value = PlaylistSyncState.Idle
    }

    private fun ExtractedTrack.toTrackDto(index: Int): TrackDto {
        val trackId = spotifyTrackId?.takeIf { it.isNotBlank() }
            ?: spotifyUri?.removePrefix("spotify:track:")?.takeIf { it.isNotBlank() }
            ?: "cloned_$index"
        val individualArt = getEffectiveArtwork()
        return TrackDto(
            id = trackId,
            title = name,
            artist = artist,
            album = album,
            duration = (duration / 1000).toInt(),
            images = if (!individualArt.isNullOrBlank()) ImagesDto(small = individualArt, medium = individualArt, large = individualArt) else null,
            spotifyId = trackId,
            spotifyUri = spotifyUri ?: "spotify:track:$trackId",
            streamUrl = previewUrl
        )
    }

    override fun onCleared() {
        super.onCleared()
        syncJob?.cancel()
        createJob?.cancel()
    }
}
