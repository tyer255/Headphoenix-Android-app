package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
import com.example.ui.CollectionArtworkImage
import com.example.ui.theme.GreenPrimary

sealed class LibraryDisplayItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetRoute: String,
    val isPinned: Boolean = false
) {
    class LikedSongs(val count: Int) : LibraryDisplayItem(
        id = "liked-songs",
        title = "Liked Songs",
        subtitle = "Playlist • $count songs",
        targetRoute = "playlist/liked-songs",
        isPinned = true
    )

    class DownloadedSongs(val count: Int) : LibraryDisplayItem(
        id = "downloaded-tracks",
        title = "Downloaded Songs",
        subtitle = "Playlist • $count songs (Offline)",
        targetRoute = "playlist/downloaded-tracks",
        isPinned = true
    )

    class UserPlaylistItem(val playlist: Playlist) : LibraryDisplayItem(
        id = playlist.id,
        title = playlist.title,
        subtitle = "Playlist • ${playlist.owner}${if (playlist.tracks.isNotEmpty()) " • ${playlist.tracks.size} songs" else ""}",
        targetRoute = "playlist/${playlist.id}",
        isPinned = false
    )

    class DownloadedTrackItem(val track: TrackDto) : LibraryDisplayItem(
        id = track.id,
        title = track.title,
        subtitle = "${track.artist} • Offline",
        targetRoute = "track/${track.id}",
        isPinned = false
    )

    class ActionItem(
        val actionId: String,
        title: String,
        subtitle: String = "",
        val isCircle: Boolean = false,
        val iconVector: androidx.compose.ui.graphics.vector.ImageVector
    ) : LibraryDisplayItem(
        id = actionId,
        title = title,
        subtitle = subtitle,
        targetRoute = actionId,
        isPinned = false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToPlaylist: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onNavigateToExtractor: () -> Unit = {},
    playerViewModel: PlayerViewModel? = null
) {
    val context = LocalContext.current
    val playlists by PlaylistRepository.playlists.collectAsState()
    val likedTracks by PlaylistRepository.likedTracks.collectAsState()
    val downloadedTracks by AppDownloadManager.downloadedTracks.collectAsState()
    val userProfile by PlaylistRepository.userProfile.collectAsState()

    var isGridView by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") } // All, Playlists, Artists, Albums, Podcasts, Downloaded
    var sortOrder by remember { mutableStateOf("Recents") } // Recents, Recently Added, Alphabetical, Creator
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    var showCreateBottomSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPlaylistSyncModal by remember { mutableStateOf(false) }

    // Build items based on filters and search
    val displayItems = remember(playlists, likedTracks, downloadedTracks, selectedFilter, searchQuery, sortOrder) {
        val items = mutableListOf<LibraryDisplayItem>()

        when (selectedFilter) {
            "Downloaded" -> {
                items.add(LibraryDisplayItem.DownloadedSongs(downloadedTracks.size))
                downloadedTracks.forEach { track ->
                    items.add(LibraryDisplayItem.DownloadedTrackItem(track))
                }
            }
            "Playlists" -> {
                if (likedTracks.isNotEmpty()) {
                    items.add(LibraryDisplayItem.LikedSongs(likedTracks.size))
                }
                if (downloadedTracks.isNotEmpty()) {
                    items.add(LibraryDisplayItem.DownloadedSongs(downloadedTracks.size))
                }
                playlists.forEach { pl ->
                    items.add(LibraryDisplayItem.UserPlaylistItem(pl))
                }
                items.add(
                    LibraryDisplayItem.ActionItem(
                        actionId = "action_playlist_sync",
                        title = "Playlist Sync",
                        subtitle = "Clone & sync playlists",
                        isCircle = false,
                        iconVector = Icons.Filled.Add
                    )
                )
            }
            "Artists" -> {
                items.add(
                    LibraryDisplayItem.ActionItem(
                        actionId = "action_add_artists",
                        title = "Add artists",
                        subtitle = "",
                        isCircle = true,
                        iconVector = Icons.Filled.Add
                    )
                )
            }
            "Albums" -> {
                // Albums filter
            }
            "Podcasts" -> {
                items.add(
                    LibraryDisplayItem.ActionItem(
                        actionId = "action_add_podcasts",
                        title = "Add podcasts",
                        subtitle = "",
                        isCircle = false,
                        iconVector = Icons.Filled.Add
                    )
                )
            }
            else -> {
                // "All" filter
                items.add(LibraryDisplayItem.LikedSongs(likedTracks.size))
                items.add(LibraryDisplayItem.DownloadedSongs(downloadedTracks.size))
                playlists.forEach { pl ->
                    items.add(LibraryDisplayItem.UserPlaylistItem(pl))
                }
                // Dedicated Library Action Items matching Screenshot_20260920_143545
                items.add(
                    LibraryDisplayItem.ActionItem(
                        actionId = "action_playlist_sync",
                        title = "Playlist Sync",
                        subtitle = "Clone & sync playlists",
                        isCircle = false,
                        iconVector = Icons.Filled.Add
                    )
                )
                items.add(
                    LibraryDisplayItem.ActionItem(
                        actionId = "action_add_artists",
                        title = "Add artists",
                        subtitle = "",
                        isCircle = true,
                        iconVector = Icons.Filled.Add
                    )
                )
                items.add(
                    LibraryDisplayItem.ActionItem(
                        actionId = "action_add_podcasts",
                        title = "Add podcasts",
                        subtitle = "",
                        isCircle = false,
                        iconVector = Icons.Filled.Add
                    )
                )
                items.add(
                    LibraryDisplayItem.ActionItem(
                        actionId = "action_add_events",
                        title = "Add events and venues",
                        subtitle = "",
                        isCircle = false,
                        iconVector = Icons.Filled.Add
                    )
                )
                items.add(
                    LibraryDisplayItem.ActionItem(
                        actionId = "action_import_music",
                        title = "Import your music",
                        subtitle = "",
                        isCircle = false,
                        iconVector = Icons.Filled.ArrowDownward
                    )
                )
            }
        }

        var filtered = if (searchQuery.isNotBlank()) {
            items.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
            }
        } else {
            items
        }

        when (sortOrder) {
            "Alphabetical" -> filtered.sortedWith(compareBy({ !it.isPinned }, { it.title.lowercase() }))
            "Creator" -> filtered.sortedWith(compareBy({ !it.isPinned }, { it.subtitle.lowercase() }))
            else -> filtered
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
    ) {
        // 1. Header (User Profile Avatar + "Your Library" + Search + Plus)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF282828))
                    .clickable { onOpenProfile() },
                contentAlignment = Alignment.Center
            ) {
                if (!userProfile.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = userProfile.avatarUrl,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initial = userProfile.name.take(1).uppercase()
                    Text(
                        initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                "Your Library",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    isSearching = !isSearching
                    if (!isSearching) searchQuery = ""
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search Library",
                    tint = if (isSearching) GreenPrimary else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = { showCreateBottomSheet = true },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Create Playlist",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // 2. Animated Search Input Bar
        AnimatedVisibility(
            visible = isSearching,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF242424))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Find in Your Library", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Filter Pills Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            if (selectedFilter != "All") {
                item {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2A2A))
                            .clickable { selectedFilter = "All" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear filter",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            val pills = listOf("Playlists", "Artists", "Albums", "Podcasts", "Downloaded")
            items(pills, key = { it }) { pill ->
                val isSelected = selectedFilter == pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) GreenPrimary else Color(0xFF282828))
                        .clickable {
                            selectedFilter = if (isSelected) "All" else pill
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        pill,
                        color = if (isSelected) Color.Black else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 4. Sub-header (Recents Sort + Grid/List Toggle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isSortMenuOpen = true }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Icon(
                    Icons.Filled.SwapVert,
                    contentDescription = "Sort",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    sortOrder,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { isGridView = !isGridView },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Filled.List else Icons.Filled.GridView,
                    contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 5. Library Content Area
        if (displayItems.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        if (selectedFilter == "Downloaded") "No downloaded songs yet" else "Your library is empty",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        if (selectedFilter == "Downloaded")
                            "Tap the download icon on any song or playlist to save it for offline listening."
                        else if (searchQuery.isNotEmpty())
                            "No items found matching \"$searchQuery\""
                        else
                            "Playlists you create or like will show up here.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showCreateBottomSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Create playlist", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { showPlaylistSyncModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SyncBrandLogo(size = 14.dp, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Playlist Sync", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = com.example.ui.LocalBottomContentPadding.current, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayItems, key = { it.id }) { item ->
                    LibraryGridCard(
                        item = item,
                        onClick = {
                            if (item is LibraryDisplayItem.ActionItem) {
                                when (item.actionId) {
                                    "action_playlist_sync", "action_import_music" -> showPlaylistSyncModal = true
                                    "action_add_artists" -> android.widget.Toast.makeText(context, "Search to find artists to follow", android.widget.Toast.LENGTH_SHORT).show()
                                    "action_add_podcasts" -> android.widget.Toast.makeText(context, "Explore podcasts in Search", android.widget.Toast.LENGTH_SHORT).show()
                                    "action_add_events" -> android.widget.Toast.makeText(context, "Events and venues coming soon", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else if (item is LibraryDisplayItem.DownloadedTrackItem) {
                                playerViewModel?.playTrack(item.track, downloadedTracks)
                            } else if (item.targetRoute.startsWith("playlist/")) {
                                onNavigateToPlaylist(item.targetRoute.removePrefix("playlist/"))
                            }
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = com.example.ui.LocalBottomContentPadding.current, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayItems, key = { it.id }) { item ->
                    LibraryListRow(
                        item = item,
                        onClick = {
                            if (item is LibraryDisplayItem.ActionItem) {
                                when (item.actionId) {
                                    "action_playlist_sync", "action_import_music" -> showPlaylistSyncModal = true
                                    "action_add_artists" -> android.widget.Toast.makeText(context, "Search to find artists to follow", android.widget.Toast.LENGTH_SHORT).show()
                                    "action_add_podcasts" -> android.widget.Toast.makeText(context, "Explore podcasts in Search", android.widget.Toast.LENGTH_SHORT).show()
                                    "action_add_events" -> android.widget.Toast.makeText(context, "Events and venues coming soon", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else if (item is LibraryDisplayItem.DownloadedTrackItem) {
                                playerViewModel?.playTrack(item.track, downloadedTracks)
                            } else if (item.targetRoute.startsWith("playlist/")) {
                                onNavigateToPlaylist(item.targetRoute.removePrefix("playlist/"))
                            }
                        }
                    )
                }
            }
        }
    }

    // 6. Sort Bottom Sheet
    if (isSortMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = { isSortMenuOpen = false },
            containerColor = Color(0xFF282828),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    "Sort by",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val sortOptions = listOf("Recents", "Recently Added", "Alphabetical", "Creator")
                sortOptions.forEach { opt ->
                    val isSelected = sortOrder == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                sortOrder = opt
                                isSortMenuOpen = false
                            }
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            opt,
                            color = if (isSelected) GreenPrimary else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Create Bottom Sheet / Dialog
    if (showCreateBottomSheet) {
        CreateBottomSheet(
            onDismissRequest = { showCreateBottomSheet = false },
            onCreatePlaylistClick = {
                showCreateBottomSheet = false
                showCreateDialog = true
            },
            onPlaylistSyncClick = {
                showCreateBottomSheet = false
                showPlaylistSyncModal = true
            },
            onExtractPlaylistClick = {
                showCreateBottomSheet = false
                showPlaylistSyncModal = true
            }
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onPlaylistCreated = { newPlId ->
                showCreateDialog = false
                onNavigateToPlaylist(newPlId)
            }
        )
    }

    // Playlist Sync Modal (Follows precise visual specifications)
    if (showPlaylistSyncModal) {
        PlaylistSyncModal(
            visible = true,
            onDismissRequest = { showPlaylistSyncModal = false },
            onNavigateToPlaylist = { plId ->
                showPlaylistSyncModal = false
                onNavigateToPlaylist(plId)
            },
            onPlaylistCreated = { createdPlaylist ->
                // Playlist created in Library: dismiss modal and navigate to the newly created playlist
                showPlaylistSyncModal = false
                onNavigateToPlaylist(createdPlaylist.id)
            },
            playerViewModel = playerViewModel
        )
    }
}

/**
 * Premium native-looking "Playlist Sync" card matching the existing Library aesthetic.
 */
@Composable
fun PlaylistSyncCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spotifyGreen = Color(0xFF1ED760)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF142017),
                        Color(0xFF19281D),
                        Color(0xFF101712)
                    )
                )
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        spotifyGreen.copy(alpha = 0.45f),
                        Color(0xFF2E4233),
                        Color(0xFF1A271E)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("library_playlist_sync_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Squircle icon with Headphonix logo
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF26342A),
                                Color(0xFF18221B)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF384D3E), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.headphonix_logo),
                    contentDescription = "Headphonix",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Playlist Sync",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(spotifyGreen.copy(alpha = 0.15f))
                            .border(0.5.dp, spotifyGreen.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "SYNC",
                            color = spotifyGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Import Spotify playlists to Headphonix",
                    color = Color(0xFFA2B3A7),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Capsule "Sync" button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF202C23))
                    .border(1.dp, Color(0xFF2E4032), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = spotifyGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sync",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryListRow(
    item: LibraryDisplayItem,
    onClick: () -> Unit
) {
    val likedTracks by PlaylistRepository.likedTracks.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Artwork Box (60x60)
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (item) {
                is LibraryDisplayItem.LikedSongs -> {
                    CollectionArtworkImage(
                        tracks = likedTracks,
                        placeholderIcon = Icons.Filled.Favorite,
                        placeholderBackground = Color(0xFF450AF5),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is LibraryDisplayItem.DownloadedSongs -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E3A8A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Downloaded",
                            tint = GreenPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                is LibraryDisplayItem.UserPlaylistItem -> {
                    CollectionArtworkImage(
                        tracks = item.playlist.tracks,
                        customCoverUrl = item.playlist.coverImage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is LibraryDisplayItem.DownloadedTrackItem -> {
                    val img = item.track.images?.medium ?: item.track.images?.small ?: item.track.images?.large
                    if (!img.isNullOrEmpty()) {
                        AsyncImage(
                            model = img,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF242424)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.DownloadDone,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                is LibraryDisplayItem.ActionItem -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(if (item.isCircle) CircleShape else RoundedCornerShape(8.dp))
                            .background(Color(0xFF282828)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.iconVector,
                            contentDescription = item.title,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isPinned) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = GreenPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    item.subtitle,
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LibraryGridCard(
    item: LibraryDisplayItem,
    onClick: () -> Unit
) {
    val likedTracks by PlaylistRepository.likedTracks.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Square Artwork
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            when (item) {
                is LibraryDisplayItem.LikedSongs -> {
                    CollectionArtworkImage(
                        tracks = likedTracks,
                        placeholderIcon = Icons.Filled.Favorite,
                        placeholderBackground = Color(0xFF450AF5),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is LibraryDisplayItem.DownloadedSongs -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E3A8A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Downloaded",
                            tint = GreenPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                is LibraryDisplayItem.UserPlaylistItem -> {
                    CollectionArtworkImage(
                        tracks = item.playlist.tracks,
                        customCoverUrl = item.playlist.coverImage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is LibraryDisplayItem.DownloadedTrackItem -> {
                    val img = item.track.images?.large ?: item.track.images?.medium
                    if (!img.isNullOrEmpty()) {
                        AsyncImage(
                            model = img,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF242424)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.DownloadDone,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                is LibraryDisplayItem.ActionItem -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(if (item.isCircle) CircleShape else RoundedCornerShape(8.dp))
                            .background(Color(0xFF282828)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.iconVector,
                            contentDescription = item.title,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            item.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.isPinned) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Pinned",
                    tint = GreenPrimary,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                item.subtitle,
                color = Color(0xFFAAAAAA),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
