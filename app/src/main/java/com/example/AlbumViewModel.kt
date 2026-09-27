package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.remote.models.AlbumDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AlbumUiState {
    object Loading : AlbumUiState()
    data class Success(val album: AlbumDto) : AlbumUiState()
    data class Error(val message: String) : AlbumUiState()
}

class AlbumViewModel : ViewModel() {
    private val repository = MusicRepository()

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun loadAlbum(id: String) {
        _uiState.value = AlbumUiState.Loading
        viewModelScope.launch {
            try {
                val album = repository.getAlbum(id)
                if (album != null) {
                    _uiState.value = AlbumUiState.Success(album)
                } else {
                    _uiState.value = AlbumUiState.Error("Album not found")
                }
            } catch (e: Exception) {
                _uiState.value = AlbumUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
