package com.example

import androidx.compose.runtime.compositionLocalOf
import com.example.data.remote.models.TrackDto

val LocalShareProvider = compositionLocalOf<(TrackDto) -> Unit> { 
    { _ -> }
}
