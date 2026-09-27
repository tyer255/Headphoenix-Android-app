package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.PlaylistRepository
import com.example.data.remote.models.ExtractedTrack
import com.example.data.remote.models.ImagesDto
import com.example.data.remote.models.PlaylistExtractResponse
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PlaylistExtractorUiState {
    object Idle : PlaylistExtractorUiState
    data class Loading(val message: String = "Connecting to Spotify & extracting playlist...") : PlaylistExtractorUiState
    data class Success(val response: PlaylistExtractResponse) : PlaylistExtractorUiState
    data class Error(val message: String) : PlaylistExtractorUiState
}

class PlaylistExtractorViewModel(
    private val musicRepository: MusicRepository = MusicRepository()
) : ViewModel() {

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _uiState = MutableStateFlow<PlaylistExtractorUiState>(PlaylistExtractorUiState.Idle)
    val uiState: StateFlow<PlaylistExtractorUiState> = _uiState.asStateFlow()

    private val _importSuccessMessage = MutableStateFlow<String?>(null)
    val importSuccessMessage: StateFlow<String?> = _importSuccessMessage.asStateFlow()

    private var extractJob: Job? = null

    fun onUrlChange(newUrl: String) {
        _url.value = newUrl
        if (_uiState.value is PlaylistExtractorUiState.Error) {
            _uiState.value = PlaylistExtractorUiState.Idle
        }
    }

    fun clearUrl() {
        _url.value = ""
        _uiState.value = PlaylistExtractorUiState.Idle
    }

    fun pasteFromClipboard(clipboardText: String) {
        if (clipboardText.isNotBlank()) {
            _url.value = clipboardText.trim()
            if (_uiState.value is PlaylistExtractorUiState.Error) {
                _uiState.value = PlaylistExtractorUiState.Idle
            }
        }
    }

    fun extractPlaylist() {
        val inputUrl = _url.value.trim()
        if (inputUrl.isBlank()) {
            _uiState.value = PlaylistExtractorUiState.Error("Please enter or paste a Spotify playlist URL.")
            return
        }

        // Basic client-side pre-validation
        if (!inputUrl.contains("spotify.com") && !inputUrl.contains("spotify.link")) {
            _uiState.value = PlaylistExtractorUiState.Error("Please enter a valid Spotify URL (e.g. https://open.spotify.com/playlist/...).")
            return
        }

        if (!inputUrl.contains("/playlist/") && !inputUrl.contains("spotify.link")) {
            _uiState.value = PlaylistExtractorUiState.Error("The provided URL is not a Spotify playlist. Please provide a playlist URL.")
            return
        }

        // Cancel previous extraction job if still running
        extractJob?.cancel()

        extractJob = viewModelScope.launch {
            _uiState.value = PlaylistExtractorUiState.Loading("Extracting playlist metadata and tracks...")
            val result = musicRepository.extractPlaylist(inputUrl)
            result.onSuccess { response ->
                _uiState.value = PlaylistExtractorUiState.Success(response)
            }.onFailure { error ->
                val rawMsg = error.localizedMessage ?: ""
                val cleanMsg = when {
                    rawMsg.contains("is not a Json", ignoreCase = true) ||
                    rawMsg.contains("serialization", ignoreCase = true) ||
                    rawMsg.contains("NullPointerException", ignoreCase = true) ||
                    rawMsg.contains("IndexOutOfBounds", ignoreCase = true) ||
                    rawMsg.isBlank() -> "Unable to extract this playlist. Please check the playlist link and try again."
                    else -> rawMsg
                }
                _uiState.value = PlaylistExtractorUiState.Error(cleanMsg)
            }
        }
    }

    fun importToLibrary(onImported: (String) -> Unit) {
        val currentState = _uiState.value
        if (currentState is PlaylistExtractorUiState.Success) {
            val pl = currentState.response.playlist
            val title = pl?.name ?: "Imported Playlist"
            val desc = pl?.description ?: "Extracted from Spotify"
            val cover = pl?.image

            val trackDtos = currentState.response.tracks.mapIndexed { index, track ->
                track.toTrackDto(index)
            }

            val created = PlaylistRepository.importExtractedPlaylist(
                name = title,
                description = desc,
                coverImage = cover,
                tracks = trackDtos
            )

            _importSuccessMessage.value = "Saved \"$title\" with ${trackDtos.size} tracks to Your Library!"
            onImported(created.id)
        }
    }

    fun clearImportMessage() {
        _importSuccessMessage.value = null
    }

    fun dismissError() {
        _uiState.value = PlaylistExtractorUiState.Idle
    }

    fun ExtractedTrack.toTrackDto(index: Int): TrackDto {
        val trackId = spotifyTrackId?.takeIf { it.isNotBlank() }
            ?: spotifyUri?.removePrefix("spotify:track:")?.takeIf { it.isNotBlank() }
            ?: "extracted_$index"
        val art = getEffectiveArtwork()
        return TrackDto(
            id = trackId,
            title = name,
            artist = artist,
            album = album,
            duration = (duration / 1000).toInt(),
            images = if (!art.isNullOrBlank()) ImagesDto(small = art, medium = art, large = art) else null,
            spotifyId = trackId,
            spotifyUri = spotifyUri ?: "spotify:track:$trackId",
            streamUrl = previewUrl
        )
    }

    override fun onCleared() {
        super.onCleared()
        extractJob?.cancel()
    }
}
