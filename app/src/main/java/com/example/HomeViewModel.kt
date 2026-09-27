package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.PlaybackHistoryRepository
import com.example.data.PlaylistRepository
import com.example.data.remote.models.HomeDto
import com.example.data.remote.models.ImagesDto
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PersonalizedArtistSection(
    val artistName: String = "Anuv Jain",
    val artistAvatar: String = "https://i.scdn.co/image/ab6761610000e5eba837a6cb82dd949d5e1f9b53",
    val tracks: List<TrackDto> = emptyList(),
    val isAutoPersonalized: Boolean = true
)

class HomeViewModel : ViewModel() {
    private val repository = MusicRepository()
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _personalizedSection = MutableStateFlow(
        PersonalizedArtistSection(
            artistName = "Anuv Jain",
            artistAvatar = "https://i.scdn.co/image/ab6761610000e5eba837a6cb82dd949d5e1f9b53",
            tracks = getDefaultAnuvJainTracks()
        )
    )
    val personalizedSection: StateFlow<PersonalizedArtistSection> = _personalizedSection.asStateFlow()

    private var artistSearchJob: Job? = null
    private var lastPersonalizedArtist: String = "Anuv Jain"

    companion object {
        val KNOWN_ARTIST_AVATARS = mapOf(
            "anuv jain" to "https://i.scdn.co/image/ab6761610000e5eba837a6cb82dd949d5e1f9b53",
            "arijit singh" to "https://cdn-images.dzcdn.net/images/artist/ac5350cff290edd5b69fa584b8b1bd4f/1000x1000-000000-80-0-0.jpg",
            "karan aujla" to "https://cdn-images.dzcdn.net/images/artist/a91a1d5ea91e85e4f0966569b50e8d6a/1000x1000-000000-80-0-0.jpg",
            "kr\$na" to "https://i.scdn.co/image/ab6761610000e5eb33a750f4e8264286af8e751a",
            "krsna" to "https://i.scdn.co/image/ab6761610000e5eb33a750f4e8264286af8e751a",
            "diljit dosanjh" to "https://cdn-images.dzcdn.net/images/artist/79b85e695e0ca6529e56bf3b628e92bd/1000x1000-000000-80-0-0.jpg",
            "yo yo honey singh" to "https://cdn-images.dzcdn.net/images/artist/7859b461c10352f02a11368905f0903f/1000x1000-000000-80-0-0.jpg",
            "shreya ghoshal" to "https://cdn-images.dzcdn.net/images/artist/3bb832d37d10ff2affcfa9afdc7c68a0/1000x1000-000000-80-0-0.jpg",
            "ap dhillon" to "https://cdn-images.dzcdn.net/images/artist/52594ac9fa763dc163ed13d21cb130ec/1000x1000-000000-80-0-0.jpg",
            "sidhu moose wala" to "https://cdn-images.dzcdn.net/images/artist/fb1def876c43cc16738bfd6ad3d1dcd9/1000x1000-000000-80-0-0.jpg",
            "atif aslam" to "https://i.scdn.co/image/ab6761610000e5ebc40600e02356cc86f0debe84",
            "kishore kumar" to "https://image-cdn-ak.spotifycdn.com/image/ab67706c0000da84f8a6354140d1d215eb682073"
        )

        private fun getDefaultAnuvJainTracks(): List<TrackDto> = listOf(
            TrackDto(
                id = "anuv_arz",
                title = "Arz Kiya Hai | Coke Studio Bharat",
                artist = "Anuv Jain",
                album = "Coke Studio Bharat",
                images = ImagesDto(
                    large = "https://i.scdn.co/image/ab67616d0000b27325fa2d19b2363a9520a34409",
                    medium = "https://i.scdn.co/image/ab67616d00001e0225fa2d19b2363a9520a34409",
                    small = "https://i.scdn.co/image/ab67616d0000485125fa2d19b2363a9520a34409"
                ),
                streamUrl = "https://aac.saavncdn.com/089/64beffa430e4c948223ec6bfcc3a13f0_320.mp4"
            ),
            TrackDto(
                id = "anuv_jotum",
                title = "Jo Tum Mere Ho",
                artist = "Anuv Jain",
                album = "Jo Tum Mere Ho",
                images = ImagesDto(
                    large = "https://i.scdn.co/image/ab67616d0000b27372a77d038887cdc425f5ee55",
                    medium = "https://i.scdn.co/image/ab67616d00001e0272a77d038887cdc425f5ee55",
                    small = "https://i.scdn.co/image/ab67616d0000485172a77d038887cdc425f5ee55"
                )
            ),
            TrackDto(
                id = "anuv_husn",
                title = "Husn",
                artist = "Anuv Jain",
                album = "Husn",
                images = ImagesDto(
                    large = "https://i.scdn.co/image/ab67616d0000b2730d3449f333a83a25feb423f8",
                    medium = "https://i.scdn.co/image/ab67616d00001e020d3449f333a83a25feb423f8",
                    small = "https://i.scdn.co/image/ab67616d000048510d3449f333a83a25feb423f8"
                )
            ),
            TrackDto(
                id = "anuv_afsos",
                title = "Afsos",
                artist = "Anuv Jain, AP Dhillon",
                album = "Afsos",
                images = ImagesDto(
                    large = "https://i.scdn.co/image/ab67616d0000b2738537cf974af2c408bdd8e1a6",
                    medium = "https://i.scdn.co/image/ab67616d00001e028537cf974af2c408bdd8e1a6",
                    small = "https://i.scdn.co/image/ab67616d000048518537cf974af2c408bdd8e1a6"
                )
            ),
            TrackDto(
                id = "anuv_baarishein",
                title = "Baarishein",
                artist = "Anuv Jain",
                album = "Baarishein",
                images = ImagesDto(
                    large = "https://i.scdn.co/image/ab67616d0000b273d8ca783fa570b57e082f4374",
                    medium = "https://i.scdn.co/image/ab67616d00001e02d8ca783fa570b57e082f4374",
                    small = "https://i.scdn.co/image/ab67616d00004851d8ca783fa570b57e082f4374"
                )
            ),
            TrackDto(
                id = "anuv_riha",
                title = "Riha",
                artist = "Anuv Jain",
                album = "Riha",
                images = ImagesDto(
                    large = "https://i.scdn.co/image/ab67616d0000b2733f0728f55894526094f777f9",
                    medium = "https://i.scdn.co/image/ab67616d00001e023f0728f55894526094f777f9",
                    small = "https://i.scdn.co/image/ab67616d000048513f0728f55894526094f777f9"
                )
            )
        )
    }

