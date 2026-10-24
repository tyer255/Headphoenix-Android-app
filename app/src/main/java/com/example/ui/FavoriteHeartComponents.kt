package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val FavoriteRed = Color(0xFF1ED760)
val SpotifyGreen = Color(0xFF1ED760)

/**
 * Reusable Centralized Heart Icon with Spotify's signature scale-pulse & ring burst animation
 * (matches `.heart-liked` keyframes in `PlayingBar.scss`).
 */
@Composable
fun FavoriteHeartIcon(
    isLiked: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    unlikedColor: Color = Color.White.copy(alpha = 0.7f),
    likedColor: Color = FavoriteRed,
    contentDescription: String? = if (isLiked) "Remove from Liked Songs" else "Save to Liked Songs"
) {
    val tintColor by animateColorAsState(
        targetValue = if (isLiked) likedColor else unlikedColor,
        animationSpec = tween(durationMillis = 200),
        label = "heartColorAnim"
    )

    val pulseScale = remember { Animatable(1f) }
    val ringProgress = remember { Animatable(0f) }
    var previousLiked by remember { mutableStateOf(isLiked) }

    LaunchedEffect(isLiked) {
        if (isLiked != previousLiked) {
            previousLiked = isLiked
            if (isLiked) {
                ringProgress.snapTo(0f)
                pulseScale.animateTo(
                    targetValue = 1.32f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
                ringProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                )
                pulseScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            } else {
                pulseScale.animateTo(0.82f, animationSpec = tween(90))
                pulseScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        if (ringProgress.value > 0f && ringProgress.value < 1f) {
            val p = ringProgress.value
            Canvas(modifier = Modifier.size(iconSize * 1.6f)) {
                drawCircle(
                    color = likedColor.copy(alpha = (1f - p) * 0.65f),
                    radius = (size.minDimension / 2f) * (0.6f + 0.5f * p),
                    style = Stroke(width = 2.dp.toPx() * (1f - p))
                )
            }
        }
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier
                .size(iconSize)
                .scale(pulseScale.value)
        )
    }
}

/**
 * Reusable Centralized Heart IconButton.
 */
@Composable
fun FavoriteHeartButton(
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    unlikedColor: Color = Color.White.copy(alpha = 0.7f),
    likedColor: Color = FavoriteRed,
    contentDescription: String = if (isLiked) "Remove from Liked Songs" else "Save to Liked Songs"
) {
    IconButton(
        onClick = onToggleLike,
        modifier = modifier
    ) {
        FavoriteHeartIcon(
            isLiked = isLiked,
            iconSize = iconSize,
            unlikedColor = unlikedColor,
            likedColor = likedColor,
            contentDescription = contentDescription
        )
    }
}

/**
 * Spotify Animated 4-Bar Equalizer Indicator for currently playing tracks in Search & Playlist rows.
 */
@Composable
fun SpotifyEqualizerIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = SpotifyGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spotifyEq")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isPlaying) 0.95f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isPlaying) 0.2f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isPlaying) 1.0f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(490, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar3"
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = if (isPlaying) 0.3f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(390, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar4"
    )

    Row(
        modifier = modifier.size(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3, bar4).forEach { fraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction.coerceIn(0.2f, 1f))
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Spotify Circle Play Button (`CirclePlay.scss`) with tactile press-scale spring animation.
 */
@Composable
fun SpotifyCirclePlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 52.dp,
    iconSize: Dp = 28.dp,
    containerColor: Color = SpotifyGreen,
    iconColor: Color = Color.Black
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "circlePlayScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .size(buttonSize)
            .shadow(10.dp, CircleShape, spotColor = containerColor.copy(alpha = 0.55f))
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
