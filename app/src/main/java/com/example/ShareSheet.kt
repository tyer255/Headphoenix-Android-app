package com.example

import androidx.compose.runtime.Composable
import com.example.data.remote.models.TrackDto
import com.example.ui.TrackShareBottomSheet

@Composable
fun ShareSheet(
    track: TrackDto?,
    onDismiss: () -> Unit
) {
    if (track == null) return
    TrackShareBottomSheet(
        track = track,
        onDismissRequest = onDismiss
    )
}
