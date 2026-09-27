package com.example
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState

import com.example.data.AppDownloadManager
import com.example.data.DownloadState
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import com.example.data.MusicRepository
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color



import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import android.widget.Toast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.AddCircleOutline
import com.example.data.PlaylistRepository
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.testTag
import com.example.ui.FavoriteHeartButton
import com.example.ui.FavoriteRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onNavigateToArtist: ((String) -> Unit)? = null
) {
    val track by viewModel.currentTrack.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val context = LocalContext.current
    val downloadStatusMap by AppDownloadManager.downloadStatus.collectAsState()
    val downloadState = track?.let { downloadStatusMap[it.id] }
    
    var artistImage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(track?.artist) {
        val artistId = track?.artistId
        if (artistId != null && artistId.isNotEmpty()) {
            val cached = MusicRepository.artistCache[artistId]
            if (cached?.image != null && cached.image.isNotEmpty()) {
                artistImage = cached.image
            } else {
                try {
                    val repo = MusicRepository()
                    val data = repo.getArtist(artistId)
                    if (data?.image != null) {
                        artistImage = data.image
                    }
                } catch (e: Exception) {}
            }
        }
    }

    val repeatMode by viewModel.repeatMode.collectAsState()
    val savedTracks by viewModel.savedTracks.collectAsState()
    val trackMenu = LocalTrackMenuProvider.current
    val shareProvider = LocalShareProvider.current
    val currentPositionSec by viewModel.currentPositionSec.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationSec by viewModel.durationSec.collectAsState()
    val isSaved = track != null && savedTracks.contains(track!!.id)

    val queue by viewModel.queue.collectAsState()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsState()
    var recommendations by remember { mutableStateOf<List<com.example.data.remote.models.TrackDto>>(emptyList()) }
    val vmRecommendedTracks by viewModel.recommendedTracks.collectAsState()

    val upNextTracks = remember(queue, currentQueueIndex, track, recommendations, vmRecommendedTracks) {
        val currentId = track?.id
        val activeIdx = if (currentQueueIndex in queue.indices && queue[currentQueueIndex].id == currentId) {
            currentQueueIndex
        } else {
            queue.indexOfFirst { it.id == currentId }
        }

        val followingQueue = if (activeIdx >= 0 && activeIdx + 1 < queue.size) {
            queue.subList(activeIdx + 1, queue.size)
        } else {
            emptyList()
        }

        val remainingQueue = queue.filterNot { it.id == currentId || followingQueue.contains(it) }
        val recs = (if (recommendations.isNotEmpty()) recommendations else vmRecommendedTracks).filterNot { it.id == currentId }

        val combined = (followingQueue + remainingQueue + recs).distinctBy { it.id }.filterNot { it.id == currentId }
        combined.take(4)
    }

    LaunchedEffect(track?.id) {
        val currentId = track?.id
        if (currentId != null && currentId.isNotEmpty()) {
            try {
                val recs = com.example.data.MusicRepository().getRecommendations(currentId, track?.title, track?.artist)
                recommendations = recs.filterNot { it.id == currentId }
            } catch (e: Exception) {
                recommendations = emptyList()
            }
        }
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showDevicesSheet by remember { mutableStateOf(false) }
    var showFullLyrics by remember { mutableStateOf(false) }
    var showSleepTimerModal by remember { mutableStateOf(false) }
    var showSpeedModal by remember { mutableStateOf(false) }
    var showPlaylistModal by remember { mutableStateOf(false) }
    var showCreatePlaylistModal by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var isArtistFollowed by remember { mutableStateOf(false) }
    var isProducerFollowed by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val safeDuration = durationSec.coerceAtLeast(1).toFloat()

    val themeColor = rememberTrackThemeColor(track)
    val currentCanvasUrl by viewModel.currentCanvasUrl.collectAsState()
    var userCanvasEnabled by remember(track?.id) { mutableStateOf(true) }
    val isCanvasActive = userCanvasEnabled && !currentCanvasUrl.isNullOrBlank()

    val lines: List<com.example.data.remote.models.LyricsLineDto> = remember(currentLyrics) {
        currentLyrics?.lines ?: emptyList()
    }

    val activeIndex by remember(lines, currentPositionMs) {
        derivedStateOf {
            if (lines.isEmpty()) 0
            else {
                val idx = lines.indexOfLast { it.startTimeMs <= currentPositionMs }
                if (idx == -1) 0 else idx
            }
        }
    }

    // Spotify dynamic background gradient: Deep slate/cyan to solid dark
    val topGradientColor = Color(0xFF142B30)
    val bottomBgColor = Color(0xFF121212)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var offsetY = 0f
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY > 150f) {
                            onClose()
                        }
                        offsetY = 0f
                    }
                ) { change, dragAmount ->
                    if (scrollState.value == 0 && dragAmount.y > 0) {
                        offsetY += dragAmount.y
                    }
                }
            }
            .background(Color.Black)
    ) {
        val exoPlayer = androidx.compose.runtime.remember {
            val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)

            val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
                setParameters(buildUponParameters().setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_AUDIO, true))
            }

            ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .setTrackSelector(trackSelector)
                .build().apply {
                    setRepeatMode(Player.REPEAT_MODE_ONE)
                    setVolume(0f)
                    playWhenReady = true
            }
        }

        DisposableEffect(exoPlayer) {
            onDispose {
                exoPlayer.release()
            }
        }

        androidx.compose.runtime.LaunchedEffect(currentCanvasUrl, isCanvasActive) {
            if (isCanvasActive && currentCanvasUrl != null) {
                exoPlayer.setMediaItem(MediaItem.fromUri(currentCanvasUrl!!))
                exoPlayer.prepare()
                exoPlayer.play()
            } else {
                exoPlayer.pause()
                exoPlayer.clearMediaItems()
            }
        }

        if (isCanvasActive) {
            // Full screen Spotify 9:16 Canvas with true crisp vibrant colors (no dark overlay on top of video)
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Ultra-subtle bottom-only shadow for player controls readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.65f to Color.Transparent,
                                1.0f to Color(0xFF0A0A0A).copy(alpha = 0.55f)
                            )
                        )
                    )
            )
        } else {
            // High-performance dynamic gradient background (Zero GPU blur overhead for 120 FPS)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                themeColor.copy(alpha = 0.75f),
                                themeColor.copy(alpha = 0.35f),
                                Color(0xFF141416),
                                Color(0xFF121212)
                            )
                        )
                    )
            )

            // Dynamic depth shading overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF101010).copy(alpha = 0.25f), 
                                Color(0xFF121212).copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp)
        ) {
            // Main Top Bar (when at top)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM ALBUM",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB3B3B3),
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track?.album ?: track?.title ?: "Album",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Canvas / Artwork Toggle Button left of 3 dots (shown whenever canvas is available)
                    if (!currentCanvasUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                userCanvasEnabled = !userCanvasEnabled
                                val msg = if (userCanvasEnabled) "Canvas Video On" else "Album Artwork On"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = if (userCanvasEnabled) Icons.Filled.Image else Icons.Filled.Videocam,
                                contentDescription = if (userCanvasEnabled) "Show Album Art" else "Show Canvas Video",
                                tint = if (userCanvasEnabled) Color(0xFF1DB954) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(onClick = { showContextMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Album Artwork (Spotify square card with 8dp rounded corners)
            if (!isCanvasActive) {
                com.example.ui.SongArtworkImage(
                    track = track,
                    title = track?.title,
                    artist = track?.artist,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            } else {
                Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lyrics teaser single line below album art (if real lyrics available)
            val activeTeaserText = lines.getOrNull(activeIndex)?.text
            if (activeTeaserText != null) {
                Text(
                    text = activeTeaserText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Title, Artist and Heart Like Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val artistId = track?.artistId ?: track?.artist ?: ""
                            if (artistId.isNotEmpty() && onNavigateToArtist != null) {
                                track?.let { t -> com.example.data.MusicRepository.artistCache[artistId] = com.example.data.remote.models.ArtistDto(id = artistId, name = t.artist, image = t.images?.large ?: t.images?.medium ?: t.images?.small ?: "") }

                                onNavigateToArtist(artistId)
                            }
                        }
                ) {
                    Text(
                        text = track?.title ?: "Unknown Track",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFB3B3B3),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showPlaylistModal = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircleOutline,
                            contentDescription = "Add to playlist",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(
                        onClick = { track?.let { AppDownloadManager.downloadTrack(context, it) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (downloadState is DownloadState.Downloading) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { downloadState.progress },
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF1DB954),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "${(downloadState.progress * 100).toInt()}",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (downloadState is DownloadState.Completed) {
                            Icon(
                                imageVector = Icons.Filled.DownloadDone,
                                contentDescription = "Downloaded",
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    FavoriteHeartButton(
                        isLiked = isSaved,
                        onToggleLike = { track?.let { viewModel.toggleSave(it) } },
                        modifier = Modifier.size(36.dp),
                        iconSize = 28.dp,
                        unlikedColor = Color.White,
                        likedColor = FavoriteRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CRITICAL: Pure Plain Smooth Line & Dot Progress Bar
            // User requested: "प्रोग्रेस बार देखो। कोई भी मोटा प्रोग्रेस बार नहीं है, एकदम प्लेन स्मूथ लाइन है एंड एक डॉट है।"
            SmoothSpotifyProgressBar(
                positionSec = currentPositionSec.toFloat(),
                durationSec = safeDuration,
                onSeek = { seekSec -> viewModel.seekToMs((seekSec * 1000f).toLong()) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls Row (Shuffle, Prev, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) Color(0xFF1DB954) else Color(0xFFB3B3B3),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = { viewModel.skipPrevious() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Spotify White Big Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("full_player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                IconButton(onClick = { viewModel.skipNext() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = if (repeatMode == 2) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode > 0) Color(0xFF1DB954) else Color(0xFFB3B3B3),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Secondary Controls (Lyrics, Queue, Timer, Speed, Devices, Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Lyrics
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullLyrics = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Lyrics",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "LYRICS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 0.6.sp
                    )
                }

                // 2. Queue
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "QUEUE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 0.6.sp
                    )
                }

                // 3. Sleep Timer
                val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSleepTimerModal = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Sleep timer",
                        tint = if (sleepTimerMinutes != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (sleepTimerMinutes != null) "${sleepTimerMinutes}M" else "TIMER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sleepTimerMinutes != null) Color(0xFF10B981) else Color.White.copy(alpha = 0.75f),
                        letterSpacing = 0.6.sp
                    )
                }

                // 4. Playback Speed
                val playbackSpeed by viewModel.playbackSpeed.collectAsState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSpeedModal = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Playback speed",
                        tint = if (playbackSpeed != 1.0f) Color(0xFF10B981) else Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playbackSpeed}X",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (playbackSpeed != 1.0f) Color(0xFF10B981) else Color.White.copy(alpha = 0.75f),
                        letterSpacing = 0.6.sp
                    )
                }

                // 5. Connect Devices
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDevicesSheet = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_devices),
                        contentDescription = "Devices",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "DEVICES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 0.6.sp
                    )
                }

                // 6. Share
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            track?.let { ShareHelper.shareTrack(context, it) }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SHARE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 0.6.sp
                    )
                }
            }



            Spacer(modifier = Modifier.height(24.dp))

            // Lyrics Preview Card with Spotify-inspired Dynamic Artwork Background Gradient & Real-Time 5-Line Window
            if (lines.isNotEmpty()) {
                val lyricsTheme = rememberTrackLyricsCardTheme(track)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(lyricsTheme.backgroundBrush)
                        .clickable { showFullLyrics = true }
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lyrics preview",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.2).sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val previewStartIndex = (activeIndex - 1).coerceIn(0, (lines.size - 4).coerceAtLeast(0))
                        val previewLines = lines.drop(previewStartIndex).take(4)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            previewLines.forEach { line ->
                                val isCur = line == lines.getOrNull(activeIndex)
                                val textColor by androidx.compose.animation.animateColorAsState(
                                    targetValue = if (isCur) Color.White else Color.Black.copy(alpha = 0.45f),
                                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                                    label = "previewTextColor"
                                )
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = textColor,
                                    fontWeight = if (isCur) FontWeight.Black else FontWeight.Bold,
                                    fontSize = if (isCur) 22.sp else 19.sp,
                                    lineHeight = if (isCur) 28.sp else 24.sp,
                                    letterSpacing = (-0.4).sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Spotify White pill "Show lyrics" button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .clickable { showFullLyrics = true }
                                .padding(horizontal = 24.dp, vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Show lyrics",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // SCREENSHOT 2: About the artist Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B1B22))
                    .clickable {
                        val artistId = track?.artistId ?: track?.artist ?: ""
                        if (artistId.isNotEmpty() && onNavigateToArtist != null) {
                            track?.let { t ->
                                com.example.data.MusicRepository.artistCache[artistId] = com.example.data.remote.models.ArtistDto(
                                    id = artistId,
                                    name = t.artist,
                                    image = t.images?.large ?: t.images?.medium ?: t.images?.small ?: ""
                                )
                            }
                            onNavigateToArtist(artistId)
                        }
                    }
            ) {
                Column {
                    // Artist banner image with gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        AsyncImage(
                            model = artistImage ?: track?.images?.large ?: track?.images?.medium,
                            contentDescription = track?.artist,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x801B1B22),
                                            Color(0xFF1B1B22)
                                        )
                                    )
                                )
                        )
                    }

                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 4.dp)) {
                        Text(
                            text = "About the artist",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track?.artist ?: "Artist",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "24.9M monthly listeners",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB3B3B3),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${track?.artist ?: "This artist"} is an acclaimed music creator on Spotify. Explore their songs, albums, and discography.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SCREENSHOT 3: Explore [Artist] Section
            val artistName = track?.artist ?: "Artist"
            Text(
                text = "Explore $artistName",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Songs by Artist
                ExploreArtistCard(
                    title = "Songs by $artistName",
                    badge = null,
                    imageUrl = track?.images?.small,
                    onClick = {
                        val artistId = track?.artistId ?: track?.artist ?: ""
                        if (artistId.isNotEmpty() && onNavigateToArtist != null) {
                            track?.let { t ->
                                com.example.data.MusicRepository.artistCache[artistId] = com.example.data.remote.models.ArtistDto(
                                    id = artistId,
                                    name = t.artist,
                                    image = t.images?.large ?: t.images?.medium ?: t.images?.small ?: ""
                                )
                            }
                            onNavigateToArtist(artistId)
                        }
                    }
                )

                // Card 2: Raanjha - Spotify Singles
                ExploreArtistCard(
                    title = "Raanjha - Spotify Singles",
                    badge = "Singles",
                    imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80",
                    onClick = { }
                )

                // Card 3: Maa - Spotify Singles
                ExploreArtistCard(
                    title = "Maa - Spotify Singles",
                    badge = "Singles",
                    imageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SCREENSHOT 3: Credits Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF181818))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Credits",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Show all",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB3B3B3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Credit 1: Main Artist & Composer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Main Artist • Composer",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA7A7A7),
                                fontSize = 12.sp
                            )
                        }

                        // Follow Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isArtistFollowed) Color.White else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isArtistFollowed) Color.White else Color(0xFF666666),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { isArtistFollowed = !isArtistFollowed }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArtistFollowed) "Following" else "Follow",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isArtistFollowed) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Credit 2: Studio Producer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Studio Producer",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Producer",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA7A7A7),
                                fontSize = 12.sp
                            )
                        }

                        // Follow Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isProducerFollowed) Color.White else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isProducerFollowed) Color.White else Color(0xFF666666),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { isProducerFollowed = !isProducerFollowed }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isProducerFollowed) "Following" else "Follow",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isProducerFollowed) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // SCREENSHOT 2 & 3: Sticky Top Bar when scrolled down
        val isScrolled = scrollState.value > 220
        AnimatedVisibility(
            visible = isScrolled,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = topGradientColor,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = track?.title ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track?.artist ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FavoriteHeartButton(
                            isLiked = isSaved,
                            onToggleLike = { track?.let { viewModel.toggleSave(it) } },
                            iconSize = 24.dp,
                            unlikedColor = Color.White,
                            likedColor = FavoriteRed
                        )

                        // Small Play/Pause button in sticky header
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showQueueSheet) {
            QueueSheet(playerViewModel = viewModel, onDismiss = { showQueueSheet = false })
        }

        if (showDevicesSheet) {
            DevicesSheet(playerViewModel = viewModel, onDismiss = { showDevicesSheet = false })
        }

        if (showSleepTimerModal) {
            val currentTimer by viewModel.sleepTimerMinutes.collectAsState()
            SleepTimerModal(
                currentTimer = currentTimer,
                onSetTimer = { mins ->
                    viewModel.setSleepTimer(mins)
                    val msg = if (mins != null) "Sleep timer set for $mins minutes" else "Sleep timer turned off"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showSleepTimerModal = false }
            )
        }

        if (showSpeedModal) {
            val currentSpeed by viewModel.playbackSpeed.collectAsState()
            PlaybackSpeedModal(
                currentSpeed = currentSpeed,
                onSetSpeed = { sp ->
                    viewModel.setPlaybackSpeed(sp)
                    Toast.makeText(context, "Playback speed: ${sp}x", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showSpeedModal = false }
            )
        }

        if (showPlaylistModal && track != null) {
            val playlists by PlaylistRepository.playlists.collectAsState()
            AddToPlaylistSheet(
                track = track!!,
                playlists = playlists,
                onDismiss = { showPlaylistModal = false },
                onPlaylistSelected = { pl ->
                    PlaylistRepository.addTrackToPlaylist(pl.id, track!!)
                    Toast.makeText(context, "Added to \"${pl.title}\"", Toast.LENGTH_SHORT).show()
                    showPlaylistModal = false
                },
                onCreateNewPlaylist = {
                    showPlaylistModal = false
                    showCreatePlaylistModal = true
                }
            )
        }

        if (showCreatePlaylistModal && track != null) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistModal = false },
                onPlaylistCreated = { newPlId ->
                    PlaylistRepository.addTrackToPlaylist(newPlId, track!!)
                    Toast.makeText(context, "Created and added track", Toast.LENGTH_SHORT).show()
                    showCreatePlaylistModal = false
                }
            )
        }

        if (showContextMenu && track != null) {
            TrackOptionsSheet(
                track = track!!,
                isSaved = isSaved,
                onDismiss = { showContextMenu = false },
                onToggleSave = { viewModel.toggleSave(track!!) },
                onAddToPlaylist = {
                    showContextMenu = false
                    showPlaylistModal = true
                },
                onAddToQueue = {
                    viewModel.addToQueue(track!!)
                    Toast.makeText(context, "Added to Queue", Toast.LENGTH_SHORT).show()
                    showContextMenu = false
                },
                onViewArtist = { artistId ->
                    showContextMenu = false
                    onClose()
                    onNavigateToArtist?.invoke(artistId)
                },
                onViewAlbum = { albumId ->
                    showContextMenu = false
                    onClose()
                }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showFullLyrics,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(300)
            ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
        ) {
            FullScreenLyricsView(
                viewModel = viewModel,
                onClose = { showFullLyrics = false }
            )
        }
    }
}

