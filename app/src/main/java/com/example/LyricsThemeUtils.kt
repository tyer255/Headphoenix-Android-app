package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class LyricsCardTheme(
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundBrush: Brush
)

object LyricsThemeCache {
    private val gradientCache = mutableMapOf<String, LyricsCardTheme>()
    private val mutedColorCache = mutableMapOf<String, Color>()

    fun getTheme(trackId: String): LyricsCardTheme? = gradientCache[trackId]
    fun setTheme(trackId: String, theme: LyricsCardTheme) { gradientCache[trackId] = theme }

    fun getMutedColor(trackId: String): Color? = mutedColorCache[trackId]
    fun setMutedColor(trackId: String, color: Color) { mutedColorCache[trackId] = color }
}

/**
 * Generates Spotify-authentic dynamic rich gradient theme for the Lyrics Preview Card.
 * Extracts dominant, vibrant, and dark swatches from the track's album art.
 * Every song receives a distinct, richly saturated yet dark, elegant gradient.
 */
@Composable
fun rememberTrackLyricsCardTheme(track: TrackDto?): LyricsCardTheme {
    val context = LocalContext.current
    val trackId = track?.id ?: "unknown"

    val fallbackTheme = remember(trackId) {
        val hash = abs((track?.id ?: track?.title ?: "").hashCode())
        val curatedPairs = listOf(
            // Rust Crimson / Dark Red (like Tauba Tauba)
            Color(0xFF8B251E) to Color(0xFF4A100C),
            // Deep Plum / Wine (like Haseen)
            Color(0xFF5A2A42) to Color(0xFF2C1220),
            // Burnt Amber / Dark Terracotta
            Color(0xFF8C4A1C) to Color(0xFF48220A),
            // Forest Emerald / Deep Olive
            Color(0xFF245E43) to Color(0xFF102E20),
            // Deep Ocean Sapphire / Indigo
            Color(0xFF1E466E) to Color(0xFF0D2138),
            // Regal Violet / Midnight Purple
            Color(0xFF532E74) to Color(0xFF27133A),
            // Warm Mocha / Espresso
            Color(0xFF6E402B) to Color(0xFF381F13),
            // Teal Slate / Dark Marine
            Color(0xFF1E5B5E) to Color(0xFF0D2E30)
        )
        val (topColor, bottomColor) = curatedPairs[hash % curatedPairs.size]
        LyricsCardTheme(
            primaryColor = topColor,
            secondaryColor = bottomColor,
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(topColor, bottomColor)
            )
        )
    }

    val themeState = remember(trackId) {
        mutableStateOf(LyricsThemeCache.getTheme(trackId) ?: fallbackTheme)
    }

    LaunchedEffect(trackId, track?.images?.large, track?.images?.medium) {
        val cached = LyricsThemeCache.getTheme(trackId)
        if (cached != null) {
            themeState.value = cached
            return@LaunchedEffect
        }

        val imageUrl = track?.images?.large ?: track?.images?.medium ?: track?.images?.small
        if (!imageUrl.isNullOrBlank()) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .size(250)
                    .build()

                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap()
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).generate()
                    }

                    val mainSwatch = palette.darkVibrantSwatch
                        ?: palette.vibrantSwatch
                        ?: palette.dominantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.darkMutedSwatch

                    if (mainSwatch != null) {
                        val hslTop = FloatArray(3)
                        ColorUtils.colorToHSL(mainSwatch.rgb, hslTop)
                        // Rich, saturated yet comfortable Spotify-style gradient colors
                        hslTop[1] = hslTop[1].coerceIn(0.40f, 0.75f)
                        hslTop[2] = hslTop[2].coerceIn(0.24f, 0.36f)

                        val topColor = Color(ColorUtils.HSLToColor(hslTop))

                        // Bottom darker gradient stop
                        val hslBottom = hslTop.clone()
                        hslBottom[2] = (hslTop[2] * 0.55f).coerceIn(0.10f, 0.20f)
                        val bottomColor = Color(ColorUtils.HSLToColor(hslBottom))

                        val dynamicTheme = LyricsCardTheme(
                            primaryColor = topColor,
                            secondaryColor = bottomColor,
                            backgroundBrush = Brush.verticalGradient(
                                colors = listOf(topColor, bottomColor)
                            )
                        )

                        LyricsThemeCache.setTheme(trackId, dynamicTheme)
                        themeState.value = dynamicTheme
                        return@LaunchedEffect
                    }
                }
            } catch (e: Exception) {
                // Ignore and use fallback
            }
        }

        val hex = track?.color
        if (!hex.isNullOrBlank()) {
            try {
                val parsed = android.graphics.Color.parseColor(hex)
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(parsed, hsl)
                hsl[1] = hsl[1].coerceIn(0.40f, 0.75f)
                hsl[2] = hsl[2].coerceIn(0.24f, 0.36f)
                val topColor = Color(ColorUtils.HSLToColor(hsl))
                hsl[2] = (hsl[2] * 0.55f).coerceIn(0.10f, 0.20f)
                val bottomColor = Color(ColorUtils.HSLToColor(hsl))

                val hexTheme = LyricsCardTheme(
                    primaryColor = topColor,
                    secondaryColor = bottomColor,
                    backgroundBrush = Brush.verticalGradient(listOf(topColor, bottomColor))
                )
                LyricsThemeCache.setTheme(trackId, hexTheme)
                themeState.value = hexTheme
                return@LaunchedEffect
            } catch (_: Exception) {}
        }

        LyricsThemeCache.setTheme(trackId, fallbackTheme)
        themeState.value = fallbackTheme
    }

    return themeState.value
}

/**
 * Backwards-compatibility wrapper for single-color usage.
 */
@Composable
fun rememberTrackMutedLyricsColor(track: TrackDto?): Color {
    return rememberTrackLyricsCardTheme(track).primaryColor
}
