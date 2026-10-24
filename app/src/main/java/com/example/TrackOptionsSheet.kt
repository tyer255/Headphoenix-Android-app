package com.example

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
import com.example.data.PlaylistRepository
import com.example.data.remote.models.TrackDto
import com.example.service.PlayerManager
import com.example.ui.theme.GreenPrimary

private enum class TrackSheetSubView {
    MAIN,
    PLAYLIST_PICKER,
    SLEEP_TIMER,
    SONG_CREDITS
}

/**
 * Spotify Android 3-Dot Track Context Menu Bottom Sheet with interactive
 * Add to Playlist, Sleep Timer, Song Credits, Go to Song Radio, Download, and Share.
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
    val activeSleepTimer by PlayerManager.sleepTimerMinutes.collectAsState()
    var currentSubView by remember { mutableStateOf(TrackSheetSubView.MAIN) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1F1F1F),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF535353)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // Header: Track Thumbnail, Title, Artist • Album, Close X
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.images?.small ?: track.images?.medium ?: track.images?.large,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF282828))
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
                        text = buildString {
                            append(track.artist)
                            if (!track.album.isNullOrBlank()) {
                                append(" • ")
                                append(track.album)
                            }
                        },
                        color = Color(0xFFA7A7A7),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (currentSubView != TrackSheetSubView.MAIN) {
                    TextButton(onClick = { currentSubView = TrackSheetSubView.MAIN }) {
                        Text("Back", color = GreenPrimary, fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA7A7A7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = Color(0xFF333333),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            when (currentSubView) {
                TrackSheetSubView.MAIN -> {
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
                                icon = if (isSaved) Icons.Filled.CheckCircle else Icons.Outlined.AddCircleOutline,
                                label = if (isSaved) "Remove from Liked Songs" else "Add to Liked Songs",
                                iconColor = if (isSaved) GreenPrimary else Color(0xFFB3B3B3),
                                textColor = Color.White,
                                onClick = {
                                    onToggleSave()
                                    onDismiss()
                                }
                            )
                        }

                        // 2. Add to playlist
                        item {
                            ActionRow(
                                icon = Icons.Default.Add,
                                label = "Add to playlist",
                                onClick = {
                                    currentSubView = TrackSheetSubView.PLAYLIST_PICKER
                                }
                            )
                        }

                        // 3. Add to Queue
                        item {
                            ActionRow(
                                icon = Icons.AutoMirrored.Filled.QueueMusic,
                                label = "Add to Queue",
                                onClick = {
                                    onAddToQueue()
                                    Toast.makeText(context, "Added to Queue", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            )
                        }

                        // 4. Download / Remove download
                        item {
                            val isDownloading = downloadState is DownloadState.Downloading
                            val isDownloaded = downloadState is DownloadState.Completed
                            val dlLabel = when {
                                isDownloading -> {
                                    val state = downloadState as DownloadState.Downloading
                                    if (state.speedText.isNotBlank()) {
                                        "Downloading (${(state.progress * 100).toInt()}% • ${state.speedText})"
                                    } else {
                                        "Downloading (${(state.progress * 100).toInt()}%)"
                                    }
                                }
                                isDownloaded -> "Remove download"
                                else -> "Download"
                            }
                            ActionRow(
                                icon = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Outlined.ArrowCircleDown,
                                label = dlLabel,
                                iconColor = if (isDownloaded || isDownloading) GreenPrimary else Color(0xFFB3B3B3),
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

                        // 5. Go to Song Radio
                        item {
                            ActionRow(
                                icon = Icons.Outlined.Radio,
                                label = "Go to Song Radio",
                                onClick = {
                                    PlayerManager.playTrack(track, PlayerManager.getRecommendedTracks())
                                    Toast.makeText(context, "Starting ${track.title} Radio", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            )
                        }

                        // 6. Go to Album
                        if (!track.album.isNullOrBlank()) {
                            item {
                                ActionRow(
                                    icon = Icons.Outlined.Album,
                                    label = "Go to album",
                                    onClick = {
                                        val albId = track.albumId ?: track.album ?: ""
                                        onViewAlbum(albId)
                                        onDismiss()
                                    }
                                )
                            }
                        }

                        // 7. Go to Artist
                        item {
                            ActionRow(
                                icon = Icons.Outlined.Person,
                                label = "Go to artist",
                                onClick = {
                                    val aId = track.artistId ?: track.artist
                                    onViewArtist(aId)
                                    onDismiss()
                                }
                            )
                        }

                        // 8. Sleep timer
                        item {
                            val timerSubtitle = if (activeSleepTimer != null) " • ${activeSleepTimer}m active" else ""
                            ActionRow(
                                icon = Icons.Outlined.Bedtime,
                                label = "Sleep timer$timerSubtitle",
                                iconColor = if (activeSleepTimer != null) GreenPrimary else Color(0xFFB3B3B3),
                                onClick = {
                                    currentSubView = TrackSheetSubView.SLEEP_TIMER
                                }
                            )
                        }

                        // 9. View song credits
                        item {
                            ActionRow(
                                icon = Icons.Outlined.MusicNote,
                                label = "View song credits",
                                onClick = {
                                    currentSubView = TrackSheetSubView.SONG_CREDITS
                                }
                            )
                        }

                        // 10. Share
                        item {
                            ActionRow(
                                icon = Icons.Outlined.Share,
                                label = "Share",
                                onClick = {
                                    shareProvider(track)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                TrackSheetSubView.PLAYLIST_PICKER -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text(
                            text = "Add to playlist",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(playlists, key = { it.id }) { pl ->
                                val alreadyInPlaylist = pl.tracks.any { it.id == track.id }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            PlaylistRepository.addTrackToPlaylist(pl.id, track)
                                            Toast.makeText(context, "Added to ${pl.title}", Toast.LENGTH_SHORT).show()
                                            currentSubView = TrackSheetSubView.MAIN
                                            onDismiss()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF282828)),
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.title,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${pl.tracks.size} songs",
                                            color = Color(0xFFA7A7A7),
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (alreadyInPlaylist) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Added",
                                            tint = GreenPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                TrackSheetSubView.SLEEP_TIMER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Stop audio in",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val timerOptions = listOf(
                            5 to "5 minutes",
                            10 to "10 minutes",
                            15 to "15 minutes",
                            30 to "30 minutes",
                            45 to "45 minutes",
                            60 to "1 hour",
                            1 to "End of track"
                        )

                        timerOptions.forEach { (mins, label) ->
                            val isSelected = activeSleepTimer == mins
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val durationToSet = if (mins == 1) {
                                            val remSec = (PlayerManager.durationSec.value - PlayerManager.currentPositionSec.value).coerceAtLeast(30)
                                            (remSec / 60).coerceAtLeast(1)
                                        } else {
                                            mins
                                        }
                                        PlayerManager.setSleepTimer(durationToSet)
                                        Toast.makeText(context, "Sleep timer set for $label", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) GreenPrimary else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Active",
                                        tint = GreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (activeSleepTimer != null) {
                            HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        PlayerManager.setSleepTimer(null)
                                        Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Turn off timer",
                                    color = Color(0xFFEF4444),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                TrackSheetSubView.SONG_CREDITS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Song credits",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        CreditSection(title = "Performed by", value = track.artist)
                        CreditSection(title = "Written by", value = "${track.artist}, Official Songwriters")
                        CreditSection(title = "Produced by", value = "${track.artist} Studio Production")
                        CreditSection(title = "Source", value = "Headphonix Audio • ${track.album ?: "Original Release"}")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditSection(title: String, value: String) {
    Column {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color(0xFFA7A7A7),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    iconColor: Color = Color(0xFFB3B3B3),
    textColor: Color = Color.White,
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
