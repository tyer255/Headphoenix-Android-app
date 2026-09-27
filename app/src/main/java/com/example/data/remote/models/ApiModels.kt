package com.example.data.remote.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

@Serializable
data class HealthStatusDto(
    val status: String,
    val timestamp: String
)

@Serializable
data class ImagesDto(
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null
)

@Serializable
data class TrackDto(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val duration: Int = 0,
    val images: ImagesDto? = null,
    val color: String? = null,
    val streamUrl: String? = null,
    val playbackAvailability: Boolean? = true,
    val spotifyId: String? = null,
    val spotifyUri: String? = null
)

@Serializable
data class AlbumDto(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String? = null,
    val year: Int? = null,
    val images: ImagesDto? = null,
    val tracks: List<TrackDto>? = null,
    val totalDuration: Int? = null
)

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val image: String? = null,
    val followers: Long? = null,
    val monthlyListeners: Long? = null,
    val genres: List<String>? = null,
    val bio: String? = null,
    val verified: Boolean? = false,
    val topTracks: List<TrackDto>? = null,
    val albums: List<AlbumDto>? = null,
    val singles: List<TrackDto>? = null
)

@Serializable
data class MoodDto(
    val id: String,
    val name: String,
    val color: String? = null,
    val image: String? = null,
    val query: String? = null
)

@Serializable
data class HomeDto(
    val greeting: String? = null,
    val quickPicks: List<TrackDto>? = null,
    val recentlyPlayed: List<TrackDto>? = null,
    val madeForYou: List<TrackDto>? = null,
    val trending: List<TrackDto>? = null,
    val popularSongs: List<TrackDto>? = null,
    val popularArtists: List<ArtistDto>? = null,
    val newReleases: List<AlbumDto>? = null,
    val recommendedAlbums: List<AlbumDto>? = null,
    val moods: List<MoodDto>? = null
)

@Serializable
data class TopResultDto(
    val type: String,
    val data: JsonElement
)

@Serializable
data class PlaylistDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverImage: String? = null,
    val image: String? = null,
    val userId: String? = null,
    val color: String? = null
)

@Serializable
data class SearchDto(
    val topResult: TopResultDto? = null,
    val songs: List<TrackDto>? = null,
    val artists: List<ArtistDto>? = null,
    val albums: List<AlbumDto>? = null,
    val playlists: List<PlaylistDto>? = null
)

@Serializable
data class SearchSuggestionDto(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val type: String,
    val image: String? = null
)

@Serializable
data class StreamDto(
    val url: String,
    val fallbackUrls: List<String> = emptyList(),
    val mimeType: String? = null,
    val expiresAt: Long? = null,
    val resolvedTitle: String? = null,
    val resolvedArtist: String? = null,
    val descriptorType: String? = null,
    val isMediaDescriptor: Boolean? = null
)

@Serializable
data class PlaybackResolveDataDto(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnail: String? = null,
    val duration: Int,
    val stream: StreamDto
)

@Serializable
data class PlaybackResolveRequest(
    val trackId: String,
    val title: String? = null,
    val artist: String? = null,
    val duration: Int? = null
)

@Serializable
data class LyricsLineDto(
    val time: Double = 0.0,
    val startTimeMs: Long = 0L,
    val text: String = ""
)

@Serializable
data class LyricsDto(
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val synced: Boolean = false,
    val lines: List<LyricsLineDto> = emptyList(),
    val plainLyrics: String? = null
)


@Serializable
data class CanvasDto(
    val requestedTrackId: String? = null,
    val canvasUrl: String? = null,
    val videoUrl: String? = null,
    val url: String? = null,
    val canvas: String? = null
)

// ================= SPOTIFY PLAYLIST EXTRACTOR MODELS =================

@Serializable
data class PlaylistExtractRequest(
    val url: String = ""
)

@Serializable
data class ExtractedPlaylist(
    val name: String = "Spotify Playlist",
    val image: String? = null,
    val spotifyUrl: String? = null,
    val description: String? = null
)

@Serializable
data class ExtractedTrack(
    val name: String = "Untitled Track",
    val artist: String = "Unknown Artist",
    val album: String? = null,
    val duration: Long = 0L,
    val spotifyUri: String? = null,
    val spotifyTrackId: String? = null,
    val previewUrl: String? = null,
    val imageUrl: String? = null,
    val artwork: String? = null,
    val isrc: String? = null
) {
    fun getEffectiveArtwork(): String? = artwork?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }
}

@Serializable
data class ExtractedError(
    val code: String = "ERROR",
    val message: String = "An error occurred"
)

@Serializable
data class PlaylistExtractResponse(
    val success: Boolean = true,
    val playlist: ExtractedPlaylist? = null,
    val totalExtracted: Int = 0,
    val tracks: List<ExtractedTrack> = emptyList(),
    val truncated: Boolean = false,
    val note: String? = null,
    val error: ExtractedError? = null
)

