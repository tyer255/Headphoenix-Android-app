package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.remote.models.ExtractedTrack
import com.example.data.remote.models.PlaylistExtractResponse
import com.example.data.remote.models.TrackDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistExtractorScreen(
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit = {},
    extractorViewModel: PlaylistExtractorViewModel = viewModel()
) {
    val clipboardManager = LocalClipboardManager.current
    val url by extractorViewModel.url.collectAsState()
    val uiState by extractorViewModel.uiState.collectAsState()
    val importMessage by extractorViewModel.importSuccessMessage.collectAsState()

    val spotifyGreen = Color(0xFF1DB954)
    val darkCardBackground = Color(0xFF1E1E1E)
    val surfaceHighlight = Color(0xFF282828)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(importMessage) {
        importMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            extractorViewModel.clearImportMessage()
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        contentColor = Color.White,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Playlist Extractor",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Extract tracks from Spotify links",
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("extractor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(spotifyGreen.copy(alpha = 0.15f))
                            .border(1.dp, spotifyGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = spotifyGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Spotify API",
                                color = spotifyGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Input & Controls Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = darkCardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = null,
                                tint = spotifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Spotify Playlist URL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // URL Input Field
                        OutlinedTextField(
                            value = url,
                            onValueChange = { extractorViewModel.onUrlChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("spotify_url_input"),
                            placeholder = {
                                Text(
                                    "https://open.spotify.com/playlist/...",
                                    color = Color(0xFF666666),
                                    fontSize = 13.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = spotifyGreen,
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF141414),
                                unfocusedContainerColor = Color(0xFF141414)
                            ),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (url.isNotEmpty()) {
                                        IconButton(
                                            onClick = { extractorViewModel.clearUrl() },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = Color(0xFFAAAAAA),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                extractorViewModel.pasteFromClipboard(clip)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("paste_url_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = spotifyGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick sample links chips
                        Text(
                            text = "Sample Playlists:",
                            fontSize = 11.sp,
                            color = Color(0xFF888888),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SampleChip(
                                label = "Today's Top Hits",
                                onClick = {
                                    extractorViewModel.onUrlChange("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M")
                                }
                            )
                            SampleChip(
                                label = "All Out 80s (100)",
                                onClick = {
                                    extractorViewModel.onUrlChange("https://open.spotify.com/playlist/37i9dQZF1DX4UtSsGT1Sbe")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Extract Button
                        val isLoading = uiState is PlaylistExtractorUiState.Loading
                        Button(
                            onClick = { extractorViewModel.extractPlaylist() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("extract_playlist_button"),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = spotifyGreen,
                                disabledContainerColor = spotifyGreen.copy(alpha = 0.5f),
                                contentColor = Color.Black,
                                disabledContentColor = Color.Black.copy(alpha = 0.6f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Extracting Playlist...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Extract Playlist",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. Loading State Banner
            if (uiState is PlaylistExtractorUiState.Loading) {
                item {
                    val loadingState = uiState as PlaylistExtractorUiState.Loading
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = darkCardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = spotifyGreen,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Processing Spotify Playlist",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = loadingState.message,
                                    fontSize = 12.sp,
                                    color = Color(0xFFAAAAAA)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Error State Banner
            if (uiState is PlaylistExtractorUiState.Error) {
                item {
                    val errorState = uiState as PlaylistExtractorUiState.Error
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Error",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Extraction Failed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFCDD2)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errorState.message,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE0E0E0),
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(
                                onClick = { extractorViewModel.dismissError() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Extracted Playlist Info Header & Tracks
            if (uiState is PlaylistExtractorUiState.Success) {
                val successState = uiState as PlaylistExtractorUiState.Success
                val response = successState.response
                val playlist = response.playlist
                val tracks = response.tracks

                item {
                    ExtractedPlaylistHeroCard(
                        response = response,
                        onPlayAll = {
                            if (tracks.isNotEmpty()) {
                                val trackDtos = tracks.mapIndexed { idx, t ->
                                    with(extractorViewModel) { t.toTrackDto(idx) }
                                }
                                playerViewModel.playTrack(trackDtos.first(), trackDtos)
                            }
                        },
                        onImportToLibrary = {
                            extractorViewModel.importToLibrary { importedId ->
                                onNavigateToPlaylist(importedId)
                            }
                        }
                    )
                }

                // Truncated limitation notice if >= 100 tracks
                if (response.truncated) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF262215)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = response.note ?: "This extractor currently supports the first 100 tracks of this playlist.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFFE082),
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Section Header: Track Count
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extracted Tracks (${response.totalExtracted})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${response.totalExtracted} tracks",
                            fontSize = 12.sp,
                            color = spotifyGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Track items
                itemsIndexed(tracks) { index, track ->
                    ExtractedTrackItem(
                        index = index + 1,
                        track = track,
                        onClick = {
                            val trackDtos = tracks.mapIndexed { idx, t ->
                                with(extractorViewModel) { t.toTrackDto(idx) }
                            }
                            playerViewModel.playTrack(trackDtos[index], trackDtos)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFCCCCCC),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ExtractedPlaylistHeroCard(
    response: PlaylistExtractResponse,
    onPlayAll: () -> Unit,
    onImportToLibrary: () -> Unit
) {
    val playlist = response.playlist
    val spotifyGreen = Color(0xFF1DB954)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Playlist Cover Image
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2C)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!playlist?.image.isNullOrBlank()) {
                        AsyncImage(
                            model = playlist?.image,
                            contentDescription = playlist?.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Metadata Column
                Column(modifier = Modifier.weight(1f)) {
                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(spotifyGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${response.totalExtracted} TRACKS EXTRACTED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = spotifyGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = playlist?.name ?: "Spotify Playlist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!playlist?.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playlist?.description ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFAAAAAA),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Play All Button
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = spotifyGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Save to Library Button
                OutlinedButton(
                    onClick = onImportToLibrary,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF444444)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save Library",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtractedTrackItem(
    index: Int,
    track: ExtractedTrack,
    onClick: () -> Unit
) {
    val durationFormatted = remember(track.duration) {
        val totalSec = (track.duration / 1000).toInt()
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        "%d:%02d".format(minutes, seconds)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track Index Number
        Text(
            text = "$index",
            fontSize = 12.sp,
            color = Color(0xFF777777),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(28.dp)
        )

        // Artwork (using track's own individual artwork)
        val coverArt = track.getEffectiveArtwork()
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            com.example.ui.SongArtworkImage(
                imageUrl = coverArt,
                title = track.name,
                artist = track.artist,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                fontSize = 12.sp,
                color = Color(0xFFAAAAAA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration
        Text(
            text = durationFormatted,
            fontSize = 12.sp,
            color = Color(0xFF888888)
        )
    }
}
