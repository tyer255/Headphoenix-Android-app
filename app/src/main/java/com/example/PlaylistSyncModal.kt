package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.Playlist
import com.example.data.remote.models.TrackDto

@Composable
fun PlaylistSyncModal(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onPlaylistCreated: ((Playlist) -> Unit)? = null,
    playerViewModel: PlayerViewModel? = null,
    syncViewModel: PlaylistSyncViewModel = viewModel()
) {
    if (!visible) return

    val clipboardManager = LocalClipboardManager.current
    val url by syncViewModel.url.collectAsState()
    val syncState by syncViewModel.syncState.collectAsState()

    val spotifyGreen = Color(0xFF1ED760)

    Dialog(
        onDismissRequest = {
            syncViewModel.dismissError()
            onDismissRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        syncViewModel.dismissError()
                        onDismissRequest()
                    }
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Main Modal Container
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(320)
                ) + fadeIn(animationSpec = tween(250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(250)
                ) + fadeOut(animationSpec = tween(200)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF151916),
                                    Color(0xFF0F1210),
                                    Color(0xFF0C0E0D)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF2E3831),
                                    Color(0xFF1A201C),
                                    Color.Transparent
                                )
                            ),
                            RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Stop propagation */ }
                        )
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Drag / Grab Bar & Close button row
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            // Subtle grab bar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(38.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF333D35))
                            )

                            // Close icon button
                            IconButton(
                                onClick = {
                                    syncViewModel.dismissError()
                                    onDismissRequest()
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(32.dp)
                                    .testTag("playlist_sync_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF88968D),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Modal Title: SYNC TO HEADPHONIX
                        Text(
                            text = "SYNC PLAYLIST TO HEADPHONIX",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 2.dp, bottom = 14.dp)
                                .testTag("sync_playlist_pro_title")
                        )

                        // Spotify -> Headphonix Transfer Visual Section with Ambient Glow
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(102.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Ambient green glow behind the tiles
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                spotifyGreen.copy(alpha = 0.22f),
                                                spotifyGreen.copy(alpha = 0.06f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )

                            // Two floating squircle cards connected by dedicated transfer arrow
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left tile: Spotify logo (Source platform)
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .graphicsLayer {
                                            rotationZ = -3f
                                        }
                                        .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = spotifyGreen.copy(alpha = 0.35f))
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF2C3530),
                                                    Color(0xFF1B221E)
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFF425046),
                                            RoundedCornerShape(20.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SyncBrandLogo(
                                        size = 38.dp,
                                        tint = spotifyGreen,
                                        modifier = Modifier.graphicsLayer { rotationZ = 3f }
                                    )
                                }

                                // Center: Dedicated Transfer / Sync Directional Arrow
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .shadow(8.dp, CircleShape, ambientColor = Color.Black, spotColor = spotifyGreen.copy(alpha = 0.4f))
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF2E3D32),
                                                        Color(0xFF18221B)
                                                    )
                                                )
                                            )
                                            .border(1.5.dp, Color(0xFF4D6654), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Transfer Spotify playlist into Headphonix",
                                            tint = spotifyGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "TRANSFER",
                                        color = spotifyGreen,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                // Right tile: Headphonix Logo (Destination app - crisp, clear, unblurred)
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .graphicsLayer {
                                            rotationZ = 3f
                                        }
                                        .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black, spotColor = Color(0xFF4A6050).copy(alpha = 0.4f))
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF2C3530),
                                                    Color(0xFF1B221E)
                                                )
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFF506155),
                                            RoundedCornerShape(20.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.headphonix_logo),
                                        contentDescription = "Headphonix",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .graphicsLayer { rotationZ = -3f }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Instruction Text: PASTE YOUR SPOTIFY PLAYLIST LINK BELOW.
                        Text(
                            text = "PASTE SPOTIFY PLAYLIST LINK TO IMPORT INTO HEADPHONIX",
                            color = Color(0xFF98A69E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // URL Input Container (Pill with vibrant green border + Paste button)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF121614))
                                .border(
                                    1.5.dp,
                                    spotifyGreen,
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Green link icon
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = spotifyGreen,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Text input field
                                BasicTextField(
                                    value = url,
                                    onValueChange = { syncViewModel.onUrlChange(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("playlist_sync_url_input"),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(spotifyGreen),
                                    decorationBox = { innerTextField ->
                                        if (url.isEmpty()) {
                                            Text(
                                                text = "https://open.spotify.com/playlist/...",
                                                color = Color(0xFF5A665E),
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Clear button if text present
                                if (url.isNotEmpty()) {
                                    IconButton(
                                        onClick = { syncViewModel.clearUrl() },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = Color(0xFF88968D),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

                                // Dark Capsule "Paste" Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF282E2B))
                                        .clickable {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                syncViewModel.pasteFromClipboard(clip)
                                            }
                                        }
                                        .padding(horizontal = 18.dp, vertical = 9.dp)
                                        .testTag("playlist_sync_paste_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Paste",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Error Banner (if error)
                        if (syncState is PlaylistSyncState.Error) {
                            val errorMsg = (syncState as PlaylistSyncState.Error).message
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1616)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMsg,
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { syncViewModel.dismissError() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Synced Banner (when all tracks are synced and ready for creation)
                        if (syncState is PlaylistSyncState.Synced) {
                            val synced = syncState as PlaylistSyncState.Synced
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playlist_synced_summary_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF142419)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, spotifyGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Cover
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF242E28)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!synced.coverImage.isNullOrBlank()) {
                                            AsyncImage(
                                                model = synced.coverImage,
                                                contentDescription = synced.originalPlaylistName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            SyncBrandLogo(size = 28.dp, tint = spotifyGreen)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = spotifyGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "READY TO CLONE",
                                                color = spotifyGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = synced.originalPlaylistName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${synced.syncedTrackCount} songs synced successfully",
                                            color = Color(0xFFA6B5AC),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                if (synced.tracks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFF242E28))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "SYNCED TRACKS (${synced.tracks.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = spotifyGreen,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp)
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 14.dp, vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        synced.tracks.forEachIndexed { idx, track ->
                                            SyncedTrackPreviewItem(
                                                index = idx + 1,
                                                track = track
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }

                        // Success Banner (if created)
                        if (syncState is PlaylistSyncState.Success) {
                            val success = syncState as PlaylistSyncState.Success
                            val playlist = success.playlist
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playlist_created_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF142419)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, spotifyGreen.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Cover
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF242E28)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!playlist.coverImage.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = playlist.coverImage,
                                                    contentDescription = playlist.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                SyncBrandLogo(size = 28.dp, tint = spotifyGreen)
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = spotifyGreen,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "CLONED TO LIBRARY",
                                                    color = spotifyGreen,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = playlist.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${success.trackCount} songs synced successfully",
                                                color = Color(0xFFA6B5AC),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                onDismissRequest()
                                                onNavigateToPlaylist(playlist.id)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("view_playlist_button"),
                                            shape = RoundedCornerShape(19.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = spotifyGreen,
                                                contentColor = Color.Black
                                            )
                                        ) {
                                            Text(
                                                "View Playlist",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        if (playerViewModel != null && playlist.tracks.isNotEmpty()) {
                                            OutlinedButton(
                                                onClick = {
                                                    playerViewModel.playTrack(playlist.tracks.first(), playlist.tracks)
                                                    onDismissRequest()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .testTag("play_now_button"),
                                                shape = RoundedCornerShape(19.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B4A40)),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Play Now",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Action Buttons: Clone Playlist vs Syncing vs Create Playlist vs Creating vs Success
                        when (val state = syncState) {
                            is PlaylistSyncState.Idle, is PlaylistSyncState.Error -> {
                                Button(
                                    onClick = {
                                        syncViewModel.clonePlaylist()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .shadow(14.dp, RoundedCornerShape(50), spotColor = spotifyGreen.copy(alpha = 0.45f))
                                        .testTag("clone_playlist_button"),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = spotifyGreen,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = "Clone Playlist",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            is PlaylistSyncState.Syncing -> {
                                // Syncing in progress: Create Playlist is strictly NOT shown
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("syncing_playlist_button"),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = spotifyGreen.copy(alpha = 0.35f),
                                        disabledContentColor = Color.White
                                    )
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = spotifyGreen,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    val progressText = if (state.totalCount > 0) {
                                        "${state.processedCount} / ${state.totalCount} songs synced"
                                    } else {
                                        state.message
                                    }
                                    Text(
                                        text = progressText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            is PlaylistSyncState.Synced -> {
                                // Sync complete: ONLY NOW show "Create Playlist"
                                Button(
                                    onClick = {
                                        syncViewModel.createPlaylist { createdPl ->
                                            onPlaylistCreated?.invoke(createdPl)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .shadow(14.dp, RoundedCornerShape(50), spotColor = spotifyGreen.copy(alpha = 0.45f))
                                        .testTag("create_playlist_button"),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = spotifyGreen,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.headphonix_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Import to Headphonix",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            is PlaylistSyncState.Creating -> {
                                // Creating in progress: duplicate protection, disabled button
                                Button(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("creating_playlist_button"),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        disabledContainerColor = spotifyGreen.copy(alpha = 0.5f),
                                        disabledContentColor = Color.Black
                                    )
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Creating Playlist...",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            is PlaylistSyncState.Success -> {
                                // Created successfully
                                Button(
                                    onClick = {
                                        onDismissRequest()
                                        onNavigateToPlaylist(state.playlist.id)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .shadow(14.dp, RoundedCornerShape(50), spotColor = spotifyGreen.copy(alpha = 0.45f))
                                        .testTag("created_view_playlist_button"),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = spotifyGreen,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Text(
                                        text = "View in Library",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Bottom Informational Note (Accurate, Non-misleading)
                        Text(
                            text = "DIRECT PLAYLIST CLONER | HIGH-SPEED TRACK EXTRACTION.\nINSTANT LOCAL SYNC TO YOUR LIBRARY.",
                            color = Color(0xFF6B7A71),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.8.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncedTrackPreviewItem(
    index: Int,
    track: TrackDto
) {
    val trackImage = track.images?.small ?: track.images?.medium ?: track.images?.large
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B2A1E))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(20.dp)
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF282828)),
            contentAlignment = Alignment.Center
        ) {
            if (!trackImage.isNullOrBlank()) {
                AsyncImage(
                    model = trackImage,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist ?: "Unknown Artist",
                color = Color(0xFFA6B5AC),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val durSec = track.duration ?: 180
        Text(
            text = "${durSec / 60}:${(durSec % 60).toString().padStart(2, '0')}",
            color = Color.Gray,
            fontSize = 10.sp
        )
    }
}
