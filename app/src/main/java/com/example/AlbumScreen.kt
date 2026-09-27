package com.example

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.PlaylistRepository
import com.example.ui.FavoriteHeartButton
import com.example.ui.FavoriteRed
import com.example.ui.SkeletonAlbumScreen
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumId: String,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val albumViewModel: AlbumViewModel = viewModel()
    
    LaunchedEffect(albumId) {
        albumViewModel.loadAlbum(albumId)
    }
    
    val uiState by albumViewModel.uiState.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val trackMenu = LocalTrackMenuProvider.current

    when (val state = uiState) {
        is AlbumUiState.Loading -> {
            SkeletonAlbumScreen(onBack = onBack)
        }
        is AlbumUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)), contentAlignment = Alignment.Center) {
                Text(state.message, color = Color.White)
            }
        }
        is AlbumUiState.Success -> {
            val album = state.album
            val isAlbumPlaying = isPlaying && currentTrack != null && (album.tracks ?: emptyList()).any { it.id == currentTrack!!.id }
            
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
                // Background Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.DarkGray,
                                    Color(0xFF121212)
                                )
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            navigationIconContentColor = Color.White
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = album.images?.large ?: album.images?.medium,
                                    contentDescription = album.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(220.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = album.artist,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = " • ${album.year ?: ""} • ${album.tracks?.size ?: 0} songs",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Play button row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row {
                                        IconButton(onClick = { /* Download */ }) {
                                            Icon(Icons.Filled.Download, contentDescription = "Download", tint = Color.Gray)
                                        }
                                        IconButton(onClick = { /* Save album */ }) {
                                            Icon(Icons.Filled.AddCircleOutline, contentDescription = "Save", tint = Color.Gray)
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(GreenPrimary)
                                            .clickable {
                                                if ((album.tracks ?: emptyList()).isNotEmpty()) {
                                                    if (isAlbumPlaying) {
                                                        playerViewModel.togglePlayPause()
                                                    } else {
                                                        playerViewModel.playTrack((album.tracks ?: emptyList()).first(), (album.tracks ?: emptyList()))
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isAlbumPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Play Album",
                                            tint = Color.Black,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        itemsIndexed(album.tracks ?: emptyList()) { index, track ->
                            val isThisPlaying = currentTrack?.id == track.id
                            val isLiked = PlaylistRepository.isTrackLiked(track.id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playerViewModel.playTrack(track, album.tracks ?: emptyList()) }
                                    .background(if (isThisPlaying) Color(0x221DB954) else Color.Transparent)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.CenterStart) {
                                    if (isThisPlaying && isPlaying) {
                                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text("${index + 1}", color = if (isThisPlaying) GreenPrimary else Color.Gray, fontSize = 14.sp)
                                    }
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isThisPlaying) GreenPrimary else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                FavoriteHeartButton(
                                    isLiked = isLiked,
                                    onToggleLike = { playerViewModel.toggleSave(track) },
                                    iconSize = 20.dp,
                                    unlikedColor = Color.Gray,
                                    likedColor = FavoriteRed
                                )
                                IconButton(onClick = { trackMenu(TrackMenuState(track)) }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
