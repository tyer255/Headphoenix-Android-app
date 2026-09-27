package com.example.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dynamic content bottom padding that automatically accounts for:
 * 1. Android System Navigation Bar (Gesture bar, 3-button navigation, foldables)
 * 2. Headphonix persistent BottomNavigationBar (when visible on current route)
 * 3. Headphonix MiniPlayer (when playback or track is loaded)
 * 4. Extra safe clearance buffer so bottom-most items are never cramped or overlapping.
 */
val LocalBottomContentPadding = compositionLocalOf { 160.dp }

@Composable
fun calculateBottomContentPadding(
    shouldShowBottomBar: Boolean,
    hasMiniPlayer: Boolean
): Dp {
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (shouldShowBottomBar) {
        58.dp + (if (hasMiniPlayer) 68.dp else 0.dp) + navBarsBottom + 24.dp
    } else {
        navBarsBottom + 24.dp
    }
}
