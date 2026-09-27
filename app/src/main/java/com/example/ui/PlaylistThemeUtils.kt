package com.example.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

object PlaylistColorCache {
    private val colorMap = mutableMapOf<String, Color>()

    fun getColor(id: String): Color? = colorMap[id]
    fun setColor(id: String, color: Color) { colorMap[id] = color }
}

@Composable
fun rememberPlaylistThemeColor(playlist: Playlist?): Color {
    val context = LocalContext.current
    val playlistId = playlist?.id ?: "unknown"

    val fallbackColor = remember(playlistId, playlist?.colorHex) {
        if (playlist?.colorHex != null && playlist.colorHex != 0L) {
            Color(playlist.colorHex)
        } else {
            val hash = abs(playlistId.hashCode())
            val defaultPalettes = listOf(
                Color(0xFF8B1818), // Deep Red (like Shikayat)
                Color(0xFF1E3A8A), // Deep Blue
                Color(0xFF135D54), // Teal
                Color(0xFF5B21B6), // Purple
                Color(0xFF78350F), // Warm Brown/Amber
                Color(0xFF1F4E38), // Forest Green
                Color(0xFF831843)  // Burgundy
            )
            defaultPalettes[hash % defaultPalettes.size]
        }
    }

    val themeColorState = remember(playlistId) {
        mutableStateOf(PlaylistColorCache.getColor(playlistId) ?: fallbackColor)
    }

    val imageUrl = remember(playlist) {
        playlist?.coverImage
            ?: playlist?.tracks?.firstOrNull()?.images?.large
            ?: playlist?.tracks?.firstOrNull()?.images?.medium
            ?: playlist?.tracks?.firstOrNull()?.images?.small
    }

    LaunchedEffect(playlistId, imageUrl) {
        if (PlaylistColorCache.getColor(playlistId) != null) {
            themeColorState.value = PlaylistColorCache.getColor(playlistId)!!
            return@LaunchedEffect
        }

        if (!imageUrl.isNullOrBlank()) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .size(200)
                    .build()

                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).generate()
                    }

                    val swatch = palette.dominantSwatch
                        ?: palette.vibrantSwatch
                        ?: palette.darkVibrantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.darkMutedSwatch

                    if (swatch != null) {
                        val hsl = FloatArray(3)
                        ColorUtils.colorToHSL(swatch.rgb, hsl)

                        // Keep saturation rich and vivid like Spotify
                        hsl[1] = hsl[1].coerceIn(0.40f, 0.85f)
                        // Keep lightness at comfortable header brightness (22% - 36%)
                        hsl[2] = hsl[2].coerceIn(0.20f, 0.38f)

                        val extractedColor = Color(ColorUtils.HSLToColor(hsl))
                        PlaylistColorCache.setColor(playlistId, extractedColor)
                        themeColorState.value = extractedColor
                    }
                }
            } catch (e: Exception) {
                // Ignore and use fallback
            }
        }
    }

    return themeColorState.value
}
