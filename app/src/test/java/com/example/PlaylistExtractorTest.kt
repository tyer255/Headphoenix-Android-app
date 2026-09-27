package com.example

import com.example.data.PlaylistRepository
import com.example.data.remote.models.ExtractedPlaylist
import com.example.data.remote.models.ExtractedTrack
import com.example.data.remote.models.PlaylistExtractResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistExtractorTest {

    @Test
    fun testUrlChangeAndClear() {
        val viewModel = PlaylistExtractorViewModel()
        assertEquals("", viewModel.url.value)

        viewModel.onUrlChange("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M")
        assertEquals("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M", viewModel.url.value)

        viewModel.clearUrl()
        assertEquals("", viewModel.url.value)
        assertTrue(viewModel.uiState.value is PlaylistExtractorUiState.Idle)
    }

    @Test
    fun testPasteFromClipboard() {
        val viewModel = PlaylistExtractorViewModel()
        viewModel.pasteFromClipboard("  https://open.spotify.com/playlist/test12345  ")
        assertEquals("https://open.spotify.com/playlist/test12345", viewModel.url.value)
    }

    @Test
    fun testValidationForEmptyUrl() {
        val viewModel = PlaylistExtractorViewModel()
        viewModel.extractPlaylist()
        val state = viewModel.uiState.value
        assertTrue(state is PlaylistExtractorUiState.Error)
        assertEquals("Please enter or paste a Spotify playlist URL.", (state as PlaylistExtractorUiState.Error).message)
    }

    @Test
    fun testValidationForNonSpotifyUrl() {
        val viewModel = PlaylistExtractorViewModel()
        viewModel.onUrlChange("https://youtube.com/playlist?list=123")
        viewModel.extractPlaylist()
        val state = viewModel.uiState.value
        assertTrue(state is PlaylistExtractorUiState.Error)
        assertTrue((state as PlaylistExtractorUiState.Error).message.contains("valid Spotify URL"))
    }

    @Test
    fun testValidationForNonPlaylistUrl() {
        val viewModel = PlaylistExtractorViewModel()
        viewModel.onUrlChange("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT")
        viewModel.extractPlaylist()
        val state = viewModel.uiState.value
        assertTrue(state is PlaylistExtractorUiState.Error)
        assertTrue((state as PlaylistExtractorUiState.Error).message.contains("not a Spotify playlist"))
    }

    @Test
    fun testImportToLibrary() {
        val viewModel = PlaylistExtractorViewModel()
        val sampleTrack = ExtractedTrack(
            name = "Blinding Lights",
            artist = "The Weeknd",
            duration = 200000L,
            spotifyUri = "spotify:track:0VjIjW4GlUZAMYd2vXMi3b",
            imageUrl = "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36"
        )
        val sampleResponse = PlaylistExtractResponse(
            success = true,
            playlist = ExtractedPlaylist(
                name = "Top Hits",
                image = "https://i.scdn.co/image/ab67616d0000b2738863bc11d2aa12b54f5aeb36",
                spotifyUrl = "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M",
                description = "Hottest tracks"
            ),
            totalExtracted = 1,
            tracks = listOf(sampleTrack),
            truncated = false
        )

        // Set state to Success manually via reflection or simulate
        // Test track conversion
        with(viewModel) {
            val trackDto = sampleTrack.toTrackDto(0)
            assertEquals("0VjIjW4GlUZAMYd2vXMi3b", trackDto.id)
            assertEquals("Blinding Lights", trackDto.title)
            assertEquals("The Weeknd", trackDto.artist)
            assertEquals(200, trackDto.duration)
        }

        // Test saving into PlaylistRepository
        val initialCount = PlaylistRepository.playlists.value.size
        val imported = PlaylistRepository.importExtractedPlaylist(
            name = sampleResponse.playlist!!.name,
            description = sampleResponse.playlist!!.description ?: "",
            coverImage = sampleResponse.playlist?.image,
            tracks = listOf(
                with(viewModel) { sampleTrack.toTrackDto(0) }
            )
        )

        assertEquals("Top Hits", imported.title)
        assertEquals(1, imported.trackCount)
        assertEquals(initialCount + 1, PlaylistRepository.playlists.value.size)
        val found = PlaylistRepository.getPlaylistById(imported.id)
        assertNotNull(found)
        assertEquals(1, found?.tracks?.size)
    }
}
