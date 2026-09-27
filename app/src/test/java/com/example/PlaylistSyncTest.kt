package com.example

import com.example.data.PlaylistRepository
import com.example.data.remote.SpotifyNativeExtractor
import com.example.data.remote.models.PlaylistExtractResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistSyncTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUrlChangeAndClear() {
        val viewModel = PlaylistSyncViewModel()
        assertEquals("", viewModel.url.value)

        viewModel.onUrlChange("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M")
        assertEquals("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M", viewModel.url.value)

        viewModel.clearUrl()
        assertEquals("", viewModel.url.value)
        assertTrue(viewModel.syncState.value is PlaylistSyncState.Idle)
    }

    @Test
    fun testPasteFromClipboard() {
        val viewModel = PlaylistSyncViewModel()
        viewModel.pasteFromClipboard("  https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=12345  ")
        assertEquals("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=12345", viewModel.url.value)
    }

    @Test
    fun testValidationForEmptyUrl() {
        val viewModel = PlaylistSyncViewModel()
        viewModel.clonePlaylist()
        val state = viewModel.syncState.value
        assertTrue(state is PlaylistSyncState.Error)
        assertEquals("Please paste or enter a Spotify playlist link.", (state as PlaylistSyncState.Error).message)
    }

    @Test
    fun testValidationForInvalidDomain() {
        val viewModel = PlaylistSyncViewModel()
        viewModel.onUrlChange("https://apple.music.com/playlist/test")
        viewModel.clonePlaylist()
        val state = viewModel.syncState.value
        assertTrue(state is PlaylistSyncState.Error)
        assertTrue((state as PlaylistSyncState.Error).message.contains("valid Spotify link"))
    }

    @Test
    fun testValidationForNonPlaylistUrl() {
        val viewModel = PlaylistSyncViewModel()
        viewModel.onUrlChange("https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT")
        viewModel.clonePlaylist()
        val state = viewModel.syncState.value
        assertTrue(state is PlaylistSyncState.Error)
        assertTrue((state as PlaylistSyncState.Error).message.contains("not a Spotify playlist"))
    }

    @Test
    fun testDismissError() {
        val viewModel = PlaylistSyncViewModel()
        viewModel.clonePlaylist()
        assertTrue(viewModel.syncState.value is PlaylistSyncState.Error)

        viewModel.dismissError()
        assertTrue(viewModel.syncState.value is PlaylistSyncState.Idle)
    }

    @Test
    fun testJsonNullInApiResponseParsing() {
        val responseWithNulls = """
            {
                "success": true,
                "playlist": {
                    "name": null,
                    "image": null,
                    "spotifyUrl": null,
                    "description": null
                },
                "totalExtracted": 1,
                "tracks": [
                    {
                        "name": "Song With Nulls",
                        "artist": "Artist",
                        "duration": 180000,
                        "spotifyUri": null,
                        "previewUrl": null,
                        "imageUrl": null
                    }
                ],
                "truncated": false,
                "note": null,
                "error": null
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PlaylistExtractResponse>(responseWithNulls)
        assertTrue(parsed.success)
        assertEquals(1, parsed.tracks.size)
        assertEquals("Song With Nulls", parsed.tracks[0].name)
        assertNull(parsed.tracks[0].previewUrl)
        assertNull(parsed.playlist?.image)
    }

    @Test
    fun testLiveSpotifyPlaylistExtraction() = runBlocking {
        val result = SpotifyNativeExtractor.extract("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M")
        assertTrue("Extraction should succeed for public Spotify playlist", result.isSuccess)
        val data = result.getOrThrow()
        assertNotNull(data.playlist)
        assertTrue("Should extract tracks", data.tracks.isNotEmpty())
        assertTrue("Playlist name should not be blank", data.playlist?.name?.isNotBlank() == true)
    }

    @Test
    fun testHighQualityPlaylistCoverImageExtraction() {
        val mosaicUrl = "https://mosaic.scdn.co/60/ab67616d0000b273..."
        val upgradedMosaic = SpotifyNativeExtractor.upgradeSpotifyImageUrl(mosaicUrl)
        assertEquals("https://mosaic.scdn.co/640/ab67616d0000b273...", upgradedMosaic)

        val smallCdn = "https://i.scdn.co/image/ab67616d000048511234567890abcdef"
        val upgradedCdn = SpotifyNativeExtractor.upgradeSpotifyImageUrl(smallCdn)
        assertEquals("https://i.scdn.co/image/ab67616d0000b2731234567890abcdef", upgradedCdn)
    }

    @Test
    fun testIndividualTrackArtworkPreservation() {
        val jsonPayload = """
            {
                "success": true,
                "playlist": {
                    "name": "Bollywood Hits",
                    "image": "https://mosaic.scdn.co/640/playlist_cover_collage_123",
                    "spotifyUrl": "https://open.spotify.com/playlist/test",
                    "description": null
                },
                "totalExtracted": 3,
                "tracks": [
                    {
                        "name": "Hum Se Hai",
                        "artist": "Artist A",
                        "duration": 180000,
                        "spotifyUri": "spotify:track:track1",
                        "spotifyTrackId": "track1",
                        "artwork": "https://i.scdn.co/image/hum_se_hai_art_123"
                    },
                    {
                        "name": "Tere Ho Ke Rahenge",
                        "artist": "Artist B",
                        "duration": 200000,
                        "spotifyUri": "spotify:track:track2",
                        "spotifyTrackId": "track2",
                        "artwork": "https://i.scdn.co/image/tere_ho_ke_art_456"
                    },
                    {
                        "name": "Phir Se",
                        "artist": "Artist C",
                        "duration": 210000,
                        "spotifyUri": "spotify:track:track3",
                        "spotifyTrackId": "track3",
                        "artwork": "https://i.scdn.co/image/phir_se_art_789"
                    }
                ],
                "truncated": false,
                "note": null,
                "error": null
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PlaylistExtractResponse>(jsonPayload)
        assertEquals(3, parsed.tracks.size)

        val track1 = parsed.tracks[0]
        val track2 = parsed.tracks[1]
        val track3 = parsed.tracks[2]

        // Ensure each track has its own distinct artwork
        assertEquals("https://i.scdn.co/image/hum_se_hai_art_123", track1.getEffectiveArtwork())
        assertEquals("https://i.scdn.co/image/tere_ho_ke_art_456", track2.getEffectiveArtwork())
        assertEquals("https://i.scdn.co/image/phir_se_art_789", track3.getEffectiveArtwork())

        assertNotEquals(track1.getEffectiveArtwork(), track2.getEffectiveArtwork())
        assertNotEquals(track1.getEffectiveArtwork(), parsed.playlist?.image)
    }

    @Test
    fun testTwoPhaseSyncAndCreatePlaylistFlow() = runBlocking {
        val viewModel = PlaylistSyncViewModel()
        viewModel.onUrlChange("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M")

        // 1. Clone Playlist triggers extraction
        viewModel.clonePlaylist()

        // Wait for extraction to complete into Synced state
        var attempts = 0
        while (viewModel.syncState.value !is PlaylistSyncState.Synced && attempts < 100) {
            kotlinx.coroutines.delay(100)
            attempts++
        }

        val syncedState = viewModel.syncState.value
        assertTrue("Expected Synced state but was $syncedState", syncedState is PlaylistSyncState.Synced)
        val synced = syncedState as PlaylistSyncState.Synced
        assertTrue(synced.originalPlaylistName.isNotBlank())
        assertTrue(synced.syncedTrackCount > 0)
        assertEquals(synced.syncedTrackCount, synced.tracks.size)

        // 2. User taps Create Playlist
        var createdPlaylistId: String? = null
        viewModel.createPlaylist { pl ->
            createdPlaylistId = pl.id
        }

        attempts = 0
        while (viewModel.syncState.value !is PlaylistSyncState.Success && attempts < 20) {
            kotlinx.coroutines.delay(50)
            attempts++
        }

        val successState = viewModel.syncState.value
        assertTrue("Expected Success state but was $successState", successState is PlaylistSyncState.Success)
        val success = successState as PlaylistSyncState.Success
        assertEquals(synced.originalPlaylistName, success.playlist.title)
        assertEquals(synced.syncedTrackCount, success.playlist.trackCount)

        // 3. Verify in PlaylistRepository
        val libraryPlaylist = PlaylistRepository.getPlaylistById(success.playlist.id)
        assertNotNull(libraryPlaylist)
        assertEquals(synced.originalPlaylistName, libraryPlaylist?.title)
        assertEquals(synced.syncedTrackCount, libraryPlaylist?.tracks?.size)
    }
}
