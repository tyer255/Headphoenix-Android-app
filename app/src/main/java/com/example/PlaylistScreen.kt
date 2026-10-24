package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDownloadManager
import com.example.data.Playlist
import com.example.data.PlaylistRepository
import com.example.data.remote.models.TrackDto
import com.example.ui.*
import com.example.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
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

    // Recommended songs state (matches `src/pages/Playlist/recommendations/index.tsx` in reference repo)
    var recommendedTracks by remember { mutableStateOf<List<TrackDto>>(emptyList()) }
    var isRefreshingRecs by remember { mutableStateOf(false) }
    var recQueryIndex by remember { mutableIntStateOf(0) }

    val recSeedQueries = remember(playlist?.tracks, remotePlaylist?.tracks) {
        val currentTracks = playlist?.tracks ?: remotePlaylist?.tracks ?: emptyList()
        val artists = currentTracks.mapNotNull { it.artist.takeIf { a -> a.isNotBlank() } }.distinct()
        if (artists.isNotEmpty()) {
            artists + listOf("Top Hindi Hits", "Punjabi Pop Hits", "Bollywood Acoustic", "Indie India")
        } else {
            listOf("Arijit Singh Hits", "Anuv Jain", "Karan Aujla", "Bollywood Top 50", "Coke Studio")
        }
    }

    fun loadRecommendations(seedIdx: Int) {
        scope.launch {
            isRefreshingRecs = true
            try {
                val repo = com.example.data.MusicRepository()
                val q = recSeedQueries[seedIdx % recSeedQueries.size]
                val res = repo.search(q)
                val existingIds = (playlist?.tracks ?: remotePlaylist?.tracks ?: emptyList()).map { it.id }.toSet()
                val candidates = (res?.songs ?: emptyList()).filter { it.id !in existingIds }.take(6)
                if (candidates.isNotEmpty()) {
                    recommendedTracks = candidates
                }
            } catch (_: Exception) {
            } finally {
                isRefreshingRecs = false
            }
        }
    }

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
                        owner = "Spotify",
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
            kotlinx.coroutines.delay(200)
            isLoading = false
        }
        loadRecommendations(0)
    }

    val trackMenu = LocalTrackMenuProvider.current

    // Navigation and Sheet States
    var activeSubPage by remember { mutableStateOf(PlaylistSubPage.NONE) }
    var showNameDetailsSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var currentSort by remember { mutableStateOf(PlaylistSortOption.CUSTOM_ORDER) }
    var inPlaylistSearchQuery by remember { mutableStateOf("") }
    var isShuffleActive by remember { mutableStateOf(true) }

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
        PlaylistSubPage.NONE -> {}
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

        if (inPlaylistSearchQuery.isNotBlank()) {
            list = list.filter { track ->
                track.title.contains(inPlaylistSearchQuery.trim(), ignoreCase = true) ||
                (track.artist ?: "").contains(inPlaylistSearchQuery.trim(), ignoreCase = true)
            }
        }

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

    val isCurrentPlPlaying = isPlaying && currentTrack != null && currentPl.tracks.any { it.id == currentTrack!!.id }

    // Scroll tracking for Spotify Collapsing Sticky Header
    val listState = rememberLazyListState()
    val showStickyHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 520
        }
    }
    val stickyHeaderAlpha by animateFloatAsState(
        targetValue = if (showStickyHeader) 1f else 0f,
        animationSpec = tween(220),
        label = "stickyHeaderAlpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
        ) {
            // 1. Header & Artwork with Dynamic Vibrant Gradient (`PlaylistPage.scss`)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    dynamicThemeColor.copy(alpha = 0.92f),
                                    dynamicThemeColor.copy(alpha = 0.55f),
                                    dynamicThemeColor.copy(alpha = 0.16f),
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

                        // Sleek "Find in playlist" search bar + Sort button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (inPlaylistSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Find in playlist",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    BasicTextField(
                                        value = inPlaylistSearchQuery,
                                        onValueChange = { inPlaylistSearchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        cursorBrush = SolidColor(Color(0xFF1ED760)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (inPlaylistSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { inPlaylistSearchQuery = "" },
                                        modifier = Modifier.size(26.dp)
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

                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .clickable { showSortSheet = true }
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sort",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // Large Centered Square Cover Artwork (224dp) with deep Spotify shadow
                        Box(
                            modifier = Modifier
                                .size(224.dp)
                                .align(Alignment.CenterHorizontally)
                                .shadow(24.dp, RoundedCornerShape(6.dp), spotColor = Color.Black.copy(alpha = 0.75f))
                                .clip(RoundedCornerShape(6.dp))
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

                        Spacer(modifier = Modifier.height(22.dp))

                        // Playlist Title (Spotify bold headline)
                        Text(
                            text = currentPl.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 25.sp,
                            letterSpacing = (-0.5).sp,
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
                                color = Color(0xFFB3B3B3),
                                fontSize = 13.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Owner Row: Circular Avatar badge + Owner name
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val ownerName = if (currentPl.isCustom) userProfile.name else currentPl.owner
                            val avatarInitial = ownerName.firstOrNull()?.uppercase() ?: "S"
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1ED760)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarInitial,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = ownerName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Visibility, Track count & Duration row: [Globe/Lock] • X songs, duration
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (currentPl.isPublic) Icons.Filled.Public else Icons.Filled.Lock,
                                contentDescription = if (currentPl.isPublic) "Public" else "Private",
                                tint = Color(0xFFB3B3B3),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentPl.tracks.size} songs • $durationFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB3B3B3),
                                fontSize = 12.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Controls Row: [Thumbnail, Download, Add Collaborator, Share, More] -------- [Shuffle (with green dot), 56dp CirclePlay]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Miniature thumbnail icon preview (Spotify Canvas/Artwork preview box)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF282828)),
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
                                            Toast.makeText(context, "Downloading ${currentPl.title}...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (allDownloaded) Icons.Filled.CheckCircle else Icons.Filled.ArrowCircleDown,
                                        contentDescription = "Download Playlist",
                                        tint = if (allDownloaded) Color(0xFF1ED760) else Color(0xFFB3B3B3),
                                        modifier = Modifier.size(25.dp)
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
                                        tint = Color(0xFFB3B3B3),
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
                                        tint = Color(0xFFB3B3B3),
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 5. Shuffle Button with Spotify active green dot indicator underneath
                                Box(contentAlignment = Alignment.Center) {
                                    IconButton(
                                        onClick = {
                                            isShuffleActive = !isShuffleActive
                                            Toast.makeText(context, if (isShuffleActive) "Shuffle on" else "Shuffle off", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Shuffle,
                                            contentDescription = "Shuffle",
                                            tint = if (isShuffleActive) Color(0xFF1ED760) else Color(0xFFB3B3B3),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    if (isShuffleActive) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .offset(y = (-3).dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1ED760))
                                        )
                                    }
                                }

                                // 6. Big Green Circular Play Button (56dp) with tactile spring bounce
                                SpotifyCirclePlayButton(
                                    isPlaying = isCurrentPlPlaying,
                                    buttonSize = 56.dp,
                                    iconSize = 30.dp,
                                    onClick = {
                                        if (currentPl.tracks.isNotEmpty()) {
                                            if (currentTrack != null && currentPl.tracks.any { it.id == currentTrack!!.id }) {
                                                playerViewModel.togglePlayPause()
                                            } else {
                                                val playList = if (isShuffleActive) currentPl.tracks.shuffled() else currentPl.tracks
                                                playerViewModel.playTrack(playList.first(), playList)
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Horizontally Scrollable Management Pills: [+ Add] [Edit] [Sort] [Name & details]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlaylistManagementPill(
                                icon = Icons.Filled.Add,
                                label = "Add to this playlist",
                                onClick = { activeSubPage = PlaylistSubPage.ADD }
                            )
                            PlaylistManagementPill(
                                icon = Icons.Filled.Edit,
                                label = "Edit",
                                onClick = { activeSubPage = PlaylistSubPage.EDIT }
                            )
                            PlaylistManagementPill(
                                icon = Icons.Filled.SwapVert,
                                label = "Sort",
                                onClick = { showSortSheet = true }
                            )
                            PlaylistManagementPill(
                                icon = Icons.Filled.EditNote,
                                label = "Name & details",
                                onClick = { showNameDetailsSheet = true }
                            )
                        }
                    }
                }
            }

            // 2. Song List (`PlaylistPage.scss` `.song-list`)
            if (processedTracks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (inPlaylistSearchQuery.isNotBlank()) "No matching songs in playlist" else "Let's start building your playlist",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Add to this playlist' above or add from recommendations below.",
                            color = Color(0xFFB3B3B3),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                itemsIndexed(processedTracks, key = { idx, track -> "${track.id}_$idx" }) { index, track ->
                    val isThisTrackCurrent = currentTrack?.id == track.id
                    val isTrackLiked = PlaylistRepository.isTrackLiked(track.id)
                    val isTrackDownloaded = downloadedTracks.any { it.id == track.id }
                    val durSec = (track.duration ?: 195).coerceAtLeast(1)
                    val formattedDur = "${durSec / 60}:${(durSec % 60).toString().padStart(2, '0')}"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerViewModel.playTrack(track, processedTracks)
                            }
                            .background(if (isThisTrackCurrent) Color(0x1F1ED760) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Official Song Thumbnail (48dp) with dark overlay + Equalizer when playing
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF282828)),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.SongArtworkImage(
                                track = track,
                                title = track.title,
                                artist = track.artist,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isThisTrackCurrent) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SpotifyEqualizerIcon(isPlaying = isPlaying)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title, Equalizer/Explicit badge & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = if (isThisTrackCurrent) Color(0xFF1ED760) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTrackDownloaded) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowCircleDown,
                                        contentDescription = "Downloaded",
                                        tint = Color(0xFF1ED760),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                // Explicit 'E' badge on selected tracks like Spotify
                                if (index % 3 == 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFB3B3B3)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "E",
                                            color = Color(0xFF121212),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))
                                }
                                Text(
                                    text = track.artist ?: "Unknown Artist",
                                    color = Color(0xFFB3B3B3),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Track Duration (`m:ss`)
                        Text(
                            text = formattedDur,
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        // Like Heart Button with Spotify pulse animation
                        FavoriteHeartButton(
                            isLiked = isTrackLiked,
                            onToggleLike = { playerViewModel.toggleSave(track) },
                            modifier = Modifier.size(34.dp),
                            iconSize = 19.dp,
                            unlikedColor = Color(0xFFB3B3B3),
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
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Options",
                                tint = Color(0xFFB3B3B3),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 3. Official Spotify "Recommended Songs" Section (`src/pages/Playlist/recommendations/index.tsx` & `PlaylistPage.scss`)
            if (recommendedTracks.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 10.dp)
                    ) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Recommended Songs",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Based on the songs in this playlist",
                            color = Color(0xFFB3B3B3),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                itemsIndexed(recommendedTracks, key = { _, rec -> "rec_${rec.id}" }) { _, recTrack ->
                    val isRecPlaying = currentTrack?.id == recTrack.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerViewModel.playTrack(recTrack, recommendedTracks)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF282828)),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.SongArtworkImage(
                                track = recTrack,
                                title = recTrack.title,
                                artist = recTrack.artist,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isRecPlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.55f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SpotifyEqualizerIcon(isPlaying = isPlaying)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recTrack.title,
                                color = if (isRecPlaying) Color(0xFF1ED760) else Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = recTrack.artist ?: "Recommended Artist",
                                color = Color(0xFFB3B3B3),
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Spotify "Add" circle/pill button (`add-song` in `PlaylistPage.scss`)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Color(0xFF727272), RoundedCornerShape(50))
                                .clickable {
                                    if (currentPl.id == "liked-songs" || currentPl.id == "liked") {
                                        playerViewModel.toggleSave(recTrack)
                                    } else {
                                        PlaylistRepository.addTrackToPlaylist(currentPl.id, recTrack)
                                    }
                                    recommendedTracks = recommendedTracks.filter { it.id != recTrack.id }
                                    Toast.makeText(context, "Added to ${currentPl.title}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add to playlist",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Refresh button at the bottom of Recommended Songs
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val spinAngle by animateFloatAsState(
                            targetValue = if (isRefreshingRecs) 360f else 0f,
                            animationSpec = tween(500),
                            label = "refreshSpin"
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, Color(0xFF727272), RoundedCornerShape(50))
                                .clickable {
                                    recQueryIndex += 1
                                    loadRecommendations(recQueryIndex)
                                }
                                .padding(horizontal = 22.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh recommendations",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(16.dp)
                                    .rotate(spinAngle)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRefreshingRecs) "Refreshing..." else "Refresh",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Sticky Collapsing Spotify Top Bar (appears when scrolled past playlist artwork)
        if (stickyHeaderAlpha > 0.01f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dynamicThemeColor.copy(alpha = 0.96f * stickyHeaderAlpha),
                                Color(0xFF121212).copy(alpha = 0.92f * stickyHeaderAlpha)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .height(58.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentPl.title,
                        color = Color.White.copy(alpha = stickyHeaderAlpha),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(
                    visible = showStickyHeader,
                    enter = scaleIn(tween(200)) + fadeIn(tween(200)),
                    exit = scaleOut(tween(150)) + fadeOut(tween(150))
                ) {
                    SpotifyCirclePlayButton(
                        isPlaying = isCurrentPlPlaying,
                        buttonSize = 42.dp,
                        iconSize = 22.dp,
                        onClick = {
                            if (currentPl.tracks.isNotEmpty()) {
                                if (currentTrack != null && currentPl.tracks.any { it.id == currentTrack!!.id }) {
                                    playerViewModel.togglePlayPause()
                                } else {
                                    val playList = if (isShuffleActive) currentPl.tracks.shuffled() else currentPl.tracks
                                    playerViewModel.playTrack(playList.first(), playList)
                                }
                            }
                        }
                    )
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
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 7.dp),
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
            fontWeight = FontWeight.SemiBold
        )
    }
}
