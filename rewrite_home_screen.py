import re

content = """package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Playlist
import com.example.data.PlaylistRepository
import com.example.data.remote.models.TrackDto
import com.example.data.PlaybackHistoryRepository
import com.example.ui.theme.GreenPrimary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlaylist: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToSearch: (String) -> Unit = {},
    trackMenu: (TrackMenuState) -> Unit = {},
    openProfileDrawer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentlyPlayedHistory by PlaybackHistoryRepository.history.collectAsState()
    val playlists by PlaylistRepository.playlists.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = 0.dp)
        ) {
            // Header: Avatar & Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE6121212))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF282828))
                            .clickable { openProfileDrawer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.LightGray, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterPill("All", selected = selectedFilter == "All", onClick = { selectedFilter = "All" })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterPill("Music", selected = selectedFilter == "Music", onClick = { selectedFilter = "Music" })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterPill("Podcasts", selected = selectedFilter == "Podcasts", onClick = { selectedFilter = "Podcasts" })
                }
            }

            if (selectedFilter == "Podcasts") {
                item {
                    Text(
                        text = "Top 10 Hindi Podcasts & Shows",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
                items(10) { index ->
                    PodcastCardFake(index)
                }
                return@LazyColumn
            }

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = GreenPrimary)
                        }
                    }
                }
                is HomeUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x33FF0000))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WifiOff, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Connection Error", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Could not connect to servers. Check internet.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                is HomeUiState.Success -> {
                    val data = state.data
                    
                    // Recommended for today
                    if (!data.popularSongs.isNullOrEmpty()) {
                        item {
                            SectionTitle("Recommended for today")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(data.popularSongs.take(8)) { track ->
                                    TrackCard(
                                        track = track, 
                                        queue = data.popularSongs, 
                                        playerViewModel = playerViewModel, 
                                        onNavigateToArtist = onNavigateToArtist, 
                                        onOptionsClick = { trackMenu(TrackMenuState(it)) },
                                        contentType = "Single"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // Start Listening (Grid of Compact Tracks)
                    if (!data.quickPicks.isNullOrEmpty()) {
                        item {
                            Text(
                                text = "Jump into a session based on your tastes",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, bottom = 4.dp)
                            )
                            SectionTitle("Start listening")
                            
                            val chunks = data.quickPicks.take(6).chunked(2)
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                chunks.forEach { rowTracks ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        rowTracks.forEachIndexed { idx, track ->
                                            Box(modifier = Modifier.weight(1f).padding(end = if (idx == 0 && rowTracks.size > 1) 8.dp else 0.dp)) {
                                                CompactTrackCard(
                                                    track = track,
                                                    queue = data.quickPicks,
                                                    playerViewModel = playerViewModel,
                                                    onOptionsClick = { trackMenu(TrackMenuState(it)) }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // Trending Hits
                    if (!data.trending.isNullOrEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle("Trending Hits")
                                Text("GLOBAL POP", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(data.trending) { track ->
                                    TrackCard(
                                        track = track,
                                        queue = data.trending,
                                        playerViewModel = playerViewModel,
                                        onNavigateToArtist = onNavigateToArtist,
                                        onOptionsClick = { trackMenu(TrackMenuState(it)) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // Recents
                    if (recentlyPlayedHistory.isNotEmpty() || playlists.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle("Recents")
                                Text("Show all", color = Color.Gray, fontSize = 12.sp)
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                // Liked Songs Tile
                                item {
                                    LikedSongsTile(onClick = { onNavigateToPlaylist("liked-songs") })
                                }
                                
                                // User Playlists
                                items(playlists.take(3)) { pl ->
                                    PlaylistCard(playlist = pl, onClick = { onNavigateToPlaylist(pl.id) })
                                }

                                // Played History
                                items(recentlyPlayedHistory.take(10)) { track ->
                                    TrackCard(
                                        track = track,
                                        queue = recentlyPlayedHistory,
                                        playerViewModel = playerViewModel,
                                        onNavigateToArtist = onNavigateToArtist,
                                        onOptionsClick = { trackMenu(TrackMenuState(it)) },
                                        subtitle = "Recent"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // Popular Artists
                    if (!data.popularArtists.isNullOrEmpty()) {
                        item {
                            SectionTitle("Your favourite artists")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(data.popularArtists) { artist ->
                                    ArtistCircleCard(
                                        id = artist.id,
                                        name = artist.name,
                                        imageUrl = artist.image ?: "",
                                        onNavigateToArtist = onNavigateToArtist
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LikedSongsTile(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, animationSpec = tween(150), label = "scale")

    Column(
        modifier = Modifier
            .width(150.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF4B32C3)), // Purple-ish blue
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Liked Songs", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Playlist", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun FilterPill(text: String, selected: Boolean = false, onClick: () -> Unit) {
    val bgColor = if (selected) GreenPrimary else Color(0x14FFFFFF)
    val textColor = if (selected) Color.Black else Color(0xFFE5E5E5)
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 16.dp, bottom = 12.dp)
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TrackCard(
    track: TrackDto,
    queue: List<TrackDto>,
    playerViewModel: PlayerViewModel,
    onNavigateToArtist: (String) -> Unit,
    onOptionsClick: (TrackDto) -> Unit,
    contentType: String? = null,
    subtitle: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, animationSpec = tween(150), label = "scale")

    Column(
        modifier = Modifier
            .width(150.dp)
            .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { playerViewModel.playTrack(track, queue) }
            )
    ) {
        Box(
            modifier = Modifier
                .size(126.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF282828))
        ) {
            AsyncImage(
                model = track.images?.medium ?: track.images?.small,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        if (contentType != null) {
            Text(text = contentType, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subtitle ?: track.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clickable {
                    track.artistId?.let { onNavigateToArtist(it) }
                }
            )
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = Color.Gray,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onOptionsClick(track) }
            )
        }
    }
}

@Composable
fun CompactTrackCard(
    track: TrackDto,
    queue: List<TrackDto>,
    playerViewModel: PlayerViewModel,
    onOptionsClick: (TrackDto) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, animationSpec = tween(150))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x1AFFFFFF))
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { playerViewModel.playTrack(track, queue) }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.images?.small ?: track.images?.medium,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = track.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onOptionsClick(track) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.Gray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ArtistCircleCard(
    id: String,
    name: String,
    imageUrl: String,
    onNavigateToArtist: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, animationSpec = tween(150))

    Column(
        modifier = Modifier
            .width(130.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interactionSource, indication = null) { onNavigateToArtist(id) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(130.dp).clip(CircleShape).background(Color(0xFF282828))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = "Artist", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun PlaylistCard(playlist: Playlist, onClick: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, animationSpec = tween(150))

    Column(
        modifier = Modifier
            .width(150.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(126.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF282828)),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.coverImage != null) {
                AsyncImage(model = playlist.coverImage, contentDescription = playlist.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = playlist.title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = playlist.description ?: "${playlist.tracks.size} songs", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AlbumCard(album: com.example.data.remote.models.AlbumDto, onClick: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, animationSpec = tween(150))

    Column(
        modifier = Modifier
            .width(150.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .padding(12.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        AsyncImage(
            model = album.images?.medium ?: album.images?.small,
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(126.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF282828))
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = album.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = album.artist ?: "Unknown", color = Color.Gray, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PodcastCardFake(index: Int) {
    val titles = listOf(
        "The Ranveer Show (Hindi)", "Figuring Out", "Dostcast", "RealHit Podcast", 
        "ANI Podcast with Smita Prakash", "Prakhar Ke Pravachan", "Bharti TV Podcast", 
        "Untriggered with Aminjaz", "Shubhankar Mishra Podcast", "Sandeep Maheshwari Show"
    )
    val publishers = listOf(
        "Ranveer Allahbadia", "Raj Shamani", "Vinamre Kasanaa", "RealHit", 
        "ANI News", "Prakhar Gupta", "Bharti Singh", "Aminjaz", "Shubhankar Mishra", "Sandeep Maheshwari"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x1AFFFFFF))
            .padding(12.dp)
    ) {
        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titles[index], color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text = "Show • ${publishers[index]}", color = Color.Gray, fontSize = 13.sp)
        }
    }
}
"""

with open("app/src/main/java/com/example/HomeScreen.kt", "w") as f:
    f.write(content)
