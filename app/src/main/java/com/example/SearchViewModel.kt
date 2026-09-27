package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.PlaybackHistoryRepository
import com.example.data.PlaylistRepository
import com.example.data.remote.models.AlbumDto
import com.example.data.remote.models.ArtistDto
import com.example.data.remote.models.PlaylistDto
import com.example.data.remote.models.SearchSuggestionDto
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchIntentInfo(
    val query: String,
    val intentType: IntentType,
    val matchedPreference: String? = null,
    val personalizedTag: String? = null,
    val explanation: String? = null
)

enum class IntentType {
    ARTIST,
    MOOD_VIBE,
    SONG,
    LANGUAGE,
    GENERAL
}

class SearchViewModel : ViewModel() {
    private val repository = MusicRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchTracks = MutableStateFlow<List<TrackDto>>(emptyList())
    val searchTracks: StateFlow<List<TrackDto>> = _searchTracks.asStateFlow()

    private val _searchArtists = MutableStateFlow<List<ArtistDto>>(emptyList())
    val searchArtists: StateFlow<List<ArtistDto>> = _searchArtists.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestionDto>>(emptyList())
    val searchSuggestions: StateFlow<List<SearchSuggestionDto>> = _searchSuggestions.asStateFlow()

    private val _searchAlbums = MutableStateFlow<List<AlbumDto>>(emptyList())
    val searchAlbums: StateFlow<List<AlbumDto>> = _searchAlbums.asStateFlow()

    private val _searchPlaylists = MutableStateFlow<List<PlaylistDto>>(emptyList())
    val searchPlaylists: StateFlow<List<PlaylistDto>> = _searchPlaylists.asStateFlow()

    private val _searchIntent = MutableStateFlow<SearchIntentInfo?>(null)
    val searchIntent: StateFlow<SearchIntentInfo?> = _searchIntent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    // Known favorite artists from user personalization
    private val favoriteArtists = listOf("Anuv Jain", "Arijit Singh", "Karan Aujla", "KR" + "$" + "NA", "AP Dhillon", "Shreya Ghoshal", "Pritam")

