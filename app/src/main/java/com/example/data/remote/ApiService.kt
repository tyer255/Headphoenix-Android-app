package com.example.data.remote

import com.example.data.remote.models.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    
    @GET("health")
    suspend fun checkHealth(): Response<ApiResponse<HealthStatusDto>>

    @GET("home")
    suspend fun getHome(): Response<ApiResponse<HomeDto>>

    @GET("search")
    suspend fun search(
        @Query("q") query: String
    ): Response<ApiResponse<SearchDto>>

    @GET("search/suggestions")
    suspend fun getSearchSuggestions(
        @Query("q") query: String
    ): Response<ApiResponse<List<SearchSuggestionDto>>>

    @GET("track/{id}")
    suspend fun getTrack(@Path("id") id: String): Response<ApiResponse<TrackDto>>

    @GET("artist/{id}")
    suspend fun getArtist(@Path("id") id: String): Response<ApiResponse<ArtistDto>>

    @GET("album/{id}")
    suspend fun getAlbum(@Path("id") id: String): Response<ApiResponse<AlbumDto>>

    @GET("playlist/{id}")
    suspend fun getPlaylist(@Path("id") id: String): Response<ApiResponse<PlaylistDto>>

    @GET("lyrics/{id}")
    suspend fun getLyrics(
        @Path("id") id: String,
        @Query("title") title: String? = null,
        @Query("artist") artist: String? = null
    ): Response<ApiResponse<LyricsDto>>

    @GET("radio")
    suspend fun getRadio(
        @Query("seedType") seedType: String = "song",
        @Query("seedId") seedId: String,
        @Query("seedTitle") seedTitle: String? = null
    ): Response<ApiResponse<List<TrackDto>>>

    @GET("canvas")
    suspend fun getCanvas(@Query("id") id: String): Response<ApiResponse<CanvasDto>>

    @POST("playback/resolve")
    suspend fun resolvePlayback(@Body request: PlaybackResolveRequest): Response<ApiResponse<PlaybackResolveDataDto>>

    @POST("playlist/extract")
    suspend fun extractPlaylist(@Body request: PlaylistExtractRequest): Response<PlaylistExtractResponse>
}
