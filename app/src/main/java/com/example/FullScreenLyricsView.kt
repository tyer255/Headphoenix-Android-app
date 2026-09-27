package com.example

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.ColorUtils
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.remote.models.LyricsLineDto
import com.example.data.remote.models.TrackDto
import com.example.ui.shimmerEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Resolves a dynamic theme color based on the track's album art using the Palette API,
 * falling back to API color or a rich saturated hash color.
 */
@Composable
fun rememberTrackThemeColor(track: TrackDto?): Color {
    val context = LocalContext.current
    
    val fallback = remember(track?.id) {
        val hash = (track?.id ?: track?.title ?: "").hashCode()
        val paletteList = listOf(
            Color(0xFF6B1D2F), // Dark Wine / Crimson
            Color(0xFF8A2522), // Deep Rust Red
            Color(0xFF943D1A), // Burnt Orange
            Color(0xFF284855), // Deep Ocean Blue
            Color(0xFF33503B), // Forest Olive
            Color(0xFF4C2A58), // Deep Violet
            Color(0xFF5A3825), // Warm Mocha
            Color(0xFF1E3A5F)  // Midnight Slate
        )
        paletteList[abs(hash) % paletteList.size]
    }

    val themeColor = remember { mutableStateOf(fallback) }

    LaunchedEffect(track?.id, track?.images?.large) {
        val imageUrl = track?.images?.large ?: track?.images?.medium ?: track?.images?.small
        if (imageUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .size(300)
                .build()
                
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).generate()
                }
                
                val swatch = palette.darkVibrantSwatch
                    ?: palette.vibrantSwatch
                    ?: palette.dominantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.darkMutedSwatch
                    
                if (swatch != null) {
                    val hsl = swatch.hsl
                    hsl[2] = hsl[2].coerceAtMost(0.35f)
                    themeColor.value = Color(ColorUtils.HSLToColor(hsl))
                    return@LaunchedEffect
                }
            }
        }
        
        val hex = track?.color
        if (!hex.isNullOrBlank()) {
            try {
                val parsed = android.graphics.Color.parseColor(hex)
                themeColor.value = Color(parsed)
                return@LaunchedEffect
            } catch (_: Exception) {}
        }
        
        themeColor.value = fallback
    }
    
    return themeColor.value
}

