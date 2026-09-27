package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.FavoriteHeartButton
import com.example.ui.FavoriteRed

/**
 * Native Android implementation matching temp_repo/src/components/Player/MiniPlayer.tsx 1:1.
 */
@Composable
fun MiniPlayer(
    viewModel: PlayerViewModel,
    onOpenFullScreen: () -> Unit,
    onOpenLyrics: (() -> Unit)? = null
) {
    val track by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val savedTracks by viewModel.savedTracks.collectAsState()
    val currentPositionSec by viewModel.currentPositionSec.collectAsState()
    val durationSec by viewModel.durationSec.collectAsState()
    val isSaved = track != null && savedTracks.contains(track!!.id)
    val rawProgress = (currentPositionSec.toFloat() / durationSec.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 150, easing = LinearEasing),
        label = "mini_player_progress"
    )

    if (track == null) return

    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF202024).copy(alpha = 0.94f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                var offsetX = 0f
                var offsetY = 0f
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -40f) {
                            onOpenFullScreen()
                        } else if (offsetX > 60f) {
                            viewModel.skipPrevious()
                        } else if (offsetX < -60f) {
                            viewModel.skipNext()
                        }
                        offsetX = 0f
                        offsetY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .clickable { onOpenFullScreen() }
    ) {
        // Continuous top thin progress bar indicator with glowing gradient
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(2.5.dp)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF10B981), Color(0xFF34D399))
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Thumbnail with rounded corners + animated pulse ping when playing
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF282828)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track?.images?.small ?: track?.images?.medium ?: track?.images?.large,
                    contentDescription = track?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Animated green pulse dot
                        val infiniteTransition = rememberInfiniteTransition(label = "ping")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "ping_scale"
                        )
                        Box(
                            modifier = Modifier
                                .size((6 * scale).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Metadata: Title and Artist with animated equalizer bars
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = track?.title ?: "Select a track",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (isPlaying) {
                        // Mini 3-bar Equalizer animation
                        EqualizerAnimation()
                    }
                }

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = track?.artist ?: "Spotify",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB3B3B3),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right: Quick Action Controls (Devices, Lyrics, Like/Save, Play/Pause)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Spotify Connect device icon
                IconButton(
                    onClick = { onOpenFullScreen() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_devices),
                        contentDescription = "Devices",
                        tint = Color(0xFFB3B3B3),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Lyrics mic button
                IconButton(
                    onClick = {
                        if (onOpenLyrics != null) {
                            onOpenLyrics()
                        } else {
                            onOpenFullScreen()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Lyrics",
                        tint = Color(0xFFB3B3B3),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Like / Favorite Heart button
                FavoriteHeartButton(
                    isLiked = isSaved,
                    onToggleLike = { track?.let { viewModel.toggleSave(it) } },
                    modifier = Modifier.size(32.dp),
                    iconSize = 22.dp,
                    unlikedColor = Color(0xFFB3B3B3),
                    likedColor = FavoriteRed
                )

                // Play / Pause button
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 9f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h1.dp)
                .background(Color(0xFF10B981), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h2.dp)
                .background(Color(0xFF10B981), RoundedCornerShape(1.dp))
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(h3.dp)
                .background(Color(0xFF10B981), RoundedCornerShape(1.dp))
        )
    }
}
