package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Playlist
import com.example.data.PlaylistRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistNameDetailsSheet(
    playlist: Playlist,
    onDismissRequest: () -> Unit,
    onPlaylistDeleted: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(playlist) { mutableStateOf(playlist.title) }
    var description by remember(playlist) { mutableStateOf(playlist.description ?: "") }
    var isPublic by remember(playlist) { mutableStateOf(playlist.isPublic) }
    var customCoverUri by remember(playlist) { mutableStateOf(playlist.coverImage) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Android Photo Picker for choosing 1 photo from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            customCoverUri = uri.toString()
            PlaylistRepository.updatePlaylistCover(playlist.id, uri.toString())
            Toast.makeText(context, "Cover photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 20.dp)
        ) {
            // Top Bar: Cancel | Name & details | Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Name & details",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = {
                        PlaylistRepository.updatePlaylistDetails(
                            playlistId = playlist.id,
                            name = name.trim().ifBlank { playlist.title },
                            description = description.trim(),
                            isPublic = isPublic
                        )
                        customCoverUri?.let {
                            PlaylistRepository.updatePlaylistCover(playlist.id, it)
                        }
                        Toast.makeText(context, "Playlist updated", Toast.LENGTH_SHORT).show()
                        onDismissRequest()
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Save",
                        color = Color(0xFF1ED760),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cover + Inputs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail with edit pencil icon (Click to pick 1 photo from gallery)
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF282828))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CollectionArtworkImage(
                        tracks = playlist.tracks,
                        customCoverUrl = customCoverUri ?: playlist.coverImage,
                        contentDescription = playlist.title,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Edit pencil icon in bottom right
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit cover",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Name & Description Inputs
                Column(modifier = Modifier.weight(1f)) {
                    // Playlist Name input box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Playlist name", color = Color.Gray, fontSize = 14.sp) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF1ED760),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Playlist Description input box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 90.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        TextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Add a description", color = Color.Gray, fontSize = 13.sp) },
                            maxLines = 3,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = Color.LightGray,
                                unfocusedTextColor = Color.LightGray,
                                cursorColor = Color(0xFF1ED760),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Options: Make public / private
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        isPublic = !isPublic
                        Toast.makeText(
                            context,
                            if (isPublic) "Playlist is now Public" else "Playlist is now Private",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPublic) Icons.Filled.Public else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (isPublic) Color(0xFF1ED760) else Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isPublic) "Make private" else "Make public",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Option: Delete playlist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDeleteConfirmDialog = true }
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Delete playlist",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = Color(0xFF282828),
            title = {
                Text(
                    text = "Delete ${playlist.title}?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "This will delete the playlist from Your Library.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        PlaylistRepository.deletePlaylist(playlist.id)
                        onDismissRequest()
                        onPlaylistDeleted()
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}
