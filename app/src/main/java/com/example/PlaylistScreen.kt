package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppDownloadManager
import com.example.data.Playlist
import com.example.data.PlaylistRepository
import com.example.data.remote.models.TrackDto
import com.example.ui.*
import com.example.ui.theme.GreenPrimary

private enum class PlaylistSubPage {
    NONE,
    EDIT,
    ADD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: String,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val playlists by PlaylistRepository.playlists.collectAsState()
    val likedTracks by PlaylistRepository.likedTracks.collectAsState()
    val userProfile by PlaylistRepository.userProfile.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val downloadedTracks by AppDownloadManager.downloadedTracks.collectAsState()

    val playlist = remember(playlistId, playlists, likedTracks, downloadedTracks) {
        when {
            playlistId == "downloaded" || playlistId == "downloaded-tracks" -> {
                Playlist(
                    id = "downloaded-tracks",
                    title = "Downloaded Songs",
                    description = "Your offline available tracks",
                    coverImage = null,
                    trackCount = downloadedTracks.size,
                    owner = "You",
                    tracks = downloadedTracks,
                    colorHex = 0xFF1E3A8A,
                    isCustom = false
                )
            }
            playlistId == "liked" || playlistId == "liked-songs" || playlistId == "liked_songs" -> {
                Playlist(
                    id = "liked-songs",
                    title = "Liked Songs",
                    description = "Your favorite saved songs",
                    coverImage = null,
                    trackCount = likedTracks.size,
                    owner = "You",
                    tracks = likedTracks,
                    colorHex = 0xFF5B21B6,
                    isCustom = false
                )
            }
            else -> PlaylistRepository.getPlaylistById(playlistId)
        }
    }

    var remotePlaylist by remember { mutableStateOf<Playlist?>(null) }
    var isLoading by remember(playlistId) { mutableStateOf(true) }

    LaunchedEffect(playlistId) {
        isLoading = true
        if (playlist == null) {
            try {
                val repo = com.example.data.MusicRepository()
                val searchRes = repo.search(playlistId.replace("-", " "))
                val songs = searchRes?.songs ?: emptyList()
                if (songs.isNotEmpty()) {
                    val pl = Playlist(
                        id = playlistId,
                        title = playlistId.replace("-", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        description = "Curated collection",
                        coverImage = songs.firstOrNull()?.images?.medium ?: songs.firstOrNull()?.images?.large,
                        trackCount = songs.size,
                        owner = "Headphonix",
                        tracks = songs,
                        colorHex = 0xFF1DB954,
                        isCustom = false
                    )
                    remotePlaylist = pl
                }
            } catch (e: Exception) {
                // fallback
            } finally {
                isLoading = false
            }
        } else {
            // Smooth brief shimmer loading animation when opening any playlist
            kotlinx.coroutines.delay(220)
            isLoading = false
        }
    }

    val trackMenu = LocalTrackMenuProvider.current

    // Navigation and Sheet States
    var activeSubPage by remember { mutableStateOf(PlaylistSubPage.NONE) }
    var showNameDetailsSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(PlaylistSortOption.CUSTOM_ORDER) }
    var inPlaylistSearchQuery by remember { mutableStateOf("") }
    var isShuffleActive by remember { mutableStateOf(false) }

    if (isLoading) {
        SkeletonPlaylistScreen(onBack = onBack)
        return
    }

