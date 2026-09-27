package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.remote.models.TrackDto

object CollectionArtworkGenerator {
    /**
     * Extracts up to 4 UNIQUE track models prioritizing the latest/recently added tracks.
     * When new tracks are added to the playlist, they immediately appear in the 4-track collage,
     * replacing older tracks, while maintaining 4 distinct items.
     */
    fun extractUniqueTracks(tracks: List<TrackDto>): List<TrackDto> {
        if (tracks.isEmpty()) return emptyList()

        // Scan in reverse (latest tracks first) to prioritize the most recent additions
        val reversed = tracks.asReversed()
        val uniqueTracks = mutableListOf<TrackDto>()
        val seenUrls = mutableSetOf<String>()
        val seenTitles = mutableSetOf<String>()

        for (track in reversed) {
            val url = track.images?.large
                ?: track.images?.medium
                ?: track.images?.small

            val titleKey = track.title.trim().lowercase()

            if (!url.isNullOrBlank()) {
                if (!seenUrls.contains(url)) {
                    seenUrls.add(url)
                    seenTitles.add(titleKey)
                    uniqueTracks.add(track)
                }
            } else {
                if (!seenTitles.contains(titleKey)) {
                    seenTitles.add(titleKey)
                    uniqueTracks.add(track)
                }
            }

            if (uniqueTracks.size == 4) break
        }

        // If we have fewer than 4 unique tracks but more total tracks, fill up to 4
        if (uniqueTracks.size < 4 && tracks.size > uniqueTracks.size) {
            for (track in reversed) {
                if (!uniqueTracks.contains(track)) {
                    uniqueTracks.add(track)
                    if (uniqueTracks.size == 4) break
                }
            }
        }

        return uniqueTracks
    }

    /**
     * Legacy extractor for raw URLs
     */
    fun extractUniqueArtworks(tracks: List<TrackDto>): List<String> {
        val uniqueTracks = extractUniqueTracks(tracks)
        return uniqueTracks.mapNotNull { track ->
            track.images?.large
                ?: track.images?.medium
                ?: track.images?.small
        }
    }
}

/**
 * Reusable Centralized Collection/Playlist Artwork Component.
 * - If [customCoverUrl] is provided, prioritizes custom cover.
 * - Otherwise dynamically generates artwork from the track collection:
 *   - 0 tracks: displays placeholder icon
 *   - 1 track: full square image
 *   - 2 tracks: 2-column split (50% / 50%)
 *   - 3 tracks: 3-tile composition (Left 50%, Right top/bottom 25% each)
 *   - 4 or more tracks: 2x2 grid of the 4 most recently added tracks.
 * Uses [SongArtworkImage] for individual tiles to ensure robust fallback gradients and
 * dynamic artwork resolution without ever showing black or broken screens.
 */
@Composable
fun CollectionArtworkImage(
    tracks: List<TrackDto>,
    modifier: Modifier = Modifier,
    customCoverUrl: String? = null,
    contentDescription: String? = "Collection Cover",
    placeholderIcon: ImageVector = Icons.Default.MusicNote,
    placeholderBackground: Color = Color(0xFF282828)
) {
    if (!customCoverUrl.isNullOrBlank()) {
        AsyncImage(
            model = customCoverUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
        return
    }

    val uniqueTracks = remember(tracks) {
        CollectionArtworkGenerator.extractUniqueTracks(tracks)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(placeholderBackground),
        contentAlignment = Alignment.Center
    ) {
        when (uniqueTracks.size) {
            0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = contentDescription,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxSize(0.45f)
                    )
                }
            }
            1 -> {
                SongArtworkImage(
                    track = uniqueTracks[0],
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxSize()
                )
            }
            2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    SongArtworkImage(
                        track = uniqueTracks[0],
                        shape = RectangleShape,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    SongArtworkImage(
                        track = uniqueTracks[1],
                        shape = RectangleShape,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
            3 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    SongArtworkImage(
                        track = uniqueTracks[0],
                        shape = RectangleShape,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        SongArtworkImage(
                            track = uniqueTracks[1],
                            shape = RectangleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                        SongArtworkImage(
                            track = uniqueTracks[2],
                            shape = RectangleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
            else -> {
                // 4 or more tracks: 2x2 grid with 4 newest unique items
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        SongArtworkImage(
                            track = uniqueTracks[0],
                            shape = RectangleShape,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SongArtworkImage(
                            track = uniqueTracks[1],
                            shape = RectangleShape,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        SongArtworkImage(
                            track = uniqueTracks[2],
                            shape = RectangleShape,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SongArtworkImage(
                            track = uniqueTracks[3],
                            shape = RectangleShape,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}
