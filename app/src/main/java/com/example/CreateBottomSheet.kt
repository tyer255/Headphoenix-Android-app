package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CreateBottomSheet(
    onDismissRequest: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistSyncClick: () -> Unit = {},
    onExtractPlaylistClick: () -> Unit = onPlaylistSyncClick
) {
    // This perfectly mirrors the CreateActionMenu.tsx from the web app
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        // Outer Box receives clicks on empty space outside the menu card and dismisses
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
                .navigationBarsPadding()
                .padding(bottom = 68.dp, start = 12.dp, end = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevents clicking inside card from dismissing
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .padding(12.dp)
            ) {
                ActionMenuItem(
                    icon = { Icon(Icons.Filled.MusicNote, null, tint = Color.LightGray) },
                    title = "Playlist",
                    subtitle = "Create a playlist with songs or episodes",
                    onClick = { 
                        onDismissRequest()
                        onCreatePlaylistClick()
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                ActionMenuItem(
                    icon = { SyncBrandLogo(size = 22.dp, tint = Color(0xFF1ED760)) },
                    title = "Playlist Sync",
                    subtitle = "Import tracks from Spotify into Headphonix",
                    onClick = { 
                        onDismissRequest()
                        onPlaylistSyncClick()
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                ActionMenuItem(
                    icon = { Icon(Icons.Filled.People, null, tint = Color.LightGray) },
                    title = "Collaborative playlist",
                    subtitle = "Create a playlist together with friends",
                    onClick = { 
                        onDismissRequest()
                        onCreatePlaylistClick() // Same action for now, type can be handled later
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                ActionMenuItem(
                    icon = { 
                        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.size(12.dp).offset(x = (-4).dp).clip(CircleShape).background(Color.LightGray))
                            Box(modifier = Modifier.size(12.dp).offset(x = 4.dp).clip(CircleShape).background(Color.LightGray.copy(alpha = 0.8f)))
                        }
                    },
                    title = "Blend",
                    subtitle = "Combine your friends' tastes into a playlist",
                    onClick = { 
                        onDismissRequest()
                        // Blend flow
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF282828))
                .border(1.dp, Color(0x1AFFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
        }
    }
}
