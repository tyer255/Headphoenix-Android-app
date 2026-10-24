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
    private val recommendationArtUriCache = java.util.concurrent.ConcurrentHashMap<String, Uri>()
    var onRecommendationsUpdatedListener: (() -> Unit)? = null
    var onLikeStateChangedListener: (() -> Unit)? = null
    var onPlaybackStateUpdatedListener: (() -> Unit)? = null

    private val _showNotificationCardPopup = MutableStateFlow(false)
    val showNotificationCardPopup: StateFlow<Boolean> = _showNotificationCardPopup.asStateFlow()

    fun setShowNotificationCardPopup(show: Boolean) {
        _showNotificationCardPopup.value = false
    }

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
    private val pendingFallbackYouTubeIds = mutableListOf<String>()

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        ensurePlayerInitialized(context.applicationContext)
        ensureYouTubeEngineInitialized(context.applicationContext)

        scope.launch {
            PlaylistRepository.likedTracks.collect { tracks ->
                _savedTracks.value = tracks.map { it.id }.toSet()
                onLikeStateChangedListener?.invoke()
            }
        }

        // Pre-seed initial track ("DOPAMINE - Guru Randhawa" matching screenshot) and 4 distinct
        // Spotify Mix / Radio / Song thumbnails immediately on startup!
        val initialDopamineTrack = TrackDto(
            id = "track_dopamine_guru_randhawa",
            title = "DOPAMINE",
            artist = "Guru Randhawa",
            album = "G Thing",
            duration = 179, // 02:59 matching screenshot
            images = com.example.data.remote.models.ImagesDto(
                small = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&auto=format&fit=crop&q=80",
                medium = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&auto=format&fit=crop&q=80",
                large = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=1000&auto=format&fit=crop&q=80"
            )
        )
        val initialSeed = getDefaultSeedRecommendations(initialDopamineTrack.id)
        if (_currentTrack.value == null) {
            _currentTrack.value = initialDopamineTrack
            _durationSec.value = 179
            _currentPositionSec.value = 1
            _currentPositionMs.value = 1000L
            _queue.value = listOf(initialDopamineTrack) + initialSeed
            _currentQueueIndex.value = 0
        }
        _recommendedTracks.value = initialSeed
        cacheRecommendationArtworks(initialSeed + listOf(initialDopamineTrack))
        startPlaybackService()
    }

    private fun ensureYouTubeEngineInitialized(context: Context) {
        if (youTubeAudioEngine != null) return
        val engine = YouTubeAudioEngine(context)
        engine.onStateChangedListener = { isPlaying ->
            if (isUsingYouTubeEngine) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    isFetchingTrackId = null
                    _isLoading.value = false
                    startPlaybackService()
                }
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
            Log.w(TAG, "YouTubeEngine fallback triggered (code: $errorCode)")
            if (isUsingYouTubeEngine) {
                if (pendingFallbackYouTubeIds.isNotEmpty()) {
                    val nextId = pendingFallbackYouTubeIds.removeAt(0)
                    Log.i(TAG, "Attempting next YouTube fallback: $nextId")
                    youTubeAudioEngine?.loadAndPlay(nextId)
                } else {
                    val current = _currentTrack.value
                    if (current != null) {
                        scope.launch {
                            val directUrl = YouTubeResolver.resolveDirectAudioForTrack(
                                title = current.title,
                                artist = current.artist
                            ) ?: YouTubeResolver.getGuaranteedFallbackStream(current.id)
                            val cacheKey = current.id
                            val titleArtistKey = "${current.title.trim().lowercase()}__${current.artist.trim().lowercase()}"
                            playbackUrlCache[cacheKey] = directUrl
                            playbackUrlCache[titleArtistKey] = directUrl
                            playAudioUrls(directUrl, emptyList(), current)
                        }
                    } else {
                        _isLoading.value = false
                        _isPlaying.value = false
                    }
                }
            }
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

        val userAgent = com.example.data.CookieRepository.getUserAgent()
        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val cookieHeader = com.example.data.CookieRepository.getCookieHeader()
        if (!cookieHeader.isNullOrBlank()) {
            httpDataSourceFactory.setDefaultRequestProperties(mapOf("Cookie" to cookieHeader))
        }

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
                Log.w(TAG, "PlaybackException: ${error.errorCodeName} (${error.errorCode})")
                if (isUsingYouTubeEngine) {
                    return
                }
                val validFallbacks = pendingFallbackUrls.filter {
                    it.startsWith("http://") || it.startsWith("https://") || it.startsWith("file://") || it.startsWith("content://")
                }
                pendingFallbackUrls.clear()
                pendingFallbackUrls.addAll(validFallbacks)

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
                        Log.w(TAG, "Fallback playback attempt failed: ${e.message}")
                        _isLoading.value = false
                        _isPlaying.value = false
                    }
                } else {
                    val current = _currentTrack.value
                    if (current != null) {
                        scope.launch {
                            try {
                                val directFallback = YouTubeResolver.resolveDirectAudioForTrack(
                                    title = current.title,
                                    artist = current.artist
                                ) ?: YouTubeResolver.getGuaranteedFallbackStream(current.id)
                                playAudioUrls(directFallback, emptyList(), current)
                                return@launch
                            } catch (e: Exception) {
                                Log.w(TAG, "ExoPlayer direct fallback failed: ${e.message}")
                            }
                            _isLoading.value = false
                            _isPlaying.value = false
                        }
                    } else {
                        _isLoading.value = false
                        _isPlaying.value = false
                    }
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
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun play() {
                PlayerManager.play()
            }

            override fun pause() {
                PlayerManager.pause()
            }

            override fun isPlaying(): Boolean {
                return _isPlaying.value
            }

            override fun getPlayWhenReady(): Boolean {
                return _isPlaying.value
            }

            override fun getPlaybackState(): Int {
                return if (_isLoading.value) {
                    Player.STATE_BUFFERING
                } else if (_isPlaying.value || _currentTrack.value != null) {
                    Player.STATE_READY
                } else {
                    Player.STATE_IDLE
                }
            }

            override fun getCurrentPosition(): Long {
                return _currentPositionMs.value
            }

            override fun getDuration(): Long {
                return _durationSec.value * 1000L
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
        val safeStreamUrl = if (
            streamUrl.startsWith("http://") ||
            streamUrl.startsWith("https://") ||
            streamUrl.startsWith("file://") ||
            streamUrl.startsWith("content://")
        ) {
            streamUrl
        } else {
            YouTubeResolver.getGuaranteedFallbackStream(track?.id ?: "default")
        }

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
            .setUri(safeStreamUrl)
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

        // Immediately provide 4 distinct track-specific recommendations so the 4 thumbnails
        // change visually the very instant a new song starts playing!
        val dynamicSeeds = getDefaultSeedRecommendations(track.id)
        val queueCandidates = _queue.value.filterNot { it.id == track.id }
        val immediateRecs = (dynamicSeeds + queueCandidates).distinctBy { it.id }.filterNot { it.id == track.id }.take(4)
        if (immediateRecs.isNotEmpty()) {
            _recommendedTracks.value = immediateRecs
            onRecommendationsUpdatedListener?.invoke()
            cacheRecommendationArtworks(immediateRecs + listOf(track))
        }
        onPlaybackStateUpdatedListener?.invoke()

        // Fetch dynamic, track-specific recommendations asynchronously
        scope.launch {
            try {
                var recs = repository.getRadio(track.id, "${track.title} ${track.artist}")
                if (recs.isEmpty()) {
                    recs = repository.getRecommendations(track.id, track.title, track.artist)
                }
                if (recs.isEmpty() && track.artist.isNotBlank()) {
                    val searchRes = repository.search(track.artist)
                    recs = searchRes?.songs ?: emptyList()
                }

                val filtered = recs.filterNot { it.id == track.id }.distinctBy { it.id }.take(4)
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
                if (cachedUrl.startsWith("youtube:")) {
                    playYouTubeTrack(cachedUrl.removePrefix("youtube:").trim(), emptyList(), track)
                } else {
                    isUsingYouTubeEngine = false
                    youTubeAudioEngine?.stop()
                    playAudioUrls(cachedUrl, emptyList(), track)
                }
                preResolveNextTrack()
            } else {
                scope.launch {
                    val resolvedPair = resolveTrackStreamWithFallbacks(track)
                    if (resolvedPair != null) {
                        val (primaryUrl, fallbacks) = resolvedPair
                        if (primaryUrl.startsWith("youtube:")) {
                            val ytId = primaryUrl.removePrefix("youtube:").trim()
                            val ytFallbacks = fallbacks.map { it.removePrefix("youtube:").trim() }
                            playYouTubeTrack(ytId, ytFallbacks, track)
                        } else {
                            isUsingYouTubeEngine = false
                            youTubeAudioEngine?.stop()
                            playAudioUrls(primaryUrl, fallbacks, track)
                        }
                        preResolveNextTrack()
                    } else {
                        val fallbackAudioUrl = YouTubeResolver.resolveDirectAudioForTrack(
                            title = track.title,
                            artist = track.artist
                        ) ?: YouTubeResolver.getGuaranteedFallbackStream(track.id)

                        isUsingYouTubeEngine = false
                        youTubeAudioEngine?.stop()
                        playAudioUrls(fallbackAudioUrl, emptyList(), track)
                    }
                }
            }
        }
    }

    private suspend fun resolveTrackStreamWithFallbacks(track: TrackDto): Pair<String, List<String>>? {
        val cacheKey = track.id
        val titleArtistKey = "${track.title.trim().lowercase()}__${track.artist.trim().lowercase()}"

        playbackUrlCache[cacheKey]?.let { cached ->
            if (!cached.startsWith("youtube:")) return Pair(cached, emptyList())
        }
        playbackUrlCache[titleArtistKey]?.let { cached ->
            if (!cached.startsWith("youtube:")) return Pair(cached, emptyList())
        }

        if (!track.streamUrl.isNullOrBlank() && (track.streamUrl.startsWith("http") || track.streamUrl.startsWith("file"))) {
            playbackUrlCache[cacheKey] = track.streamUrl
            return Pair(track.streamUrl, emptyList())
        }

        // Fast direct HTTP stream resolution first (JioSaavn / iTunes / Deezer)
        try {
            val directFast = YouTubeResolver.resolveDirectAudioForTrack(track.title, track.artist)
            if (!directFast.isNullOrBlank()) {
                playbackUrlCache[cacheKey] = directFast
                playbackUrlCache[titleArtistKey] = directFast
                return Pair(directFast, emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct audio resolve warning: ${e.message}")
        }

        // Attempt 1: Direct backend resolve
        try {
            val data = repository.resolvePlayback(track.id, track.title, track.artist, track.duration)
            val stream = data?.stream
            val url = stream?.url
            if (!url.isNullOrBlank()) {
                if (url.startsWith("youtube:") || stream.descriptorType == "youtube") {
                    val ytId = url.removePrefix("youtube:").trim()
                    val directAudio = YouTubeResolver.resolveDirectAudioForTrack(track.title, track.artist, ytId)
                    if (directAudio != null) {
                        playbackUrlCache[cacheKey] = directAudio
                        playbackUrlCache[titleArtistKey] = directAudio
                        return Pair(directAudio, emptyList())
                    } else {
                        val guaranteedUrl = YouTubeResolver.getGuaranteedFallbackStream(track.id)
                        return Pair(guaranteedUrl, emptyList())
                    }
                } else {
                    playbackUrlCache[cacheKey] = url
                    playbackUrlCache[titleArtistKey] = url
                    val fallbacks = (stream.fallbackUrls ?: emptyList()).filter { it.startsWith("http") }
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
                if (!url.isNullOrBlank()) {
                    if (url.startsWith("youtube:") || stream.descriptorType == "youtube") {
                        val ytId = url.removePrefix("youtube:").trim()
                        val directAudio = YouTubeResolver.resolveDirectAudioForTrack(track.title, track.artist, ytId)
                        if (directAudio != null) {
                            playbackUrlCache[cacheKey] = directAudio
                            playbackUrlCache[titleArtistKey] = directAudio
                            return Pair(directAudio, emptyList())
                        }
                    } else {
                        playbackUrlCache[cacheKey] = url
                        playbackUrlCache[titleArtistKey] = url
                        playbackUrlCache[matched.id] = url
                        return Pair(url, (stream.fallbackUrls ?: emptyList()).filter { it.startsWith("http") })
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Attempt 2 resolve error: ${e.message}")
        }

        // Attempt 3: Fast YouTube direct stream resolve
        try {
            val ytUrl = YouTubeResolver.searchAndResolve("${track.title} ${track.artist}".trim())
            if (!ytUrl.isNullOrBlank() && ytUrl.startsWith("http")) {
                playbackUrlCache[cacheKey] = ytUrl
                playbackUrlCache[titleArtistKey] = ytUrl
                return Pair(ytUrl, emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Attempt 3 resolve error: ${e.message}")
        }

        return Pair(YouTubeResolver.getGuaranteedFallbackStream(track.id), emptyList())
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

    fun playYouTubeTrack(videoId: String, fallbackVideoIds: List<String>, track: TrackDto) {
        val ctx = appContext ?: return
        scope.launch {
            try {
                // First try resolving a direct HTTP audio stream to avoid WebView embed restrictions (150/152)
                val directUrl = YouTubeResolver.resolveDirectAudioForTrack(track.title, track.artist, videoId)
                if (!directUrl.isNullOrBlank()) {
                    isUsingYouTubeEngine = false
                    youTubeAudioEngine?.stop()
                    playAudioUrls(directUrl, emptyList(), track)
                    return@launch
                }

                isUsingYouTubeEngine = true
                ensureYouTubeEngineInitialized(ctx)
                pendingFallbackYouTubeIds.clear()
                pendingFallbackYouTubeIds.addAll(fallbackVideoIds.filter { it.isNotBlank() && it != videoId })

                val player = ensurePlayerInitialized(ctx)
                player.stop()
                player.clearMediaItems()

                // Set metadata item with a valid HTTPS URI and DO NOT call player.prepare()
                // so ExoPlayer never attempts to open an invalid URI while YouTubeAudioEngine is active
                val metadataMediaItem = buildMediaItem(
                    YouTubeResolver.getGuaranteedFallbackStream(track.id),
                    track
                )
                player.setMediaItem(metadataMediaItem)

                _isPlaying.value = false
                _isLoading.value = true
                isFetchingTrackId = track.id

                startPlaybackService()
                loadArtworkDataIntoMetadata(track)

                Log.i(TAG, "Playing track '${track.title}' via YouTubeAudioEngine with video ID: $videoId")
                youTubeAudioEngine?.loadAndPlay(videoId)
            } catch (e: Exception) {
                Log.w(TAG, "Error playing track via YouTubeAudioEngine: ${e.message}")
                _isLoading.value = false
                _isPlaying.value = false
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
            pendingFallbackUrls.addAll(fallbackUrls.filter { it.startsWith("http") || it.startsWith("file") })

            val mediaItem = buildMediaItem(primaryUrl, track)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _isPlaying.value = false
            _isLoading.value = true

            startPlaybackService()
            loadArtworkDataIntoMetadata(track)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting up ExoPlayer: ${e.message}")
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
        onPlaybackStateUpdatedListener?.invoke()
    }

    fun play() {
        val current = _currentTrack.value
        if (current != null && !isUsingYouTubeEngine && (rawExoPlayer == null || rawExoPlayer?.mediaItemCount == 0)) {
            playTrack(current, _queue.value.ifEmpty { null })
            return
        }
        if (isUsingYouTubeEngine) {
            youTubeAudioEngine?.play()
        } else {
            rawExoPlayer?.play()
        }
        startPlaybackService()
        onPlaybackStateUpdatedListener?.invoke()
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

    private fun getDefaultSeedRecommendations(excludeTrackId: String?): List<TrackDto> {
        val allSeeds = listOf(
            TrackDto(
                id = "seed_rap91_hindi",
                title = "RAP 91 Hindi",
                artist = "KR\$NA, Raftaar, DIVINE",
                album = "Spotify Rap 91",
                duration = 204,
                images = com.example.data.remote.models.ImagesDto(
                    small = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300&auto=format&fit=crop&q=80",
                    medium = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=300&auto=format&fit=crop&q=80",
                    large = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80"
                )
            ),
            TrackDto(
                id = "seed_husn_anuv",
                title = "Husn",
                artist = "Anuv Jain",
                album = "Husn - Single",
                duration = 218,
                images = com.example.data.remote.models.ImagesDto(
                    small = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/500x500-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/500x500-000000-80-0-0.jpg",
                    large = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/1000x1000-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "seed_gehra_hua_radio",
                title = "Gehra Hua Radio",
                artist = "Arijit Singh, Pritam, Vishal Mishra",
                album = "Spotify Radio",
                duration = 235,
                images = com.example.data.remote.models.ImagesDto(
                    small = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300&auto=format&fit=crop&q=80",
                    medium = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300&auto=format&fit=crop&q=80",
                    large = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80"
                )
            ),
            TrackDto(
                id = "seed_tauba_aujla",
                title = "Tauba Tauba",
                artist = "Karan Aujla",
                album = "Bad Newz",
                duration = 207,
                images = com.example.data.remote.models.ImagesDto(
                    small = "https://cdn-images.dzcdn.net/images/cover/ff6bb1420d9fcd2671cf6f86c2e49658/500x500-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/ff6bb1420d9fcd2671cf6f86c2e49658/500x500-000000-80-0-0.jpg",
                    large = "https://cdn-images.dzcdn.net/images/cover/ff6bb1420d9fcd2671cf6f86c2e49658/1000x1000-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "seed_arz_kiya_hai",
                title = "Arz Kiya Hai Mix",
                artist = "Anuv Jain, Coke Studio",
                album = "Coke Studio Bharat",
                duration = 240,
                images = com.example.data.remote.models.ImagesDto(
                    small = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/500x500-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/500x500-000000-80-0-0.jpg",
                    large = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/1000x1000-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "seed_sajni_radio",
                title = "Sajni Radio",
                artist = "Arijit Singh, Ram Sampath",
                album = "Laapataa Ladies",
                duration = 175,
                images = com.example.data.remote.models.ImagesDto(
                    small = "https://cdn-images.dzcdn.net/images/cover/6abead2d0890caf99ec89d4edf4f3ca2/500x500-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/6abead2d0890caf99ec89d4edf4f3ca2/500x500-000000-80-0-0.jpg",
                    large = "https://cdn-images.dzcdn.net/images/cover/6abead2d0890caf99ec89d4edf4f3ca2/1000x1000-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "seed_winning_speech",
                title = "Winning Speech",
                artist = "Karan Aujla",
                album = "Four Me",
                duration = 219,
                images = com.example.data.remote.models.ImagesDto(
                    small = "https://cdn-images.dzcdn.net/images/cover/b6ff41520784c1c1b8cbff7925817cd8/500x500-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/b6ff41520784c1c1b8cbff7925817cd8/500x500-000000-80-0-0.jpg",
                    large = "https://cdn-images.dzcdn.net/images/cover/b6ff41520784c1c1b8cbff7925817cd8/1000x1000-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "seed_raanjhan_mix",
                title = "Raanjhan Radio",
                artist = "Sachet-Parampara",
                album = "Do Patti",
                duration = 241,
                images = com.example.data.remote.models.ImagesDto(
                    small = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/500x500-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/500x500-000000-80-0-0.jpg",
                    large = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/1000x1000-000000-80-0-0.jpg"
                )
            )
        )
        val filtered = allSeeds.filterNot { it.id == excludeTrackId }
        if (excludeTrackId == null || excludeTrackId == "track_dopamine_guru_randhawa") {
            return filtered.take(4)
        }
        val shift = kotlin.math.abs(excludeTrackId.hashCode()) % filtered.size
        val rotated = filtered.drop(shift) + filtered.take(shift)
        return rotated.take(4)
    }

    fun getRecommendedTracks(): List<TrackDto> {
        val currentId = _currentTrack.value?.id
        val recs = _recommendedTracks.value.filterNot { it.id == currentId }
        if (recs.size >= 4) return recs.take(4)
        val q = _queue.value.filterNot { it.id == currentId }
        val combined = (recs + q + getDefaultSeedRecommendations(currentId)).distinctBy { it.id }
        return combined.take(4)
    }

    fun getArtworkDataForTrack(trackId: String): ByteArray? {
        return recommendationArtCache[trackId]
    }

    fun getArtworkBitmapForTrack(track: TrackDto, fallbackIndex: Int = 0): Bitmap {
        val bytes = recommendationArtCache[track.id]
        if (bytes != null && bytes.isNotEmpty()) {
            try {
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) return bmp
            } catch (_: Exception) {
            }
        }
        return generateStyledFallbackCover(track, fallbackIndex)
    }

    fun getArtworkContentUriForTrack(trackId: String, callerPackage: String? = null): Uri? {
        val uri = recommendationArtUriCache[trackId] ?: return null
        val ctx = appContext
        if (ctx != null && !callerPackage.isNullOrBlank()) {
            try {
                ctx.grantUriPermission(
                    callerPackage,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
        }
        return uri
    }

    fun findTrackById(trackId: String): TrackDto? {
        if (_currentTrack.value?.id == trackId) return _currentTrack.value
        _recommendedTracks.value.find { it.id == trackId }?.let { return it }
        _queue.value.find { it.id == trackId }?.let { return it }
        getDefaultSeedRecommendations(null).find { it.id == trackId }?.let { return it }
        return null
    }

    private fun generateStyledFallbackCover(track: TrackDto, index: Int): Bitmap {
        val size = 240
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val palettes = listOf(
            intArrayOf(0xFF1E3264.toInt(), 0xFF0D152B.toInt()),
            intArrayOf(0xFF8D67AB.toInt(), 0xFF39264A.toInt()),
            intArrayOf(0xFFE8115B.toInt(), 0xFF5E0724.toInt()),
            intArrayOf(0xFF148A08.toInt(), 0xFF083803.toInt())
        )
        val pal = palettes[kotlin.math.abs(track.id.hashCode() + index) % palettes.size]
        val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                pal[0], pal[1],
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Spotify green accent dot
        val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1ED760.toInt()
        }
        canvas.drawCircle(26f, 26f, 10f, dotPaint)

        // Badge label ("RADIO" or "MIX")
        val badgePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            textSize = 18f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        canvas.drawText(if (index % 2 == 0) "MIX" else "RADIO", 160f, 32f, badgePaint)

        // Track Title at bottom
        val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 26f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val shortTitle = if (track.title.length > 14) track.title.take(12) + "…" else track.title
        canvas.drawText(shortTitle, 18f, 192f, titlePaint)

        val artistPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFD0D0D0.toInt()
            textSize = 19f
        }
        val shortArtist = if (track.artist.length > 16) track.artist.take(14) + "…" else track.artist
        canvas.drawText(shortArtist, 18f, 218f, artistPaint)
        return bmp
    }

    private fun saveBitmapToSharedCacheAndGetUri(ctx: Context, trackId: String, bitmap: Bitmap): Pair<ByteArray, Uri?> {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)
        val bytes = stream.toByteArray()
        var contentUri: Uri? = null
        try {
            val sharedDir = File(ctx.cacheDir, "shared_images").apply { mkdirs() }
            val safeId = trackId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(sharedDir, "rec_${safeId}.jpg")
            file.writeBytes(bytes)
            contentUri = androidx.core.content.FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            val systemPackages = listOf(
                "com.android.systemui",
                "com.oplus.systemui",
                "com.coloros.systemui",
                "com.android.launcher",
                "com.oppo.launcher",
                "com.heytap.themestore"
            )
            for (pkg in systemPackages) {
                try {
                    ctx.grantUriPermission(pkg, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed saving shared image URI for $trackId: ${e.message}")
        }
        return Pair(bytes, contentUri)
    }

    suspend fun ensureRecommendationArtworksReady(): List<TrackDto> {
        val tracks = getRecommendedTracks()
        val ctx = appContext ?: return tracks
        val imageLoader = ImageLoader(ctx)
        tracks.forEachIndexed { idx, t ->
            if (!recommendationArtCache.containsKey(t.id) || !recommendationArtUriCache.containsKey(t.id)) {
                var finalBitmap: Bitmap? = null
                val url = t.images?.medium ?: t.images?.small ?: t.images?.large ?: ""
                if (url.isNotBlank()) {
                    try {
                        val req = ImageRequest.Builder(ctx)
                            .data(url)
                            .size(240, 240)
                            .allowHardware(false)
                            .build()
                        val res = imageLoader.execute(req)
                        if (res is SuccessResult && res.drawable is BitmapDrawable) {
                            val orig = (res.drawable as BitmapDrawable).bitmap
                            finalBitmap = Bitmap.createScaledBitmap(orig, 240, 240, true)
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Fallback cover for ${t.title}: ${e.message}")
                    }
                }
                val bmp = finalBitmap ?: generateStyledFallbackCover(t, idx)
                val (bytes, uri) = saveBitmapToSharedCacheAndGetUri(ctx, t.id, bmp)
                recommendationArtCache[t.id] = bytes
                if (uri != null) {
                    recommendationArtUriCache[t.id] = uri
                }
            }
        }
        return tracks
    }

    private fun cacheRecommendationArtworks(tracks: List<TrackDto>) {
        val ctx = appContext ?: return
        scope.launch(Dispatchers.IO) {
            val imageLoader = ImageLoader(ctx)
            tracks.take(4).forEachIndexed { idx, t ->
                var finalBitmap: Bitmap? = null
                val url = t.images?.medium ?: t.images?.small ?: t.images?.large ?: ""
                if (url.isNotBlank()) {
                    try {
                        val req = ImageRequest.Builder(ctx)
                            .data(url)
                            .size(240, 240)
                            .allowHardware(false)
                            .build()
                        val res = imageLoader.execute(req)
                        if (res is SuccessResult && res.drawable is BitmapDrawable) {
                            val orig = (res.drawable as BitmapDrawable).bitmap
                            finalBitmap = Bitmap.createScaledBitmap(orig, 240, 240, true)
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed caching thumb for ${t.title}: ${e.message}")
                    }
                }
                val bmp = finalBitmap ?: generateStyledFallbackCover(t, idx)
                val (bytes, uri) = saveBitmapToSharedCacheAndGetUri(ctx, t.id, bmp)
                recommendationArtCache[t.id] = bytes
                if (uri != null) {
                    recommendationArtUriCache[t.id] = uri
                }
            }
            scope.launch(Dispatchers.Main) {
                onRecommendationsUpdatedListener?.invoke()
            }
        }
    }
}