    init {
        fetchHome()
        observeListeningPersonalization()
    }

    private fun fetchHome() {
        viewModelScope.launch {
            try {
                val data = repository.getHome()
                if (data != null) {
                    _uiState.value = HomeUiState.Success(data)
                } else {
                    _uiState.value = HomeUiState.Error("Failed to load home data")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Watches user playback history & liked songs to dynamically adapt
     * "BECAUSE YOU LISTEN TO [Artist Name]" section to the singer user listens to most!
     */
    private fun observeListeningPersonalization() {
        viewModelScope.launch {
            combine(
                PlaybackHistoryRepository.recentlyPlayed,
                PlaylistRepository.likedTracks
            ) { history, liked ->
                computeTopArtist(history, liked)
            }.collect { topArtist ->
                if (topArtist != lastPersonalizedArtist) {
                    lastPersonalizedArtist = topArtist
                    loadArtistSection(topArtist, isAuto = true)
                }
            }
        }
    }

    private fun computeTopArtist(history: List<TrackDto>, liked: List<TrackDto>): String {
        val scoreMap = mutableMapOf<String, Int>()

        // Weight recently played tracks (highest weight to most recent)
        history.forEachIndexed { index, track ->
            val primaryArtist = extractPrimaryArtist(track.artist)
            if (primaryArtist.isNotBlank() && primaryArtist.lowercase() != "unknown artist") {
                val weight = maxOf(1, 12 - index)
                scoreMap[primaryArtist] = (scoreMap[primaryArtist] ?: 0) + weight
            }
        }

        // Weight liked songs
        liked.forEach { track ->
            val primaryArtist = extractPrimaryArtist(track.artist)
            if (primaryArtist.isNotBlank() && primaryArtist.lowercase() != "unknown artist") {
                scoreMap[primaryArtist] = (scoreMap[primaryArtist] ?: 0) + 5
            }
        }

        return scoreMap.maxByOrNull { it.value }?.key ?: "Anuv Jain"
    }

    private fun extractPrimaryArtist(artistCredit: String?): String {
        if (artistCredit.isNullOrBlank()) return ""
        val firstPart = artistCredit.split(",", "&", "feat.", "ft.", "|").firstOrNull()?.trim() ?: artistCredit
        return firstPart
    }

    /**
     * User can also directly tap an artist from "Your favourite artists"
     * to immediately tune the homepage personalization!
     */
    fun selectArtistForPersonalization(artistName: String) {
        lastPersonalizedArtist = artistName
        loadArtistSection(artistName, isAuto = false)
    }

    private fun loadArtistSection(artistName: String, isAuto: Boolean) {
        artistSearchJob?.cancel()
        artistSearchJob = viewModelScope.launch {
            val lower = artistName.lowercase().trim()
            val fallbackAvatar = KNOWN_ARTIST_AVATARS[lower]
                ?: KNOWN_ARTIST_AVATARS.entries.firstOrNull { lower.contains(it.key) || it.key.contains(lower) }?.value
                ?: "https://i.scdn.co/image/ab6761610000e5eba837a6cb82dd949d5e1f9b53"

            try {
                val searchResult = repository.search(artistName)
                val songs = searchResult?.songs ?: emptyList()

                val avatar = searchResult?.artists?.firstOrNull()?.image
                    ?: fallbackAvatar

                if (songs.isNotEmpty()) {
                    _personalizedSection.value = PersonalizedArtistSection(
                        artistName = artistName,
                        artistAvatar = avatar,
                        tracks = songs.take(12),
                        isAutoPersonalized = isAuto
                    )
                } else if (lower.contains("anuv")) {
                    _personalizedSection.value = PersonalizedArtistSection(
                        artistName = "Anuv Jain",
                        artistAvatar = fallbackAvatar,
                        tracks = getDefaultAnuvJainTracks(),
                        isAutoPersonalized = isAuto
                    )
                }
            } catch (e: Exception) {
                // If network fails, keep fallback tracks with working URLs
                if (lower.contains("anuv")) {
                    _personalizedSection.value = PersonalizedArtistSection(
                        artistName = "Anuv Jain",
                        artistAvatar = fallbackAvatar,
                        tracks = getDefaultAnuvJainTracks(),
                        isAutoPersonalized = isAuto
                    )
                }
            }
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val data: HomeDto) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

