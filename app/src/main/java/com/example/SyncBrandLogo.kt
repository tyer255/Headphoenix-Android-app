package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modular brand logo component for Playlist Sync.
 * By isolating this into its own component and referencing R.drawable.ic_spotify,
 * replacing the Spotify logo with another service/brand in the future requires
 * updating only this asset/component without touching the screen UI.
 */
@Composable
fun SyncBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = Color(0xFF1ED760)
) {
    Image(
        painter = painterResource(id = R.drawable.ic_spotify),
        contentDescription = "Sync Service Brand Logo",
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}
