package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.data.PlaylistRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    // Force preview refresh
    navController: NavHostController = rememberNavController(),
    playerViewModel: PlayerViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showCreateSheet by remember { mutableStateOf(false) }
    var showPlaylistSyncModal by remember { mutableStateOf(false) }
    var showFullScreenPlayer by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)


    var globalTrackMenuFor by remember { mutableStateOf<TrackMenuState?>(null) }
    var globalAddToPlaylistFor by remember { mutableStateOf<com.example.data.remote.models.TrackDto?>(null) }
    var globalCreatePlaylistFor by remember { mutableStateOf<com.example.data.remote.models.TrackDto?>(null) }
    var globalShareTrackFor by remember { mutableStateOf<com.example.data.remote.models.TrackDto?>(null) }
    var showCreatePlaylistSheetOnly by remember { mutableStateOf(false) }
    
    val savedTracks by playerViewModel.savedTracks.collectAsState()
    val playlists by PlaylistRepository.playlists.collectAsState()
    val downloadedTracks by com.example.data.AppDownloadManager.downloadedTracks.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()

    val hideBottomBarRoutes = listOf(
        "add_tracks",
        "edit_playlist",
        "playlist_extractor",
        "settings",
        "share_landing"
    )
    val shouldShowBottomBar = currentRoute == null || hideBottomBarRoutes.none { currentRoute.startsWith(it) }
    val bottomContentPadding = com.example.ui.calculateBottomContentPadding(
        shouldShowBottomBar = shouldShowBottomBar,
        hasMiniPlayer = currentTrack != null
    )

    ModalNavigationDrawer(

        drawerState = drawerState,
        drawerContent = {
            WebsiteSidebar(
                currentRoute = currentRoute,
                playlists = playlists,
                likedTracksCount = savedTracks.size,
                downloadedTracksCount = downloadedTracks.size,
                onNavigate = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onOpenCreatePlaylist = {
                    showCreatePlaylistSheetOnly = true
                },
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {

        CompositionLocalProvider(
            LocalTrackMenuProvider provides { state -> globalTrackMenuFor = state },
            LocalShareProvider provides { track -> globalShareTrackFor = track },
            com.example.ui.LocalBottomContentPadding provides bottomContentPadding
        ) {
            Scaffold(

            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).imePadding()) {
                NavHost(
                    navController = navController, 
                    startDestination = "home",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { (it * 0.12f).toInt() },
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
                        )
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -(it * 0.08f).toInt() },
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing)
                        )
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -(it * 0.08f).toInt() },
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
                        )
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { (it * 0.12f).toInt() },
                            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing)
                        )
                    }
                ) {
                    composable("home") {
                        HomeScreen(
                            viewModel = homeViewModel,
                            playerViewModel = playerViewModel,
                            onNavigateToPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") },
                            onNavigateToArtist = { artistId -> navController.navigate("artist/$artistId") },
                            onNavigateToAlbum = { albumId -> navController.navigate("album/$albumId") },
                            onNavigateToSearch = { navController.navigate("search") },
                            trackMenu = { globalTrackMenuFor = it },
                            openProfileDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            playerViewModel = playerViewModel,
                            onNavigateToArtist = { artistId -> navController.navigate("artist/$artistId") },
                            onNavigateToPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") },
                            onNavigateToAlbum = { albumId -> navController.navigate("album/$albumId") },
                            onOpenProfile = { scope.launch { drawerState.open() } }
                        )
                    }
                    composable("library") {
                        LibraryScreen(
                            onNavigateToPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") },
                            onOpenProfile = { scope.launch { drawerState.open() } },
                            onNavigateToExtractor = { navController.navigate("playlist-extractor") },
                            playerViewModel = playerViewModel
                        )
                    }
                composable("premium") {
                    PremiumScreen()
                }
                composable("profile") {
                    ProfileScreen(onBack = { navController.popBackStack() })
                }
                composable("account") {
                    AccountScreen(onBack = { navController.popBackStack() })
                }
                composable("whatsnew") {
                    WhatsNewScreen(onBack = { navController.popBackStack() })
                }
                composable("history") {
                    HistoryScreen(onBack = { navController.popBackStack() })
                }
                composable("settings") {
                    SettingsScreen(onNavigateBack = { navController.popBackStack() })
                }
                composable("radio") {
                    RadioHubScreen(
                        onNavigateToRadioStation = { id, title -> navController.navigate("radio/$id/$title") }
                    )
                }
                composable("radio/{id}/{title}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: ""
                    RadioStationScreen(
                        stationId = id,
                        stationTitle = title,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSearch = { navController.navigate("search") },
                        playerViewModel = playerViewModel
                    )
                }
                composable("blend-invite/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    BlendInviteScreen(
                        blendId = id,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") {
                            popUpTo("home")
                        } }
                    )
                }
                composable("blend-setup") {
                    BlendSetupScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") {
                            popUpTo("home")
                        } }
                    )
                }
                composable("share/{type}/{id}") { backStackEntry ->
                    val type = backStackEntry.arguments?.getString("type") ?: ""
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    ShareLandingScreen(
                        shareType = type,
                        shareId = id,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToEntity = { t, i -> navController.navigate("$t/$i") {
                            popUpTo("home")
                        } }
                    )
                }
                composable("artist/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    ArtistScreen(
                        artistId = id,
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("album/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    AlbumScreen(
                        albumId = id,
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("playlist/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    PlaylistScreen(
                        playlistId = id,
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToArtist = { artistId -> navController.navigate("artist/$artistId") }
                    )
                }
                composable("playlist-extractor") {
                    PlaylistExtractorScreen(
                        playerViewModel = playerViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlaylist = { playlistId ->
                            navController.navigate("playlist/$playlistId")
                        }
                    )
                }
            }

            if (shouldShowBottomBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    val currentTrack by playerViewModel.currentTrack.collectAsState()
                    
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x00121214),
                                        Color(0xE6121214),
                                        Color(0xFF0C0C0E)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (currentTrack != null) {
                            MiniPlayer(
                                viewModel = playerViewModel,
                                onOpenFullScreen = { showFullScreenPlayer = true }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF101014))
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            BottomNavigationBar(
                                currentRoute = currentRoute ?: "home",
                                isCreateSheetOpen = showCreateSheet,
                                onNavigate = { route ->
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                onCreateClick = { showCreateSheet = true }
                            )
                        }
                    }
                }
            }
        }


        } // CompositionLocalProvider end

        if (showCreateSheet) {
            CreateBottomSheet(
                onDismissRequest = { showCreateSheet = false },
                onCreatePlaylistClick = {
                    showCreateSheet = false
                    showCreatePlaylistSheetOnly = true
                },
                onPlaylistSyncClick = {
                    showCreateSheet = false
                    showPlaylistSyncModal = true
                },
                onExtractPlaylistClick = {
                    showCreateSheet = false
                    showPlaylistSyncModal = true
                }
            )
        }

        if (showPlaylistSyncModal) {
            PlaylistSyncModal(
                onDismissRequest = { showPlaylistSyncModal = false },
                onNavigateToPlaylist = { playlistId ->
                    showPlaylistSyncModal = false
                    navController.navigate("playlist/$playlistId") {
                        launchSingleTop = true
                    }
                },
                playerViewModel = playerViewModel
            )
        }


        AnimatedVisibility(
            visible = showFullScreenPlayer,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeIn(animationSpec = tween(250)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            FullPlayerScreen(
                viewModel = playerViewModel, 
                onClose = { showFullScreenPlayer = false },
                onNavigateToArtist = { artistId ->
                    showFullScreenPlayer = false
                    navController.navigate("artist/$artistId")
                }
            )
        }

        if (globalTrackMenuFor != null) {
            TrackOptionsSheet(
                track = globalTrackMenuFor!!.track,
                isSaved = savedTracks.contains(globalTrackMenuFor!!.track.id),
                onDismiss = { globalTrackMenuFor = null },
                onToggleSave = { PlaylistRepository.toggleLikeTrack(globalTrackMenuFor!!.track) },
                onAddToPlaylist = { globalAddToPlaylistFor = globalTrackMenuFor!!.track; globalTrackMenuFor = null },
                onAddToQueue = { playerViewModel.addToQueue(globalTrackMenuFor!!.track); globalTrackMenuFor = null },
                onViewArtist = { artistId -> 
                    globalTrackMenuFor = null
                    navController.navigate("artist/$artistId")
                },
                onViewAlbum = { albumId -> 
                    globalTrackMenuFor = null
                    navController.navigate("album/$albumId")
                },
                onRemoveFromPlaylist = globalTrackMenuFor!!.onRemoveFromPlaylist
            )
        }

        if (globalShareTrackFor != null) {
            com.example.ui.TrackShareBottomSheet(
                track = globalShareTrackFor!!,
                onDismissRequest = { globalShareTrackFor = null }
            )
        }

        if (globalAddToPlaylistFor != null) {
            AddToPlaylistSheet(
                track = globalAddToPlaylistFor!!,
                playlists = playlists,
                onDismiss = { globalAddToPlaylistFor = null },
                onPlaylistSelected = { pl ->
                    PlaylistRepository.addTrackToPlaylist(pl.id, globalAddToPlaylistFor!!)
                    globalAddToPlaylistFor = null
                },
                onCreateNewPlaylist = {
                    globalCreatePlaylistFor = globalAddToPlaylistFor
                    globalAddToPlaylistFor = null
                }
            )
        }

        if (globalCreatePlaylistFor != null || showCreatePlaylistSheetOnly) {
            CreatePlaylistDialog(
                onDismiss = { globalCreatePlaylistFor = null; showCreatePlaylistSheetOnly = false },
                onPlaylistCreated = { newPlId ->
                    if (globalCreatePlaylistFor != null) {
                        PlaylistRepository.addTrackToPlaylist(newPlId, globalCreatePlaylistFor!!)
                    }
                    globalCreatePlaylistFor = null
                    showCreatePlaylistSheetOnly = false
                    navController.navigate("playlist/$newPlId")
                }
            )
        }
    }
}
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    isCreateSheetOpen: Boolean,
    onNavigate: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    val createRotation by animateFloatAsState(
        targetValue = if (isCreateSheetOpen) 45f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    Surface(
        color = Color(0xFF0D0D0F),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                selected = currentRoute == "home",
                onClick = { onNavigate("home") }
            )
            NavItem(
                icon = Icons.Outlined.Search,
                label = "Search",
                selected = currentRoute == "search",
                onClick = { onNavigate("search") }
            )
            NavItem(
                iconId = R.drawable.ic_your_library,
                label = "Your Library",
                selected = currentRoute == "library",
                onClick = { onNavigate("library") }
            )
            NavItem(
                iconId = R.drawable.ic_spotify,
                label = "Premium",
                selected = currentRoute == "premium",
                customIconTint = Color(0xFF1ED760),
                onClick = { onNavigate("premium") }
            )
            NavItem(
                icon = Icons.Default.Add,
                label = "Create",
                selected = false,
                rotation = createRotation,
                onClick = onCreateClick
            )
        }
    }
}

@Composable
fun RowScope.NavItem(
    icon: ImageVector? = null,
    iconId: Int? = null,
    label: String,
    selected: Boolean,
    rotation: Float = 0f,
    customIconTint: Color? = null,
    onClick: () -> Unit
) {
    val textColor = if (selected) Color.White else Color(0xFF8E8E93)
    val iconColor = customIconTint ?: if (selected) Color.White else Color(0xFF8E8E93)
    Column(
        modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp).rotate(rotation)
            )
        } else if (iconId != null) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp).rotate(rotation)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