/**
 * Plain Smooth Line & Dot Progress Bar
 * Matches Spotify's exact look from the user's screenshot:
 * - Inactive line: 2.dp height, muted white/grey
 * - Active line: 2.dp height, solid white
 * - Dot: 10.dp solid white circle centered on the 2.dp line
 * - Smooth tap & horizontal drag scrubbing with real-time timestamp display
 */
@Composable
fun SmoothSpotifyProgressBar(
    positionSec: Float,
    durationSec: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationSec.coerceAtLeast(1f)
    val currentProgress = if (isDragging) dragProgress else (positionSec / safeDuration).coerceIn(0f, 1f)

    val curSec = if (isDragging) (dragProgress * safeDuration).toInt() else positionSec.toInt()
    val totalSec = safeDuration.toInt()

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp) // Accessible touch target area
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(newProgress * safeDuration)
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek(dragProgress * safeDuration)
                            isDragging = false
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragProgress = newProgress
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val thumbRadiusDp = 5.dp // 10.dp diameter dot
            val thumbRadiusPx = with(LocalDensity.current) { thumbRadiusDp.toPx() }

            // Inactive track line: Plain 2dp smooth line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(1.dp))
            )

            // Active track line: Plain 2dp smooth line in white
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentProgress)
                    .height(2.dp)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )

            // Dot Thumb: 10dp solid white circle centered on the line
            val thumbOffsetDp = with(LocalDensity.current) {
                ((currentProgress * widthPx) - thumbRadiusPx).coerceIn(0f, (widthPx - thumbRadiusPx * 2).coerceAtLeast(0f)).toDp()
            }

            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetDp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        // Timestamps below scrubber
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${curSec / 60}:${(curSec % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            )
            Text(
                text = "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ExploreArtistCard(
    title: String,
    badge: String?,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Optional badge at top-right
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        // Title at bottom
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerModal(
    currentTimer: Int?,
    onSetTimer: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E22),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Sleep timer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            val options = listOf(
                5 to "5 minutes",
                15 to "15 minutes",
                30 to "30 minutes",
                45 to "45 minutes",
                60 to "60 minutes"
            )

            if (currentTimer != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSetTimer(null)
                            onDismiss()
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Turn off timer",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }

            options.forEach { (mins, label) ->
                val isSelected = currentTimer == mins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSetTimer(mins)
                            onDismiss()
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF10B981) else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedModal(
    currentSpeed: Float,
    onSetSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E22),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            val speeds = listOf(
                0.75f to "0.75x",
                1.0f to "1.0x (Normal)",
                1.25f to "1.25x",
                1.5f to "1.5x",
                2.0f to "2.0x"
            )

            speeds.forEach { (spVal, label) ->
                val isSelected = kotlin.math.abs(currentSpeed - spVal) < 0.05f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            onSetSpeed(spVal)
                            onDismiss()
                        }
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF10B981) else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendedTrackTile(
    track: com.example.data.remote.models.TrackDto,
    onClick: () -> Unit
) {
    val imageUrl = track.images?.large
        ?: track.images?.medium
        ?: track.images?.small
        ?: ""

    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("recommended_tile_${track.id}")
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF282828))
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB3B3B3),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