@Composable
fun FullScreenLyricsView(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track by viewModel.currentTrack.collectAsState()
    val lyrics by viewModel.currentLyrics.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosMs by viewModel.currentPositionMs.collectAsState()
    val currentPosSec by viewModel.currentPositionSec.collectAsState()
    val durationSec by viewModel.durationSec.collectAsState()
    val context = LocalContext.current

    val themeColor = rememberTrackThemeColor(track)

    val lines: List<LyricsLineDto> = remember(lyrics) {
        lyrics?.lines ?: emptyList()
    }

    // Skeleton / Shimmer state while full lyrics data is preparing
    var isPreparing by remember(lyrics) { mutableStateOf(lyrics == null) }
    LaunchedEffect(lyrics) {
        if (lyrics != null) {
            delay(150)
            isPreparing = false
        } else {
            isPreparing = true
        }
    }

    // Selected lines for sharing
    var selectedLineIndices by remember { mutableStateOf(setOf<Int>()) }

    // Determine currently active lyric line index based on playback position
    val activeIndex by remember(lines, currentPosMs) {
        derivedStateOf {
            if (lines.isEmpty()) 0
            else {
                val idx = lines.indexOfLast { it.startTimeMs <= currentPosMs }
                if (idx == -1) 0 else idx
            }
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to active line when not manually selecting lines
    LaunchedEffect(activeIndex) {
        if (lines.isNotEmpty() && activeIndex in lines.indices && selectedLineIndices.isEmpty()) {
            listState.animateScrollToItem(
                index = (activeIndex - 1).coerceAtLeast(0),
                scrollOffset = -140
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar: Down Arrow & Song Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close lyrics",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track?.title ?: "Unknown Track",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track?.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (selectedLineIndices.isNotEmpty()) {
                    TextButton(
                        onClick = { selectedLineIndices = emptySet() }
                    ) {
                        Text("Clear", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }
            }

            // Main Lyrics Area with Skeleton Loading & Smooth Fade-in Transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = isPreparing || lyrics == null,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "LyricsContentTransition"
                ) { isLoading ->
                    if (isLoading) {
                        // Spotify Stanza Skeleton loading screen matching authentic layout
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            // Stanza 1
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.80f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                            }

                            // Stanza 2
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.80f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                            }

                            // Stanza 3
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.65f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.80f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(28.dp)
                                        .shimmerEffect(shape = RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    } else if (lines.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Audiotrack,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Lyrics aren't available for this song",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "We don't have synchronized lyrics for \"${track?.title ?: "this song"}\" yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 16.dp,
                                bottom = 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(lines) { index, line ->
                                val isActive = index == activeIndex
                                val isSelected = selectedLineIndices.contains(index)

                                val textColor by animateColorAsState(
                                    targetValue = when {
                                        isSelected -> Color.White
                                        isActive -> Color.White
                                        else -> Color.Black.copy(alpha = 0.45f)
                                    },
                                    animationSpec = tween(durationMillis = 250),
                                    label = "textColor"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.22f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            if (selectedLineIndices.isNotEmpty()) {
                                                selectedLineIndices = if (isSelected) {
                                                    selectedLineIndices - index
                                                } else {
                                                    selectedLineIndices + index
                                                }
                                            } else {
                                                viewModel.seekToMs(line.startTimeMs)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = line.text,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = if (isActive || isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = textColor,
                                            fontSize = 22.sp,
                                            lineHeight = 32.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .padding(start = 8.dp)
                                                    .size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Top & Bottom subtle gradient fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(themeColor, Color.Transparent)
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, themeColor)
                            )
                        )
                )

                // Floating "Share [N] lines" pill button when user selects lyrics
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = selectedLineIndices.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Button(
                            onClick = {
                                track?.let { t ->
                                    val selectedTextList = selectedLineIndices.sorted().mapNotNull { lines.getOrNull(it)?.text }
                                    ShareHelper.shareLyrics(context, t, selectedTextList)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share ${selectedLineIndices.size} selected line${if (selectedLineIndices.size > 1) "s" else ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Bottom Controls Section (Progress bar + Play/Pause + Native Share)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Progress Scrubber
                LyricsProgressBar(
                    positionSec = currentPosSec.toFloat(),
                    durationSec = durationSec.toFloat(),
                    onSeek = { sec -> viewModel.seekToMs((sec * 1000f).toLong()) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons Row: Native Sharesheet button on left, Circular Play/Pause in center
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            track?.let { t ->
                                if (selectedLineIndices.isNotEmpty()) {
                                    val selectedTextList = selectedLineIndices.sorted().mapNotNull { lines.getOrNull(it)?.text }
                                    ShareHelper.shareLyrics(context, t, selectedTextList)
                                } else {
                                    val currentLine = lines.getOrNull(activeIndex)?.text
                                    val linesToShare = if (currentLine != null) listOf(currentLine) else emptyList()
                                    ShareHelper.shareLyrics(context, t, linesToShare)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share lyrics via native Sharesheet",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // White Circular Play/Pause button centered
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact scrubber with timestamps for the lyrics view
 */
@Composable
private fun LyricsProgressBar(
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
                .height(20.dp)
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
            val thumbRadiusDp = 4.dp
            val thumbRadiusPx = with(LocalDensity.current) { thumbRadiusDp.toPx() }

            // Inactive track line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
            )

            // Active track line
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentProgress)
                    .height(2.dp)
                    .background(Color.White, RoundedCornerShape(1.dp))
            )

            // Dot Thumb
            val thumbOffsetDp = with(LocalDensity.current) {
                ((currentProgress * widthPx) - thumbRadiusPx).coerceIn(0f, (widthPx - thumbRadiusPx * 2).coerceAtLeast(0f)).toDp()
            }

            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetDp)
                    .size(8.dp)
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
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
            Text(
                text = "${totalSec / 60}:${(totalSec % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}
