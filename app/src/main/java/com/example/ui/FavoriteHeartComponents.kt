package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val FavoriteRed = Color(0xFFE53935)

/**
 * Reusable Centralized Heart Icon.
 * - UNLIKED: Outline heart (FavoriteBorder), normal/inactive tint.
 * - LIKED: Filled heart (Favorite), RED color.
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
        animationSpec = spring(),
        label = "heartColorAnim"
    )

    Icon(
        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = contentDescription,
        tint = tintColor,
        modifier = modifier.size(iconSize)
    )
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
