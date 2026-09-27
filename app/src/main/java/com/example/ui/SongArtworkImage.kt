package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.remote.TrackArtworkResolver
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-reliability Song Artwork Image component.
 * Automatically attempts primary, secondary, and dynamic resolution of song artwork,
 * ensuring no song ever appears with a broken or blank thumbnail.
 */
@Composable
fun SongArtworkImage(
    modifier: Modifier = Modifier,
    track: TrackDto? = null,
    imageUrl: String? = null,
    title: String? = track?.title,
    artist: String? = track?.artist,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(4.dp),
    placeholderIcon: ImageVector = Icons.Filled.MusicNote,
    showInitialsIfMissing: Boolean = true
) {
    val context = LocalContext.current

    // Determine initial candidate URLs in order of preference
    val primaryUrl = remember(track?.id, imageUrl, track?.images?.large, track?.images?.medium, track?.images?.small) {
        imageUrl?.takeIf { it.isNotBlank() }
            ?: track?.images?.large?.takeIf { it.isNotBlank() }
            ?: track?.images?.medium?.takeIf { it.isNotBlank() }
            ?: track?.images?.small?.takeIf { it.isNotBlank() }
    }

    var currentUrl by remember(primaryUrl) { mutableStateOf(primaryUrl) }
    var hasError by remember(primaryUrl) { mutableStateOf(false) }

    // Dynamic resolution fallback if URL is null or failed
    LaunchedEffect(track?.id, title, artist, hasError) {
        if (currentUrl.isNullOrBlank() || hasError) {
            val t = title ?: track?.title
            val a = artist ?: track?.artist
            if (!t.isNullOrBlank()) {
                val cached = TrackArtworkResolver.getCachedArtwork("${t.trim().lowercase()}_${a?.trim()?.lowercase() ?: ""}")
                    ?: track?.spotifyId?.let { TrackArtworkResolver.getCachedArtwork(it) }
                    ?: track?.id?.let { TrackArtworkResolver.getCachedArtwork(it) }

                if (!cached.isNullOrBlank()) {
                    currentUrl = cached
                    hasError = false
                } else {
                    withContext(Dispatchers.IO) {
                        val resolved = TrackArtworkResolver.resolveTrackArtwork(
                            title = t,
                            artist = a ?: "",
                            album = track?.album,
                            durationSeconds = track?.duration,
                            spotifyId = track?.spotifyId ?: track?.id
                        )
                        if (!resolved.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                currentUrl = resolved
                                hasError = false
                            }
                        }
                    }
                }
            }
        }
    }

    // Generate stable gradient colors from title or artist
    val gradientColors = remember(title, artist) {
        generateStableGradient(title ?: "", artist ?: "")
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF242424)),
        contentAlignment = Alignment.Center
    ) {
        if (!currentUrl.isNullOrBlank() && !hasError) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title ?: "Artwork",
                contentScale = contentScale,
                onError = {
                    hasError = true
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Spotify-style colored gradient fallback with icon or initials
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                if (showInitialsIfMissing && !title.isNullOrBlank()) {
                    val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: ""
                    Text(
                        text = initial,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = title,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize(0.5f)
                    )
                }
            }
        }
    }
}

private fun generateStableGradient(title: String, artist: String): List<Color> {
    val hash = (title + artist).hashCode()
    val palettes = listOf(
        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
        listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
        listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)),
        listOf(Color(0xFFCC2B5E), Color(0xFF753A88)),
        listOf(Color(0xFF42275A), Color(0xFF734B6D)),
        listOf(Color(0xFF1A2980), Color(0xFF26D0CE)),
        listOf(Color(0xFFE65C00), Color(0xFFF9D423)),
        listOf(Color(0xFF134E5E), Color(0xFF71B280))
    )
    val index = kotlin.math.abs(hash) % palettes.size
    return palettes[index]
}
