package com.example

import com.example.data.remote.ApiClient
import com.example.data.remote.models.PlaybackResolveRequest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApiVerificationTest {

    private val api = ApiClient.apiService

    @Test
    fun verifyAllProductionApis() = runBlocking {
        println("==================================================")
        println("STARTING API VERIFICATION FOR https://spotify2.ai.studio")
        println("==================================================")

        // 1. Health
        try {
            val res = api.checkHealth()
            println("[1] HEALTH CHECK: Code=${res.code()}, Success=${res.body()?.success}, Data=${res.body()?.data}")
        } catch (e: Exception) {
            println("[1] HEALTH CHECK FAILED: ${e.message}")
        }

        // 2. Home
        try {
            val res = api.getHome()
            val quickPicksCount = res.body()?.data?.quickPicks?.size ?: 0
            val trendingCount = res.body()?.data?.trending?.size ?: 0
            println("[2] HOME FEED: Code=${res.code()}, Success=${res.body()?.success}, QuickPicks=$quickPicksCount, Trending=$trendingCount")
        } catch (e: Exception) {
            println("[2] HOME FEED FAILED: ${e.message}")
        }

        // 3. Search
        try {
            val res = api.search(query = "Arijit Singh")
            val songsCount = res.body()?.data?.songs?.size ?: 0
            val artistsCount = res.body()?.data?.artists?.size ?: 0
            println("[3] SEARCH q=Arijit Singh: Code=${res.code()}, Success=${res.body()?.success}, Songs=$songsCount, Artists=$artistsCount")
        } catch (e: Exception) {
            println("[3] SEARCH FAILED: ${e.message}")
        }

        // 4. Search Suggestions
        try {
            val res = api.getSearchSuggestions(query = "kesariya")
            val count = res.body()?.data?.size ?: 0
            println("[4] SEARCH SUGGESTIONS: Code=${res.code()}, Count=$count, First=${res.body()?.data?.firstOrNull()?.title}")
        } catch (e: Exception) {
            println("[4] SEARCH SUGGESTIONS FAILED: ${e.message}")
        }

        // 5. Playback Resolve
        try {
            val res = api.resolvePlayback(
                PlaybackResolveRequest(
                    trackId = "saavn-DF6eazs2",
                    title = "Tum Hi Ho",
                    artist = "Arijit Singh",
                    duration = 262
                )
            )
            val streamUrl = res.body()?.data?.stream?.url
            val bitrate = res.body()?.data?.stream?.mimeType
            println("[5] PLAYBACK RESOLVE: Code=${res.code()}, Success=${res.body()?.success}, StreamUrl=${streamUrl?.take(60)}..., MimeType=$bitrate")
        } catch (e: Exception) {
            println("[5] PLAYBACK RESOLVE FAILED: ${e.message}")
        }

        // 6. Lyrics
        try {
            val res = api.getLyrics(id = "saavn-DF6eazs2", title = "Tum Hi Ho", artist = "Arijit Singh")
            val linesCount = res.body()?.data?.lines?.size ?: 0
            println("[6] LYRICS: Code=${res.code()}, Success=${res.body()?.success}, Synced=${res.body()?.data?.synced}, Lines=$linesCount")
        } catch (e: Exception) {
            println("[6] LYRICS FAILED: ${e.message}")
        }

        // 7. Radio
        try {
            val res = api.getRadio(seedType = "song", seedId = "saavn-DF6eazs2", seedTitle = "Tum Hi Ho")
            val count = res.body()?.data?.size ?: 0
            println("[7] RADIO: Code=${res.code()}, TracksCount=$count")
        } catch (e: Exception) {
            println("[7] RADIO FAILED: ${e.message}")
        }

        // 8. Specific Song Test: Par Ab Jo Aayegi Tu
        try {
            val searchRes = api.search(query = "Par Ab Jo Aayegi Tu")
            val song = searchRes.body()?.data?.songs?.firstOrNull()
            println("[8] SONG SEARCH 'Par Ab Jo Aayegi Tu': Found Title='${song?.title}', Artist='${song?.artist}', ID='${song?.id}'")
            if (song != null) {
                val resolveRes = api.resolvePlayback(
                    PlaybackResolveRequest(
                        trackId = song.id,
                        title = song.title,
                        artist = song.artist,
                        duration = song.duration
                    )
                )
                val streamData = resolveRes.body()?.data?.stream
                println("[8] SONG RESOLVE 'Par Ab Jo Aayegi Tu': StreamUrl=${streamData?.url}, DescriptorType=${streamData?.descriptorType}, Fallbacks=${streamData?.fallbackUrls}")
            }
        } catch (e: Exception) {
            println("[8] SONG 'Par Ab Jo Aayegi Tu' FAILED: ${e.message}")
        }
    }
}
