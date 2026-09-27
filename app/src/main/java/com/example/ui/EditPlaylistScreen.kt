package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Playlist
import com.example.data.PlaylistRepository
import com.example.data.remote.models.TrackDto

@Composable
fun EditPlaylistScreen(
    playlist: Playlist,
    onBack: () -> Unit
) {
    var workingTracks by remember(playlist) { mutableStateOf(playlist.tracks) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Edit playlist",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        PlaylistRepository.reorderTracks(playlist.id, workingTracks)
                        onBack()
                    }
                ) {
                    Text(
                        text = "Save",
                        color = Color(0xFF1ED760),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        bottomBar = {
            // "Select" pill button at the bottom matching reference
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF282828))
                        .padding(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Select",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        if (workingTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracks in playlist",
                    color = Color.Gray,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(workingTracks, key = { _, track -> track.id }) { index, track ->
                    EditTrackRow(
                        track = track,
                        onRemove = {
                            workingTracks = workingTracks.filterIndexed { i, _ -> i != index }
                        },
                        canMoveUp = index > 0,
                        canMoveDown = index < workingTracks.size - 1,
                        onMoveUp = {
                            if (index > 0) {
                                val list = workingTracks.toMutableList()
                                val temp = list[index]
                                list[index] = list[index - 1]
                                list[index - 1] = temp
                                workingTracks = list
                            }
                        },
                        onMoveDown = {
                            if (index < workingTracks.size - 1) {
                                val list = workingTracks.toMutableList()
                                val temp = list[index]
                                list[index] = list[index + 1]
                                list[index + 1] = temp
                                workingTracks = list
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditTrackRow(
    track: TrackDto,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF181818))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus remove button: Circular minus icon matching reference
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Thumbnail with tiny play overlay
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            val thumbUrl = track.images?.small ?: track.images?.medium ?: track.images?.large
            AsyncImage(
                model = thumbUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Tiny play overlay
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist ?: "Unknown Artist",
                color = Color.Gray,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Reorder Up/Down / Drag Handle
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canMoveUp) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (canMoveDown) {
                IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Reorder handle",
                tint = Color.Gray,
                modifier = Modifier
                    .padding(start = 4.dp, end = 4.dp)
                    .size(22.dp)
            )
        }
    }
}
