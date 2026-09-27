package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppDownloadManager
import com.example.data.DownloadState
import com.example.data.Playlist
import com.example.data.PlaylistRepository
import com.example.data.remote.models.TrackDto

/**
 * Context Menu matching temp_repo/src/components/Common/ContextMenu.tsx exactly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
    track: TrackDto,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onToggleSave: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onViewArtist: (String) -> Unit,
    onViewAlbum: (String) -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val shareProvider = LocalShareProvider.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val downloadStatusMap by AppDownloadManager.downloadStatus.collectAsState()
    val downloadState = downloadStatusMap[track.id]
    val playlists by PlaylistRepository.playlists.collectAsState()
    var showPlaylistPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF171717), // Neutral-900 like website
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF404040)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // Header: Track Thumbnail, Title, Artist, Close X
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.images?.small ?: track.images?.medium ?: track.images?.large,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF262626))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        color = Color(0xFFA3A3A3),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA3A3A3),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFF262626),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (!showPlaylistPicker) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    if (onRemoveFromPlaylist != null) {
                        item {
                            ActionRow(
                                icon = Icons.Outlined.RemoveCircleOutline,
                                label = "Remove from this playlist",
                                onClick = {
                                    onRemoveFromPlaylist()
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // 1. Like / Remove from Liked Songs
                    item {
                        ActionRow(
                            icon = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            label = if (isSaved) "Remove from Liked Songs" else "Save to your Liked Songs",
                            iconColor = if (isSaved) Color(0xFFEF4444) else Color(0xFFA3A3A3),
                            textColor = Color.White,
                            onClick = {
                                onToggleSave()
                                onDismiss()
                            }
                        )
                    }

                    // 2. Add to Queue
                    item {
                        ActionRow(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            label = "Add to Queue",
                            onClick = {
                                onAddToQueue()
                                Toast.makeText(context, "Added \"${track.title}\" to Queue", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                    }

                    // 3. Add to Playlist
                    item {
                        ActionRow(
                            icon = Icons.Default.Add,
                            label = "Add to Playlist",
                            onClick = {
                                showPlaylistPicker = true
                            }
                        )
                    }

                    // 4. Download Offline / Remove Download
                    item {
                        val isDownloading = downloadState is DownloadState.Downloading
                        val isDownloaded = downloadState is DownloadState.Completed
                        val dlLabel = when {
                            isDownloading -> "Downloading (${((downloadState as DownloadState.Downloading).progress * 100).toInt()}%)"
                            isDownloaded -> "Remove Download"
                            else -> "Download Offline"
                        }
                        ActionRow(
                            icon = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Filled.Download,
                            label = dlLabel,
                            iconColor = if (isDownloaded || isDownloading) Color(0xFF10B981) else Color(0xFFA3A3A3),
                            onClick = {
                                if (isDownloaded) {
                                    AppDownloadManager.removeDownloadedTrack(context, track.id)
                                    Toast.makeText(context, "Removed download", Toast.LENGTH_SHORT).show()
                                } else {
                                    AppDownloadManager.downloadTrack(context, track)
                                    Toast.makeText(context, "Downloading \"${track.title}\"", Toast.LENGTH_SHORT).show()
                                }
                                onDismiss()
                            }
                        )
                    }

                    // 5. Go to Artist
                    item {
                        ActionRow(
                            icon = Icons.Outlined.Person,
                            label = "Go to Artist (${track.artist})",
                            onClick = {
                                val aId = track.artistId ?: track.artist
                                onViewArtist(aId)
                                onDismiss()
                            }
                        )
                    }

                    // 6. Go to Album
                    if (track.album != null) {
                        item {
                            ActionRow(
                                icon = Icons.Outlined.Album,
                                label = "Go to Album (${track.album})",
                                onClick = {
                                    val albId = track.albumId ?: track.album ?: ""
                                    onViewAlbum(albId)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // 7. Share Track
                    item {
                        ActionRow(
                            icon = Icons.Outlined.Share,
                            label = "Share Track",
                            onClick = {
                                shareProvider(track)
                                onDismiss()
                            }
                        )
                    }
                }
            } else {
                // Submenu: Select Playlist
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECT PLAYLIST",
                            color = Color(0xFFA3A3A3),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        TextButton(onClick = { showPlaylistPicker = false }) {
                            Text("Back", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(playlists) { pl ->
                            val alreadyInPlaylist = pl.tracks.any { it.id == track.id }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        PlaylistRepository.addTrackToPlaylist(pl.id, track)
                                        Toast.makeText(context, "Added to ${pl.title}", Toast.LENGTH_SHORT).show()
                                        showPlaylistPicker = false
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF262626)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = pl.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (alreadyInPlaylist) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Added",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    iconColor: Color = Color(0xFFA3A3A3),
    textColor: Color = Color(0xFFE5E5E5),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
