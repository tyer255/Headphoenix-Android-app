package com.example

import androidx.compose.runtime.compositionLocalOf
import com.example.data.remote.models.TrackDto

data class TrackMenuState(
    val track: TrackDto,
    val onRemoveFromPlaylist: (() -> Unit)? = null
)

val LocalTrackMenuProvider = compositionLocalOf<(TrackMenuState) -> Unit> { 
    error("No TrackMenu provider found!") 
}