    fun updateQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.length > 1) {
            searchJob = viewModelScope.launch {
                delay(300) // debounce
                val suggestions = repository.getSearchSuggestions(query)
                _searchSuggestions.value = suggestions
                performSearch(query)
            }
        } else {
            _searchSuggestions.value = emptyList()
            _searchTracks.value = emptyList()
            _searchArtists.value = emptyList()
            _searchAlbums.value = emptyList()
            _searchPlaylists.value = emptyList()
            _searchIntent.value = null
        }
    }

    fun performSearchNow(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        _searchSuggestions.value = emptyList()
        viewModelScope.launch {
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _isLoading.value = true
        try {
            val trimmed = query.trim()
            val intent = analyzeSearchIntent(trimmed)
            _searchIntent.value = intent

            val res = repository.search(trimmed)
            if (res != null) {
                val rawSongs = res.songs ?: emptyList()
                val rawArtists = res.artists ?: emptyList()

                // Personalize & Re-rank based on user preferences and search intent
                val personalizedSongs = personalizeAndRankTracks(rawSongs, intent)
                val personalizedArtists = personalizeAndRankArtists(rawArtists, intent)

                _searchTracks.value = personalizedSongs
                _searchArtists.value = personalizedArtists
                _searchAlbums.value = res.albums ?: emptyList()
                _searchPlaylists.value = res.playlists ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Understands search intent using NLP keywords, language cues, and personalization preferences.
     */
    private fun analyzeSearchIntent(query: String): SearchIntentInfo {
        val lower = query.lowercase()

        // 1. Check if user is searching for or mentioning their favorite artists
        for (fav in favoriteArtists) {
            if (lower.contains(fav.lowercase()) || fav.lowercase().split(" ").any { part -> part.length > 3 && lower.contains(part) }) {
                return SearchIntentInfo(
                    query = query,
                    intentType = IntentType.ARTIST,
                    matchedPreference = fav,
                    personalizedTag = "Tuned to your favorite artist: $fav",
                    explanation = "Prioritizing top releases, acoustic tracks, and singles by $fav based on your taste."
                )
            }
        }

        // 2. Check for Mood/Vibe intent (e.g. sad, romantic, lofi, acoustic, chill, party)
        val moodMap = mapOf(
            "sad" to "Soulful & Melancholic",
            "heartbreak" to "Heartbreak & Emotional",
            "dard" to "Emotional Ballads",
            "romantic" to "Romantic & Intimate",
            "pyar" to "Romantic Hits",
            "love" to "Love & Acoustic",
            "chill" to "Relaxed & Late Night",
            "lofi" to "Lofi Chill & Ambient",
            "acoustic" to "Indie Acoustic Vibes",
            "party" to "High-Energy Party Beats",
            "dance" to "Dance & Club Hits",
            "gym" to "Workout & Motivation",
            "workout" to "Fitness & Pump Beats",
            "rap" to "Desi Hip-Hop & Rap",
            "hiphop" to "Hip-Hop & Urban Beats",
            "sufi" to "Sufi & Devotional Soul",
            "coke studio" to "Coke Studio Live Sessions"
        )
        for ((keyword, vibe) in moodMap) {
            if (lower.contains(keyword)) {
                return SearchIntentInfo(
                    query = query,
                    intentType = IntentType.MOOD_VIBE,
                    matchedPreference = vibe,
                    personalizedTag = "Personalized Vibe: $vibe",
                    explanation = "Curated based on your listening style and $vibe preferences."
                )
            }
        }

        // 3. Language & Regional intent
        val languageKeywords = listOf("hindi", "punjabi", "urdu", "bengali", "tamil", "telugu", "english", "haryanvi")
        for (lang in languageKeywords) {
            if (lower.contains(lang)) {
                return SearchIntentInfo(
                    query = query,
                    intentType = IntentType.LANGUAGE,
                    matchedPreference = lang.replaceFirstChar { it.uppercase() },
                    personalizedTag = "Regional Style: ${lang.replaceFirstChar { it.uppercase() }} Hits",
                    explanation = "Showing relevant tracks aligned with your ${lang.replaceFirstChar { it.uppercase() }} listening history."
                )
            }
        }

        // 4. Default personalization
        return SearchIntentInfo(
            query = query,
            intentType = IntentType.GENERAL,
            matchedPreference = null,
            personalizedTag = "Personalized for your music taste",
            explanation = "Results organized according to your listening habits and favorite artists."
        )
    }

    /**
     * Re-ranks search results giving higher priority to:
     * - Liked tracks in user library
     * - Songs by favorite artists (Anuv Jain, Arijit Singh, Karan Aujla, KR$NA)
     * - Songs matching search intent
     * - Recently played history
     */
    private fun personalizeAndRankTracks(tracks: List<TrackDto>, intent: SearchIntentInfo): List<TrackDto> {
        val liked = PlaylistRepository.likedTracks.value
        val recent = PlaybackHistoryRepository.recentlyPlayed.value

        return tracks.sortedByDescending { track ->
            var score = 0

            // Liked songs boost
            if (liked.any { it.id == track.id || it.title.equals(track.title, ignoreCase = true) }) {
                score += 50
            }

            // Recently played boost
            if (recent.any { it.id == track.id || it.title.equals(track.title, ignoreCase = true) }) {
                score += 30
            }

            // Favorite artists boost
            val artistName = track.artist ?: ""
            if (favoriteArtists.any { fav -> artistName.contains(fav, ignoreCase = true) }) {
                score += 40
            }

            // Intent-matched preference boost
            intent.matchedPreference?.let { pref ->
                if (artistName.contains(pref, ignoreCase = true) || track.title.contains(pref, ignoreCase = true)) {
                    score += 60
                }
            }

            score
        }
    }

    private fun personalizeAndRankArtists(artists: List<ArtistDto>, intent: SearchIntentInfo): List<ArtistDto> {
        return artists.sortedByDescending { artist ->
            var score = 0
            if (favoriteArtists.any { it.equals(artist.name, ignoreCase = true) }) {
                score += 50
            }
            intent.matchedPreference?.let { pref ->
                if (artist.name.contains(pref, ignoreCase = true)) {
                    score += 60
                }
            }
            score
        }
    }
}
