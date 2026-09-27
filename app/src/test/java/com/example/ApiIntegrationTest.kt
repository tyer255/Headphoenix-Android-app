package com.example

import com.example.data.remote.ApiClient
import com.example.data.remote.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ApiIntegrationTest {

    @Test
    fun testLiveApiConnection() = runBlocking {
        println("--- STARTING API VERIFICATION ---")

        val api = ApiClient.apiService

        // 1. GET /api/health
        try {
            val healthRes = api.checkHealth()
            println("HEALTH HTTP STATUS: ${healthRes.code()}")
            if (healthRes.isSuccessful) {
                println("HEALTH DATA RECEIVED: YES")
                println("HEALTH CONTENT: ${healthRes.body()}")
            } else {
                println("HEALTH DATA RECEIVED: NO")
                println("HEALTH ERROR: ${healthRes.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("HEALTH ERROR: ${e.message}")
        }

        // 2. GET /api/home
        try {
            val homeRes = api.getHome()
            println("HOME HTTP STATUS: ${homeRes.code()}")
            if (homeRes.isSuccessful) {
                println("HOME DATA RECEIVED: YES")
                println("HOME CONTENT: ${homeRes.body()?.data?.quickPicks?.size} top charts, ${homeRes.body()?.data?.popularArtists?.size} artists")
            } else {
                println("HOME DATA RECEIVED: NO")
                println("HOME ERROR: ${homeRes.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("HOME ERROR: ${e.message}")
        }

        // 3. GET /api/search?q=tum%20hi%20ho
        try {
            val searchRes = api.search("tum hi ho")
            println("SEARCH HTTP STATUS: ${searchRes.code()}")
            if (searchRes.isSuccessful) {
                println("SEARCH DATA RECEIVED: YES")
                println("SEARCH CONTENT (Songs count): ${searchRes.body()?.data?.songs?.size}")
                println("SEARCH TOP RESULT: ${searchRes.body()?.data?.topResult?.type}")
            } else {
                println("SEARCH DATA RECEIVED: NO")
                println("SEARCH ERROR: ${searchRes.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("SEARCH ERROR: ${e.message}")
            e.printStackTrace()
        }

        // 4. GET /api/search/suggestions?q=arijit
        try {
            val suggRes = api.getSearchSuggestions("arijit")
            println("SUGGESTIONS HTTP STATUS: ${suggRes.code()}")
            if (suggRes.isSuccessful) {
                println("SUGGESTIONS DATA RECEIVED: YES")
                println("SUGGESTIONS CONTENT (Count): ${suggRes.body()?.data?.size}")
            } else {
                println("SUGGESTIONS DATA RECEIVED: NO")
                println("SUGGESTIONS ERROR: ${suggRes.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("SUGGESTIONS ERROR: ${e.message}")
        }

        // 5. GET /api/track/56zZ48jdyY2oDXHVnwg5Di
        try {
            val trackRes = api.getTrack("56zZ48jdyY2oDXHVnwg5Di")
            println("TRACK HTTP STATUS: ${trackRes.code()}")
            if (trackRes.isSuccessful) {
                println("TRACK DATA RECEIVED: YES")
                println("TRACK CONTENT: ${trackRes.body()?.data?.title} by ${trackRes.body()?.data?.artist}")
            } else {
                println("TRACK DATA RECEIVED: NO")
                println("TRACK ERROR: ${trackRes.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("TRACK ERROR: ${e.message}")
        }

        // 6. POST /api/playback/resolve
        try {
            val resolveRes = api.resolvePlayback(PlaybackResolveRequest("56zZ48jdyY2oDXHVnwg5Di"))
            println("RESOLVE HTTP STATUS: ${resolveRes.code()}")
            if (resolveRes.isSuccessful) {
                println("RESOLVE DATA RECEIVED: YES")
                println("RESOLVE CONTENT: URL -> ${resolveRes.body()?.data?.stream?.url}")
            } else {
                println("RESOLVE DATA RECEIVED: NO")
                println("RESOLVE ERROR: ${resolveRes.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            println("RESOLVE ERROR: ${e.message}")
        }

        // 7. Test "Par Ab Jo Aayegi Tu" — AUR
        try {
            println("--- TESTING PAR AB JO AAYEGI TU (AUR) RESOLUTION ---")
            val parAbJoReq = PlaybackResolveRequest(
                trackId = "4cOdK2wGLETKBW3PvgPWqT",
                title = "Par Ab Jo Aayegi Tu",
                artist = "AUR",
                duration = 185
            )
            val res = api.resolvePlayback(parAbJoReq)
            println("PAR AB JO AAYEGI TU STATUS: ${res.code()}")
            assert(res.isSuccessful) { "Resolution failed: ${res.errorBody()?.string()}" }
            val data = res.body()?.data
            assert(data != null) { "Data is null" }
            println("RESOLVED TITLE: ${data?.title}")
            println("RESOLVED ARTIST: ${data?.artist}")
            println("DURATION: ${data?.duration} seconds (Full length: ${data?.duration ?: 0 > 30})")
            println("STREAM URL / DESCRIPTOR: ${data?.stream?.url}")
            assert((data?.duration ?: 0) >= 180) { "Expected duration >= 180s, got ${data?.duration}" }
            assert(data?.stream?.url?.contains("Rk4WU-BTje4") == true || data?.stream?.url?.startsWith("youtube:") == true) {
                "Expected YouTube descriptor, got ${data?.stream?.url}"
            }
            println("--- PAR AB JO AAYEGI TU RESOLUTION TEST PASSED ---")
        } catch (e: Exception) {
            println("PAR AB JO AAYEGI TU ERROR: ${e.message}")
            throw e
        }

        println("--- ENDING API VERIFICATION ---")
    }
}
