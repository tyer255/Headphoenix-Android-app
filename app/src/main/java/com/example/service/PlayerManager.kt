package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.YouTubeResolver
import com.example.data.MusicRepository
import com.example.data.PlaybackDevice
import com.example.data.PlaybackHistoryRepository
import com.example.data.PlaylistRepository
import com.example.data.remote.LyricsResolver
import com.example.data.remote.models.LyricsDto
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

object PlayerManager {

    private const val TAG = "PlayerManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val repository = MusicRepository()

    private var appContext: Context? = null
    private var rawExoPlayer: ExoPlayer? = null
    private var forwardingPlayer: ForwardingPlayer? = null
    private var youTubeAudioEngine: YouTubeAudioEngine? = null
    private var isUsingYouTubeEngine = false
    private var isFetchingTrackId: String? = null
    private val playbackUrlCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    // State Flows
    private val _currentTrack = MutableStateFlow<TrackDto?>(null)
    val currentTrack: StateFlow<TrackDto?> = _currentTrack.asStateFlow()

    private val _recommendedTracks = MutableStateFlow<List<TrackDto>>(emptyList())
    val recommendedTracks: StateFlow<List<TrackDto>> = _recommendedTracks.asStateFlow()

    private val recommendationArtCache = java.util.concurrent.ConcurrentHashMap<String, ByteArray>()
    var onRecommendationsUpdatedListener: (() -> Unit)? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0) // 0: off, 1: all, 2: one
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _savedTracks = MutableStateFlow<Set<String>>(emptySet())
    val savedTracks: StateFlow<Set<String>> = _savedTracks.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0)
    val currentPositionSec: StateFlow<Int> = _currentPositionSec.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationSec = MutableStateFlow(30)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private val _volume = MutableStateFlow(0.85f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _queue = MutableStateFlow<List<TrackDto>>(emptyList())
    val queue: StateFlow<List<TrackDto>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _devices = MutableStateFlow(
        listOf(
            PlaybackDevice("d1", "This Device (Android)", "Smartphone", true, 85)
        )
    )
    val devices: StateFlow<List<PlaybackDevice>> = _devices.asStateFlow()

    private val _currentDevice = MutableStateFlow(_devices.value[0])
    val currentDevice: StateFlow<PlaybackDevice> = _currentDevice.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _currentCanvasUrl = MutableStateFlow<String?>(null)
    val currentCanvasUrl: StateFlow<String?> = _currentCanvasUrl.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsDto?>(null)
    val currentLyrics: StateFlow<LyricsDto?> = _currentLyrics.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val pendingFallbackUrls = mutableListOf<String>()

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        ensurePlayerInitialized(context.applicationContext)
        ensureYouTubeEngineInitialized(context.applicationContext)

        scope.launch {
            PlaylistRepository.likedTracks.collect { tracks ->
                _savedTracks.value = tracks.map { it.id }.toSet()
            }
        }
    }

    private fun ensureYouTubeEngineInitialized(context: Context) {
        if (youTubeAudioEngine != null) return
        val engine = YouTubeAudioEngine(context)
        engine.onStateChangedListener = { isPlaying ->
            if (isUsingYouTubeEngine) {
                _isPlaying.value = isPlaying
            }
        }
        engine.onProgressListener = { currSec, durSec ->
            if (isUsingYouTubeEngine) {
                _currentPositionSec.value = currSec.toInt().coerceAtLeast(0)
                _currentPositionMs.value = (currSec * 1000L).toLong().coerceAtLeast(0L)
                if (durSec > 0f) {
                    _durationSec.value = durSec.toInt().coerceAtLeast(1)
                }
            }
        }
        engine.onEndedListener = {
            if (isUsingYouTubeEngine) {
                if (_repeatMode.value == 2) {
                    youTubeAudioEngine?.seekTo(0f)
                    youTubeAudioEngine?.play()
                } else {
                    skipNext()
                }
            }
        }
        engine.onErrorListener = { errorCode ->
            Log.e(TAG, "YouTubeEngine error: $errorCode")
        }
        youTubeAudioEngine = engine
    }

    private fun ensurePlayerInitialized(context: Context): ExoPlayer {
        rawExoPlayer?.let { return it }

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                20000, // minBufferMs
                60000, // maxBufferMs
                250,   // bufferForPlaybackMs (instantaneous start in 250ms!)
                500    // bufferForPlaybackAfterRebufferMs (ultra-fast 500ms!)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isUsingYouTubeEngine) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        isFetchingTrackId = null
                        _isLoading.value = false
                        startProgressLoop()
                        startPlaybackService()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (!isUsingYouTubeEngine) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            // Only show circular loading indicator during the initial music fetch of a newly requested track
                            if (isFetchingTrackId != null && isFetchingTrackId == _currentTrack.value?.id) {
                                _isLoading.value = true
                            }
                        }
                        Player.STATE_READY -> {
                            val d = player.duration
                            if (d > 0) {
                                _durationSec.value = (d / 1000).toInt().coerceAtLeast(1)
                            }
                            isFetchingTrackId = null
                            _isLoading.value = false
                        }
                        Player.STATE_ENDED -> {
                            isFetchingTrackId = null
                            _isLoading.value = false
                            if (_repeatMode.value == 2) {
                                rawExoPlayer?.seekTo(0)
                                rawExoPlayer?.play()
                            } else {
                                skipNext()
                            }
                        }
                        Player.STATE_IDLE -> {
                            _isLoading.value = false
                        }
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.w(TAG, "PlaybackException: ${error.errorCodeName} (${error.errorCode})", error)
                if (pendingFallbackUrls.isNotEmpty()) {
                    val nextUrl = pendingFallbackUrls.removeAt(0)
                    Log.i(TAG, "Attempting playback with fallback URL: $nextUrl")
                    try {
                        val current = _currentTrack.value
                        val mediaItem = buildMediaItem(nextUrl, current)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                        _isLoading.value = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Fallback playback attempt failed", e)
                        _isLoading.value = false
                        _isPlaying.value = false
                    }
                } else {
                    _isLoading.value = false
                    _isPlaying.value = false
                }
            }
        })

        player.volume = _volume.value
        rawExoPlayer = player
        return player
    }

    fun getPlayerForSession(context: Context): Player {
        val exo = ensurePlayerInitialized(context)
        if (forwardingPlayer != null) return forwardingPlayer!!

        forwardingPlayer = object : ForwardingPlayer(exo) {
            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun seekToNext() {
                skipNext()
            }

            override fun seekToNextMediaItem() {
                skipNext()
            }

            override fun seekToPrevious() {
                skipPrevious()
            }

            override fun seekToPreviousMediaItem() {
                skipPrevious()
            }
        }
        return forwardingPlayer!!
    }

    private fun startPlaybackService() {
        val ctx = appContext ?: return
        try {
            val intent = Intent(ctx, PlaybackService::class.java)
            ctx.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PlaybackService", e)
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                delay(150)
                rawExoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val posMs = player.currentPosition.coerceAtLeast(0L)
                        _currentPositionMs.value = posMs
                        _currentPositionSec.value = (posMs / 1000).toInt().coerceAtLeast(0)
                        val totalDuration = player.duration
                        if (totalDuration > 0) {
                            _durationSec.value = (totalDuration / 1000).toInt().coerceAtLeast(1)
                        }
                    }
                }
            }
        }
    }

    private fun buildMediaItem(streamUrl: String, track: TrackDto?): MediaItem {
        val artworkUriString = track?.images?.large ?: track?.images?.medium ?: track?.images?.small ?: ""
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track?.title ?: "Unknown Track")
            .setArtist(track?.artist ?: "Unknown Artist")
            .setDisplayTitle(track?.title ?: "Unknown Track")
            .setSubtitle(track?.artist ?: "Unknown Artist")
            .setIsPlayable(true)

        if (artworkUriString.isNotBlank()) {
            metadataBuilder.setArtworkUri(Uri.parse(artworkUriString))
        }

        return MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(track?.id ?: "track_${System.currentTimeMillis()}")
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun loadArtworkDataIntoMetadata(track: TrackDto) {
        val ctx = appContext ?: return
        val artUrl = track.images?.large ?: track.images?.medium ?: track.images?.small ?: return
        if (artUrl.isBlank()) return

        scope.launch(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader(ctx)
                val request = ImageRequest.Builder(ctx)
                    .data(artUrl)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable) {
                        val bitmap = drawable.bitmap
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val byteArray = stream.toByteArray()

                        scope.launch(Dispatchers.Main) {
                            if (_currentTrack.value?.id == track.id) {
                                rawExoPlayer?.let { player ->
                                    val currentItem = player.currentMediaItem
                                    if (currentItem != null && currentItem.mediaId == track.id) {
                                        val updatedMeta = currentItem.mediaMetadata.buildUpon()
                                            .setArtworkData(byteArray, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                            .build()
                                        val updatedItem = currentItem.buildUpon()
                                            .setMediaMetadata(updatedMeta)
                                            .build()
                                        player.replaceMediaItem(player.currentMediaItemIndex, updatedItem)
                                        Log.d(TAG, "Updated MediaSession notification with high-res artwork data for ${track.title}")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching artwork bitmap for notification: ${e.message}")
            }
        }
    }

    fun playTrack(track: TrackDto, newQueue: List<TrackDto>? = null) {
        val ctx = appContext ?: return
        ensurePlayerInitialized(ctx)

        val currentQueueList = if (newQueue != null) {
            newQueue
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value + track
        } else {
            _queue.value
        }

        _queue.value = currentQueueList
        val index = currentQueueList.indexOfFirst { it.id == track.id }
        _currentQueueIndex.value = if (index != -1) index else 0

        _currentTrack.value = track
        _currentPositionSec.value = 0
        _currentPositionMs.value = 0L
        isFetchingTrackId = track.id
        _isLoading.value = true
        _isPlaying.value = false
        PlaybackHistoryRepository.addTrackToHistory(track)
        _durationSec.value = track.duration.coerceAtLeast(15)

        startProgressLoop()
        startPlaybackService()

        // Fetch recommendations for this track and cache artwork for notification recommendations
        scope.launch {
            try {
                val recs = repository.getRecommendations(track.id, track.title, track.artist)
                val filtered = recs.filterNot { it.id == track.id }.take(6)
                if (filtered.isNotEmpty()) {
                    _recommendedTracks.value = filtered
                    onRecommendationsUpdatedListener?.invoke()
                    cacheRecommendationArtworks(filtered)
                } else {
                    val fallback = _queue.value.filterNot { it.id == track.id }.take(4)
                    if (fallback.isNotEmpty()) {
                        _recommendedTracks.value = fallback
                        onRecommendationsUpdatedListener?.invoke()
                        cacheRecommendationArtworks(fallback)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching recommendations: ${e.message}")
                val fallback = _queue.value.filterNot { it.id == track.id }.take(4)
                if (fallback.isNotEmpty()) {
                    _recommendedTracks.value = fallback
                    onRecommendationsUpdatedListener?.invoke()
                }
            }
        }

        // Fetch lyrics asynchronously
        _currentLyrics.value = null
        _currentCanvasUrl.value = null

        scope.launch {
            try {
                val canvasUrl = com.example.data.remote.CanvasResolver.resolveCanvas(track)
                _currentCanvasUrl.value = canvasUrl
            } catch (e: Exception) {
                Log.d(TAG, "Canvas resolve error: ${e.message}")
                _currentCanvasUrl.value = null
            }
        }

        scope.launch {
            try {
                val lyrics = LyricsResolver.resolveLyrics(track)
                _currentLyrics.value = lyrics ?: LyricsDto(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    synced = false,
                    lines = emptyList()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching lyrics", e)
                _currentLyrics.value = LyricsDto(
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    synced = false,
                    lines = emptyList()
                )
            }
        }

        val localStreamUrl = com.example.data.AppDownloadManager.getLocalStreamUrl(ctx, track.id)
        if (!localStreamUrl.isNullOrEmpty()) {
            Log.i(TAG, "Playing from offline download: $localStreamUrl")
            isUsingYouTubeEngine = false
            youTubeAudioEngine?.stop()
            val offlineTrack = com.example.data.AppDownloadManager.getDownloadedTrack(track.id) ?: track
            playAudioUrls(localStreamUrl, emptyList(), offlineTrack)
            return
        }

        if (track.streamUrl?.startsWith("file://") == true) {
            isUsingYouTubeEngine = false
            youTubeAudioEngine?.stop()
            playAudioUrls(track.streamUrl, emptyList(), track)
        } else {
            val titleArtistKey = "${track.title.trim().lowercase()}__${track.artist.trim().lowercase()}"
            val cachedUrl = playbackUrlCache[track.id] ?: playbackUrlCache[titleArtistKey]
            if (cachedUrl != null) {
                isUsingYouTubeEngine = false
                youTubeAudioEngine?.stop()
                playAudioUrls(cachedUrl, emptyList(), track)
                preResolveNextTrack()
            } else {
                scope.launch {
                    val resolvedPair = resolveTrackStreamWithFallbacks(track)
                    if (resolvedPair != null) {
                        val (primaryUrl, fallbacks) = resolvedPair
                        isUsingYouTubeEngine = false
                        youTubeAudioEngine?.stop()
                        playAudioUrls(primaryUrl, fallbacks, track)
                        preResolveNextTrack()
                    } else {
                        // Attempt headless YouTube engine fallback if we have a query
                        try {
                            val ytQuery = "${track.title} ${track.artist}".trim()
                            val searchRes = repository.search(ytQuery)
                            val matched = searchRes?.songs?.firstOrNull()
                            if (matched != null) {
                                val resolvedData = repository.resolvePlayback(matched.id, matched.title, matched.artist, matched.duration)
                                val fallbackStreamUrl = resolvedData?.stream?.url
                                if (!fallbackStreamUrl.isNullOrBlank() && !fallbackStreamUrl.startsWith("youtube:")) {
                                    isUsingYouTubeEngine = false
                                    youTubeAudioEngine?.stop()
                                    playAudioUrls(fallbackStreamUrl, resolvedData.stream.fallbackUrls ?: emptyList(), track)
                                    return@launch
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Final fallback failed: ${e.message}")
                        }

                        Log.e(TAG, "No playback stream available for ${track.title} - ${track.artist}")
                        _isLoading.value = false
                        _isPlaying.value = false
                    }
                }
            }
        }
    }

    private suspend fun resolveTrackStreamWithFallbacks(track: TrackDto): Pair<String, List<String>>? {
        val cacheKey = track.id
        val titleArtistKey = "${track.title.trim().lowercase()}__${track.artist.trim().lowercase()}"

        playbackUrlCache[cacheKey]?.let { return Pair(it, emptyList()) }
        playbackUrlCache[titleArtistKey]?.let { return Pair(it, emptyList()) }

        if (!track.streamUrl.isNullOrBlank() && (track.streamUrl.startsWith("http") || track.streamUrl.startsWith("file"))) {
            playbackUrlCache[cacheKey] = track.streamUrl
            return Pair(track.streamUrl, emptyList())
        }

        // Attempt 1: Direct backend resolve
        try {
            val data = repository.resolvePlayback(track.id, track.title, track.artist, track.duration)
            val stream = data?.stream
            val url = stream?.url
            if (!url.isNullOrBlank()) {
                if (url.startsWith("youtube:") || stream.descriptorType == "youtube") {
                    val ytId = url.removePrefix("youtube:").trim()
                    val directAudio = YouTubeResolver.resolveStreamUrl(ytId)
                    if (directAudio != null) {
                        playbackUrlCache[cacheKey] = directAudio
                        playbackUrlCache[titleArtistKey] = directAudio
                        return Pair(directAudio, emptyList())
                    }
                } else {
                    playbackUrlCache[cacheKey] = url
                    playbackUrlCache[titleArtistKey] = url
                    val fallbacks = stream.fallbackUrls ?: emptyList()
                    return Pair(url, fallbacks)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Attempt 1 resolve error: ${e.message}")
        }

        // Attempt 2: Search backend by Title + Artist
        try {
            val searchQuery = "${track.title} ${track.artist}".trim()
            val searchRes = repository.search(searchQuery)
            val matched = searchRes?.songs?.firstOrNull()
            if (matched != null) {
                val data = repository.resolvePlayback(matched.id, matched.title, matched.artist, matched.duration)
                val stream = data?.stream
                val url = stream?.url
                if (!url.isNullOrBlank() && !url.startsWith("youtube:")) {
                    playbackUrlCache[cacheKey] = url
                    playbackUrlCache[titleArtistKey] = url
                    playbackUrlCache[matched.id] = url
                    return Pair(url, stream.fallbackUrls ?: emptyList())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Attempt 2 resolve error: ${e.message}")
        }

        // Attempt 3: Fast YouTube direct stream resolve
        try {
            val ytUrl = YouTubeResolver.searchAndResolve("${track.title} ${track.artist}".trim())
            if (!ytUrl.isNullOrBlank()) {
                playbackUrlCache[cacheKey] = ytUrl
                playbackUrlCache[titleArtistKey] = ytUrl
                return Pair(ytUrl, emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Attempt 3 resolve error: ${e.message}")
        }

        return null
    }

    private suspend fun resolveTrackStreamUrl(track: TrackDto): String? {
        return resolveTrackStreamWithFallbacks(track)?.first
    }

    private fun preResolveNextTrack() {
        scope.launch(Dispatchers.IO) {
            try {
                val q = _queue.value
                val nextIdx = _currentQueueIndex.value + 1
                if (nextIdx in q.indices) {
                    val nextTrack = q[nextIdx]
                    resolveTrackStreamUrl(nextTrack)
                }
            } catch (e: Exception) {
                // Ignore background pre-resolve error
            }
        }
    }

    private fun playAudioUrls(primaryUrl: String, fallbackUrls: List<String>, track: TrackDto) {
        try {
            isUsingYouTubeEngine = false
            youTubeAudioEngine?.stop()
            val ctx = appContext ?: return
            val player = ensurePlayerInitialized(ctx)
            player.stop()
            player.clearMediaItems()
            pendingFallbackUrls.clear()
            pendingFallbackUrls.addAll(fallbackUrls)

            val mediaItem = buildMediaItem(primaryUrl, track)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _isPlaying.value = false
            _isLoading.value = true

            startPlaybackService()
            loadArtworkDataIntoMetadata(track)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up ExoPlayer", e)
            _isLoading.value = false
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value || _isLoading.value) {
            pause()
        } else {
            play()
        }
    }

    fun pause() {
        _isLoading.value = false
        _isPlaying.value = false
        if (isUsingYouTubeEngine) {
            youTubeAudioEngine?.pause()
        } else {
            rawExoPlayer?.pause()
        }
    }

    fun play() {
        if (isUsingYouTubeEngine) {
            youTubeAudioEngine?.play()
        } else {
            rawExoPlayer?.play()
        }
        startPlaybackService()
    }

    fun seekToMs(ms: Long) {
        val maxMs = _durationSec.value * 1000L
        val clamped = ms.coerceIn(0L, maxMs)
        if (isUsingYouTubeEngine) {
            youTubeAudioEngine?.seekTo(clamped / 1000f)
        } else {
            rawExoPlayer?.seekTo(clamped)
        }
        _currentPositionMs.value = clamped
        _currentPositionSec.value = (clamped / 1000).toInt()
    }

    fun seekTo(seconds: Int) {
        seekToMs(seconds * 1000L)
    }

    fun setVolume(volumeLevel: Float) {
        val clamped = volumeLevel.coerceIn(0f, 1f)
        _volume.value = clamped
        rawExoPlayer?.volume = clamped
        youTubeAudioEngine?.setVolume(clamped)
    }

    fun toggleMute() {
        if (_volume.value > 0f) {
            setVolume(0f)
        } else {
            setVolume(0.85f)
        }
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        var nextIndex = _currentQueueIndex.value
        if (_isShuffle.value && q.size > 1) {
            var randomIndex = nextIndex
            while (randomIndex == nextIndex) {
                randomIndex = (0 until q.size).random()
            }
            nextIndex = randomIndex
        } else {
            nextIndex++
            if (nextIndex >= q.size) {
                if (_repeatMode.value == 1) {
                    nextIndex = 0
                } else {
                    // Try to auto-fetch recommendations if queue reached the end
                    fetchAutoplayRecommendationsAndPlay()
                    return
                }
            }
        }
        if (nextIndex in q.indices) {
            playTrack(q[nextIndex], q)
        }
    }

    private fun fetchAutoplayRecommendationsAndPlay() {
        val current = _currentTrack.value
        if (current == null) {
            rawExoPlayer?.pause()
            _isPlaying.value = false
            return
        }
        scope.launch {
            try {
                val recs: List<TrackDto> = repository.getRecommendations(current.id, current.title, current.artist)
                val newTracks = recs.filterNot { rec -> _queue.value.any { it.id == rec.id } }
                if (newTracks.isNotEmpty()) {
                    val updatedQueue = _queue.value + newTracks
                    _queue.value = updatedQueue
                    playTrack(newTracks.first(), updatedQueue)
                } else {
                    rawExoPlayer?.pause()
                    _isPlaying.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-fetch recommendations", e)
                rawExoPlayer?.pause()
                _isPlaying.value = false
            }
        }
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        if ((rawExoPlayer?.currentPosition ?: 0L) > 3000L) {
            seekTo(0)
            return
        }

        var prevIndex = _currentQueueIndex.value - 1
        if (prevIndex < 0) {
            prevIndex = if (_repeatMode.value == 1) q.size - 1 else 0
        }
        if (prevIndex in q.indices) {
            playTrack(q[prevIndex], q)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = (_repeatMode.value + 1) % 3
    }

    fun toggleSave(track: TrackDto) {
        PlaylistRepository.toggleLikeTrack(track)
    }

    fun toggleSave(trackId: String) {
        val current = _currentTrack.value
        if (current != null && current.id == trackId) {
            PlaylistRepository.toggleLikeTrack(current)
        } else {
            val trackInQueue = _queue.value.find { it.id == trackId }
            if (trackInQueue != null) {
                PlaylistRepository.toggleLikeTrack(trackInQueue)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        rawExoPlayer?.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutes.value = minutes
        if (minutes != null && minutes > 0) {
            sleepTimerJob = scope.launch {
                delay(minutes * 60 * 1000L)
                pause()
                _sleepTimerMinutes.value = null
            }
        }
    }

    fun selectDevice(device: PlaybackDevice) {
        _devices.value = _devices.value.map {
            it.copy(isActive = it.id == device.id)
        }
        _currentDevice.value = device.copy(isActive = true)
    }

    fun addToQueue(track: TrackDto) {
        _queue.value = _queue.value + track
    }

    fun removeFromQueue(index: Int) {
        val currentList = _queue.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _queue.value = currentList
            if (index < _currentQueueIndex.value) {
                _currentQueueIndex.value -= 1
            }
        }
    }

    fun playFromQueue(index: Int) {
        val q = _queue.value
        if (index in q.indices) {
            playTrack(q[index], q)
        }
    }

    fun clearQueue() {
        val current = _currentTrack.value
        if (current != null) {
            _queue.value = listOf(current)
            _currentQueueIndex.value = 0
        } else {
            _queue.value = emptyList()
            _currentQueueIndex.value = -1
        }
    }

    fun getRecommendedTracks(): List<TrackDto> {
        val recs = _recommendedTracks.value
        if (recs.isNotEmpty()) return recs
        val currentId = _currentTrack.value?.id
        val q = _queue.value.filterNot { it.id == currentId }
        if (q.isNotEmpty()) return q.take(6)
        return emptyList()
    }

    fun getArtworkDataForTrack(trackId: String): ByteArray? {
        return recommendationArtCache[trackId]
    }

    fun findTrackById(trackId: String): TrackDto? {
        if (_currentTrack.value?.id == trackId) return _currentTrack.value
        _recommendedTracks.value.find { it.id == trackId }?.let { return it }
        _queue.value.find { it.id == trackId }?.let { return it }
        return null
    }

    private fun cacheRecommendationArtworks(tracks: List<TrackDto>) {
        val ctx = appContext ?: return
        scope.launch(Dispatchers.IO) {
            val imageLoader = ImageLoader(ctx)
            for (t in tracks.take(6)) {
                val url = t.images?.large ?: t.images?.medium ?: t.images?.small ?: continue
                if (url.isBlank() || recommendationArtCache.containsKey(t.id)) continue
                try {
                    val req = ImageRequest.Builder(ctx)
                        .data(url)
                        .allowHardware(false)
                        .build()
                    val res = imageLoader.execute(req)
                    if (res is SuccessResult) {
                        val d = res.drawable
                        if (d is BitmapDrawable) {
                            val stream = ByteArrayOutputStream()
                            d.bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                            recommendationArtCache[t.id] = stream.toByteArray()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Failed caching thumb for ${t.title}: ${e.message}")
                }
            }
            scope.launch(Dispatchers.Main) {
                onRecommendationsUpdatedListener?.invoke()
            }
        }
    }
}
