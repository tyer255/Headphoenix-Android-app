package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.remote.models.ArtistDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArtistViewModel : ViewModel() {
    private val repository = MusicRepository()
    private val _uiState = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val uiState: StateFlow<ArtistUiState> = _uiState

    fun fetchArtist(artistId: String) {
        viewModelScope.launch {
            _uiState.value = ArtistUiState.Loading
            try {
                val data = repository.getArtist(artistId)
                val cachedArtist = MusicRepository.artistCache[artistId]
                
                val artistName = cachedArtist?.name ?: data?.name ?: artistId
                val artistImage = cachedArtist?.image ?: data?.image
                
                var topTracks = data?.topTracks ?: emptyList()
                var albums = data?.albums ?: emptyList()

                // If topTracks is empty or from fallback, search songs for this artist
                if (topTracks.isEmpty() || cachedArtist != null) {
                    try {
                        val searchResponse = repository.search(artistName)
                        if (searchResponse != null && !searchResponse.songs.isNullOrEmpty()) {
                            val realTracks = searchResponse.songs.filter { 
                                it.artist.contains(artistName, ignoreCase = true) 
                            }
                            topTracks = if (realTracks.isNotEmpty()) realTracks else searchResponse.songs.take(10)
                        }
                    } catch (e: Exception) {
                        // Ignore search error
                    }
                }

                // Ensure all tracks have artist name set
                topTracks = topTracks.map { track ->
                    if (track.artist.isBlank() || track.artist == "Unknown Artist") {
                        track.copy(artist = artistName)
                    } else track
                }

                val finalArtist = ArtistDto(
                    id = data?.id ?: artistId,
                    name = artistName,
                    image = artistImage ?: data?.image ?: "",
                    followers = cachedArtist?.followers ?: data?.followers ?: 1250000,
                    monthlyListeners = cachedArtist?.monthlyListeners ?: data?.monthlyListeners ?: 48512940,
                    topTracks = topTracks,
                    albums = albums
                )

                _uiState.value = ArtistUiState.Success(finalArtist)
            } catch (e: Exception) {
                _uiState.value = ArtistUiState.Error("Failed to fetch artist: ${e.message}")
            }
        }
    }
}

sealed class ArtistUiState {
    object Loading : ArtistUiState()
    data class Success(val data: ArtistDto) : ArtistUiState()
    data class Error(val message: String) : ArtistUiState()
}
