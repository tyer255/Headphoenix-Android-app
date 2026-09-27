package com.example.data

import com.example.data.remote.models.TrackDto
import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverImage: String? = null,
    val trackCount: Int = 0,
    val owner: String = "Spotify",
    val tracks: List<TrackDto> = emptyList(),
    val colorHex: Long = 0xFF1DB954,
    val isCustom: Boolean = false,
    val isPublic: Boolean = false
)

@Serializable
data class PlaybackDevice(
    val id: String,
    val name: String,
    val type: String, // "Smartphone", "Computer", "Speaker"
    val isActive: Boolean = false,
    val volume: Int = 80
)

@Serializable
data class UserProfile(
    val id: String = "u1",
    val name: String = "Spotify User",
    val email: String = "user@spotify.com",
    val product: String = "Premium",
    val avatarUrl: String? = null,
    val followers: Int = 24,
    val following: Int = 18
)

