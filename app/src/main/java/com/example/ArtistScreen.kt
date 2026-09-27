package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.PlaylistRepository
import com.example.data.remote.models.TrackDto
import com.example.ui.SkeletonArtistScreen
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistId: String,
    playerViewModel: PlayerViewModel,
    onNavigateToAlbum: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val artistViewModel: ArtistViewModel = viewModel()
    var isFollowing by remember { mutableStateOf(false) }
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val trackMenu = LocalTrackMenuProvider.current
    val playlists by PlaylistRepository.playlists.collectAsState()
    val uiState by artistViewModel.uiState.collectAsState()

    LaunchedEffect(artistId) {
        artistViewModel.fetchArtist(artistId)
    }

    val listState = rememberLazyListState()
    val headerTranslationY by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset * 0.5f
            } else {
                0f
            }
        }
    }
    val headerAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                1f - (listState.firstVisibleItemScrollOffset / 600f).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    val showTopBarName by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 400
        }
    }

    when (val state = uiState) {
        is ArtistUiState.Loading -> {
            SkeletonArtistScreen(onBack = onBack)
        }
        is ArtistUiState.Success -> {
            val artist = state.data
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().background(Color(0xFF121212)),
                    contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .graphicsLayer {
                                    translationY = headerTranslationY
                                    alpha = headerAlpha
                                }
                        ) {
                            AsyncImage(
                                model = artist.image,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(Color.DarkGray)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.3f),
                                                Color.Transparent,
                                                Color(0xFF121212)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val formattedListeners = artist.monthlyListeners?.toString()?.reversed()?.chunked(3)?.joinToString(",")?.reversed() ?: "48,512,940"
                                Text(
                                    text = "$formattedListeners monthly listeners",
                                    color = Color.LightGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { isFollowing = !isFollowing },
                                    shape = RoundedCornerShape(percent = 50),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                                ) {
                                    Text(if (isFollowing) "Following" else "Follow")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {}) {
                                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DB954))
                                        .clickable { 
                                            artist.topTracks?.let { tracks ->
                                                if (tracks.isNotEmpty()) playerViewModel.playTrack(tracks.first(), tracks)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Tabs
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Music", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.width(30.dp).height(2.dp).background(Color(0xFF1DB954)))
                            }
                            Text("Clips", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Events", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Popular Tracks Section
                    if (!artist.topTracks.isNullOrEmpty()) {
                        item {
                            Text(
                                text = "Popular",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }
                        items(artist.topTracks) { track ->
                            val isCurrentTrack = currentTrack?.id == track.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { playerViewModel.playTrack(track, artist.topTracks) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                AsyncImage(
                                    model = track.images?.medium ?: track.images?.small,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = if (isCurrentTrack) Color(0xFF1DB954) else Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val randomStreams = remember { (10000000..500000000).random().toString().reversed().chunked(3).joinToString(",").reversed() }
                                    Text(
                                        text = randomStreams,
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { trackMenu(TrackMenuState(track)) }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
                                }
                            }
                        }
                    }

                    // Albums Section
                    if (!artist.albums.isNullOrEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Popular Releases",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(artist.albums) { album ->
                                    Column(
                                        modifier = Modifier.width(140.dp).clickable { onNavigateToAlbum(album.id) }
                                    ) {
                                        AsyncImage(
                                            model = album.images?.medium ?: album.images?.small,
                                            contentDescription = album.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(140.dp)
                                                .background(Color.DarkGray)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = album.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${album.year ?: ""} • Album",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Singles Section
                    if (!artist.singles.isNullOrEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Singles & EPs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(artist.singles) { single ->
                                    Column(
                                        modifier = Modifier.width(140.dp).clickable { playerViewModel.playTrack(single) }
                                    ) {
                                        AsyncImage(
                                            model = single.images?.medium ?: single.images?.small,
                                            contentDescription = single.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(140.dp)
                                                .background(Color.DarkGray)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = single.title,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(text = "Single", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }
                
                // Overlay Top App Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (showTopBarName) Color(0xFF121212) else Color.Transparent)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (showTopBarName) Color.Transparent else Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        if (showTopBarName) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = artist.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        }
        is ArtistUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error loading artist", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { artistViewModel.fetchArtist(artistId) }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