    val currentPl = playlist ?: remotePlaylist
    if (currentPl == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Playlist not found", color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                    Text("Go Back", color = Color.Black)
                }
            }
        }
        return
    }

    // Sub-screens routing (Edit Playlist or Add to Playlist)
    when (activeSubPage) {
        PlaylistSubPage.EDIT -> {
            EditPlaylistScreen(
                playlist = currentPl,
                onBack = { activeSubPage = PlaylistSubPage.NONE }
            )
            return
        }
        PlaylistSubPage.ADD -> {
            AddTracksToPlaylistScreen(
                playlist = currentPl,
                onBack = { activeSubPage = PlaylistSubPage.NONE }
            )
            return
        }
        PlaylistSubPage.NONE -> {
            // Continue displaying main playlist screen
        }
    }

    // Dynamic color extracted from the playlist artwork
    val dynamicThemeColor = rememberPlaylistThemeColor(currentPl)

    // Duration calculation
    val totalDurationSec = currentPl.tracks.sumOf { it.duration ?: 180 }
    val durationFormatted = if (totalDurationSec >= 3600) {
        "${totalDurationSec / 3600}h ${(totalDurationSec % 3600) / 60}m"
    } else {
        "${totalDurationSec / 60}min"
    }

    // Sorted and Filtered Tracks
    val processedTracks = remember(currentPl.tracks, currentSort, inPlaylistSearchQuery) {
        var list = currentPl.tracks

        // Filter by in-playlist search query
        if (inPlaylistSearchQuery.isNotBlank()) {
            list = list.filter { track ->
                track.title.contains(inPlaylistSearchQuery.trim(), ignoreCase = true) ||
                (track.artist ?: "").contains(inPlaylistSearchQuery.trim(), ignoreCase = true)
            }
        }

        // Apply selected sort
        when (currentSort) {
            PlaylistSortOption.CUSTOM_ORDER -> list
            PlaylistSortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            PlaylistSortOption.ARTIST -> list.sortedBy { (it.artist ?: "").lowercase() }
            PlaylistSortOption.ALBUM -> list.sortedBy { (it.album ?: "").lowercase() }
            PlaylistSortOption.RECENTLY_ADDED -> list.reversed()
        }
    }

    val allDownloaded = currentPl.tracks.isNotEmpty() && currentPl.tracks.all { track ->
        downloadedTracks.any { it.id == track.id }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
        ) {
            // 1. Header & Artwork with Dynamic Vibrant Gradient
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    dynamicThemeColor.copy(alpha = 0.90f),
                                    dynamicThemeColor.copy(alpha = 0.55f),
                                    dynamicThemeColor.copy(alpha = 0.15f),
                                    Color(0xFF121212)
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Nav: Back Arrow on its own row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sleek "Find in playlist" search bar below Back Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inPlaylistSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Find in playlist",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                BasicTextField(
                                    value = inPlaylistSearchQuery,
                                    onValueChange = { inPlaylistSearchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(Color(0xFF1ED760)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (inPlaylistSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { inPlaylistSearchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear search",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Large Centered Square Cover Artwork (220dp)
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(8.dp))
                                .background(dynamicThemeColor.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentPl.id == "downloaded" || currentPl.id == "downloaded-tracks") {
                                Icon(
                                    imageVector = Icons.Filled.DownloadDone,
                                    contentDescription = "Downloaded",
                                    tint = Color.White,
                                    modifier = Modifier.size(80.dp)
                                )
                            } else {
                                CollectionArtworkImage(
                                    tracks = currentPl.tracks,
                                    customCoverUrl = currentPl.coverImage,
                                    placeholderIcon = if (currentPl.id == "liked_songs" || currentPl.id == "liked-songs") Icons.Filled.Favorite else Icons.Filled.MusicNote,
                                    contentDescription = currentPl.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Playlist Title (bold headline)
                        Text(
                            text = currentPl.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Playlist Description
                        if (!currentPl.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentPl.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Owner Row: (+) Add collaborator + Circular Avatar badge + Owner name
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // (+) Add Collaborator icon button
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { showShareSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add collaborator",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Circular user avatar badge (e.g. blue with initial)
                            val ownerName = if (currentPl.isCustom) userProfile.name else currentPl.owner
                            val avatarInitial = ownerName.firstOrNull()?.uppercase() ?: "M"
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E77D0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarInitial,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Owner name
                            Text(
                                text = ownerName,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Visibility & Duration row: [Lock/Globe] • duration
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentPl.isPublic) Icons.Filled.Public else Icons.Filled.Lock,
                                contentDescription = if (currentPl.isPublic) "Public" else "Private",
                                tint = Color.LightGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = durationFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Controls Row: [Thumbnail, Download, Share, More] -------- [Shuffle, Play]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Miniature thumbnail icon preview
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CollectionArtworkImage(
                                        tracks = currentPl.tracks,
                                        customCoverUrl = currentPl.coverImage,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // 2. Download Button
                                IconButton(
                                    onClick = {
                                        if (allDownloaded) {
                                            currentPl.tracks.forEach { track ->
                                                AppDownloadManager.removeDownloadedTrack(context, track.id)
                                            }
                                            Toast.makeText(context, "Removed downloads for ${currentPl.title}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            currentPl.tracks.forEach { track ->
                                                AppDownloadManager.downloadTrack(context, track)
                                            }
                                            Toast.makeText(context, "Downloading tracks for offline playback", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (allDownloaded) Icons.Filled.CheckCircle else Icons.Filled.ArrowCircleDown,
                                        contentDescription = "Download Playlist",
                                        tint = if (allDownloaded) Color(0xFF1ED760) else Color.LightGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // 3. Share Button
                                IconButton(
                                    onClick = { showShareSheet = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 4. More/Options Button
                                IconButton(
                                    onClick = { showNameDetailsSheet = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "More options",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 5. Shuffle Button
                                IconButton(
                                    onClick = {
                                        isShuffleActive = !isShuffleActive
                                        Toast.makeText(context, if (isShuffleActive) "Shuffle on" else "Shuffle off", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (isShuffleActive) Color(0xFF1ED760) else Color.LightGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // 6. Big Green Circular Play Button (56dp)
                                val isCurrentPlPlaying = isPlaying && currentTrack != null && currentPl.tracks.any { it.id == currentTrack!!.id }
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1ED760))
                                        .clickable {
                                            if (currentPl.tracks.isNotEmpty()) {
                                                if (currentTrack != null && currentPl.tracks.any { it.id == currentTrack!!.id }) {
                                                    playerViewModel.togglePlayPause()
                                                } else {
                                                    val playList = if (isShuffleActive) currentPl.tracks.shuffled() else currentPl.tracks
                                                    playerViewModel.playTrack(playList.first(), playList)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPlPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isCurrentPlPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Horizontally Scrollable Management Buttons: [+ Add] [Edit] [Sort] [Name & details]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Add
                            PlaylistManagementPill(
                                icon = Icons.Filled.Add,
                                label = "Add",
                                onClick = { activeSubPage = PlaylistSubPage.ADD }
                            )

                            // 2. Edit
                            PlaylistManagementPill(
                                icon = Icons.Filled.Edit,
                                label = "Edit",
                                onClick = { activeSubPage = PlaylistSubPage.EDIT }
                            )

                            // 3. Sort
                            PlaylistManagementPill(
                                icon = Icons.Filled.SwapVert,
                                label = "Sort",
                                onClick = { showSortSheet = true }
                            )

                            // 4. Name & details
                            PlaylistManagementPill(
                                icon = Icons.Filled.EditNote,
                                label = "Name & details",
                                onClick = { showNameDetailsSheet = true }
                            )
                        }
                    }
                }
            }

            // 2. Song List
            if (processedTracks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (inPlaylistSearchQuery.isNotBlank()) "No matching songs in playlist" else "No songs in this playlist yet.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap '+ Add' above to find songs for this playlist.",
                            color = Color.DarkGray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                itemsIndexed(processedTracks, key = { _, track -> track.id }) { index, track ->
                    val isThisPlaying = currentTrack?.id == track.id
                    val isTrackLiked = PlaylistRepository.isTrackLiked(track.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerViewModel.playTrack(track, processedTracks)
                            }
                            .background(if (isThisPlaying) Color(0x221DB954) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Official Song Thumbnail (48dp)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.SongArtworkImage(
                                track = track,
                                title = track.title,
                                artist = track.artist,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isThisPlaying) Color(0xFF1ED760) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist ?: "Unknown Artist",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Like Heart Button
                        FavoriteHeartButton(
                            isLiked = isTrackLiked,
                            onToggleLike = { playerViewModel.toggleSave(track) },
                            modifier = Modifier.size(36.dp),
                            iconSize = 20.dp,
                            unlikedColor = Color.Gray,
                            likedColor = FavoriteRed
                        )

                        // 3-dots Menu
                        IconButton(
                            onClick = {
                                val onRemove: (() -> Unit)? = if (currentPl.isCustom) {
                                    { PlaylistRepository.removeTrackFromPlaylist(currentPl.id, track.id) }
                                } else null
                                trackMenu(TrackMenuState(track, onRemove))
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Options",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // "Name & details" Bottom Sheet
    if (showNameDetailsSheet) {
        PlaylistNameDetailsSheet(
            playlist = currentPl,
            onDismissRequest = { showNameDetailsSheet = false },
            onPlaylistDeleted = { onBack() }
        )
    }

    // "Sort" Bottom Sheet
    if (showSortSheet) {
        PlaylistSortBottomSheet(
            currentSort = currentSort,
            onSortSelected = { selected -> currentSort = selected },
            onDismissRequest = { showSortSheet = false }
        )
    }

    // "Share" Bottom Sheet
    if (showShareSheet) {
        PlaylistShareBottomSheet(
            playlist = currentPl,
            themeColor = dynamicThemeColor,
            onDismissRequest = { showShareSheet = false }
        )
    }
}

@Composable
private fun PlaylistManagementPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
