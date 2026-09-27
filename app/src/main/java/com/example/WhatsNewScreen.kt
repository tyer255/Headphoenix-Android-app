package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class ScreenMockupInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val accentColor: Color,
    val keyFeatures: List<String>,
    val mockupType: MockupType
)

enum class MockupType {
    NOW_PLAYING,
    HOME_FEED,
    SEARCH_EXPLORE,
    SYSTEM_NOTIFICATION,
    LIBRARY_DOWNLOADS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(onBack: () -> Unit) {
    var selectedMockup by remember { mutableStateOf<ScreenMockupInfo?>(null) }
    var selectedCategory by remember { mutableStateOf("All Screens") }

    val mockups = remember {
        listOf(
            ScreenMockupInfo(
                id = "now_playing",
                title = "Now Playing & Lyrics",
                subtitle = "Lossless player with live synchronized lyrics",
                category = "Player",
                icon = Icons.Default.MusicNote,
                accentColor = Color(0xFF1DB954),
                keyFeatures = listOf(
                    "Dynamic artwork gradient background",
                    "Single-line live lyrics preview teaser",
                    "Real-time scrubbing & speed controller",
                    "Integrated Sleep Timer & Device selector"
                ),
                mockupType = MockupType.NOW_PLAYING
            ),
            ScreenMockupInfo(
                id = "system_media",
                title = "Android 13+ Notification",
                subtitle = "System media controls with 4 queue thumbnails",
                category = "System",
                icon = Icons.Default.NotificationsActive,
                accentColor = Color(0xFF3B82F6),
                keyFeatures = listOf(
                    "4 horizontal recommendation thumbnails",
                    "Native MediaLibraryService integration",
                    "Lockscreen & notification quick playback",
                    "Zero-delay instant background audio"
                ),
                mockupType = MockupType.SYSTEM_NOTIFICATION
            ),
            ScreenMockupInfo(
                id = "home_feed",
                title = "Home & Personalized Feed",
                subtitle = "Curated mixes, recent listens & top charts",
                category = "Browse",
                icon = Icons.Default.Home,
                accentColor = Color(0xFF8B5CF6),
                keyFeatures = listOf(
                    "Quick Access 2x3 recent playlist tiles",
                    "Trending & personalized daily mixes",
                    "Artist spotlight and album carousels",
                    "120 FPS buttery smooth scrolling"
                ),
                mockupType = MockupType.HOME_FEED
            ),
            ScreenMockupInfo(
                id = "search_explore",
                title = "Search & Discovery",
                subtitle = "Instant autocomplete & genre browsing",
                category = "Browse",
                icon = Icons.Default.Search,
                accentColor = Color(0xFFF59E0B),
                keyFeatures = listOf(
                    "Live multi-provider song resolver",
                    "Interactive genre category browse cards",
                    "Recent searches with fast clear",
                    "Offline-ready track match indicator"
                ),
                mockupType = MockupType.SEARCH_EXPLORE
            ),
            ScreenMockupInfo(
                id = "library_downloads",
                title = "Library & Offline Mode",
                subtitle = "Downloaded songs, playlists & local tracks",
                category = "Library",
                icon = Icons.Default.DownloadDone,
                accentColor = Color(0xFF10B981),
                keyFeatures = listOf(
                    "True offline local music storage",
                    "Instant play without WiFi or Data",
                    "Playlist builder and song organizer",
                    "Storage management and track cache"
                ),
                mockupType = MockupType.LIBRARY_DOWNLOADS
            )
        )
    }

    val categories = listOf("All Screens", "Player", "System", "Browse", "Library")
    val filteredMockups = remember(selectedCategory, mockups) {
        if (selectedCategory == "All Screens") mockups
        else mockups.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Release Showcase",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "UI Mockups & Current Architecture",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB3B3B3),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = com.example.ui.LocalBottomContentPadding.current + 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Version Banner
            item(span = { GridItemSpan(maxLineSpan) }) {
                ReleaseHeaderCard()
            }

            // Category Filter Chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E1E22),
                                labelColor = Color(0xFFB3B3B3),
                                selectedContainerColor = Color(0xFF2E2E34),
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFF333338),
                                selectedBorderColor = Color(0xFF1DB954)
                            )
                        )
                    }
                }
            }

            // Section Title
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Screen Mockups",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${filteredMockups.size} components",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888)
                    )
                }
            }

            // Screen Mockup Cards Grid
            items(filteredMockups, key = { it.id }) { mockup ->
                ScreenMockupCard(
                    mockup = mockup,
                    onClick = { selectedMockup = mockup }
                )
            }
        }
    }

    // Detail Dialog when a mockup is clicked
    selectedMockup?.let { item ->
        MockupDetailDialog(
            mockup = item,
            onDismiss = { selectedMockup = null }
        )
    }
}

@Composable
fun ReleaseHeaderCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("release_header_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        border = BorderStroke(1.dp, Color(0xFF28282E))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1DB954).copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color(0xFF1DB954).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "LATEST PRODUCTION BUILD",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF1DB954),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "v2.4.0 • 120 FPS",
                    color = Color(0xFF888888),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Modern Architecture & Android 13+ Media Integration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Engineered with native Media3 MediaLibraryService, offline audio caching, real-time synced lyrics, and high-performance Jetpack Compose UI.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB3B3B3),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ScreenMockupCard(
    mockup: ScreenMockupInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("mockup_card_${mockup.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
        border = BorderStroke(1.dp, Color(0xFF28282E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Realistic Mobile Device Mockup Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E0E10))
                    .border(1.dp, Color(0xFF2A2A30), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Miniature Android Status Bar
                    MiniStatusBar()

                    // Screen Content Mockup
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (mockup.mockupType) {
                            MockupType.NOW_PLAYING -> MiniPlayerScreenMockup()
                            MockupType.SYSTEM_NOTIFICATION -> MiniSystemNotificationMockup()
                            MockupType.HOME_FEED -> MiniHomeScreenMockup()
                            MockupType.SEARCH_EXPLORE -> MiniSearchScreenMockup()
                            MockupType.LIBRARY_DOWNLOADS -> MiniLibraryScreenMockup()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Text Info & Features
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mockup.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = mockup.accentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = mockup.category,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = mockup.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = mockup.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF999999),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Features Checklist
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                mockup.keyFeatures.take(2).forEach { feat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(mockup.accentColor)
                        )
                        Text(
                            text = feat,
                            color = Color(0xFFB3B3B3),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0C))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("9:41", color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF1DB954)))
            Text("5G", color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp)
            Text("85%", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp)
        }
    }
}

@Composable
fun MiniPlayerScreenMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A332B),
                        Color(0xFF121214),
                        Color(0xFF0F0F11)
                    )
                )
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top mini nav
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text("PLAYING FROM PLAYLIST", color = Color(0xFFB3B3B3), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }

        // Center Album Art Artwork
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF28282E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Album, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(34.dp))
        }

        // Title and Like
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Lagda Nahi", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Ammy Gill", color = Color(0xFF999999), fontSize = 9.sp)
            }
            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(14.dp))
        }

        // Mini Smooth Progress Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF333338))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.45f)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1:12", color = Color(0xFF777777), fontSize = 7.sp)
                Text("3:24", color = Color(0xFF777777), fontSize = 7.sp)
            }
        }

        // Mini Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(12.dp))
            Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pause, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            }
            Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Icon(Icons.Default.Repeat, contentDescription = null, tint = Color(0xFF888888), modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun MiniSystemNotificationMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF16181B))
            .padding(8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Android 13 Media Notification Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF22262B)),
            border = BorderStroke(1.dp, Color(0xFF333842))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Top Row: Artwork + Track Info + App Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2E3440)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lagda Nahi", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("Ammy Gill • Recommended", color = Color(0xFF94A3B8), fontSize = 8.sp)
                    }
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFF3B4252))
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.35f).fillMaxHeight().background(Color(0xFF3B82F6)))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Devices, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                    Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    }
                    Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // The 4 Upcoming Recommendation Thumbnails Row!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(4) { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E3440))
                                .border(1.dp, Color(0xFF3E4656), RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (idx) {
                                    0 -> Icons.Default.QueueMusic
                                    1 -> Icons.Default.Radio
                                    2 -> Icons.Default.Audiotrack
                                    else -> Icons.Default.Headphones
                                },
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniHomeScreenMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Good evening", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }

        // 2x2 Quick Access Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF222226))
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF5B21B6)))
                    Text("Liked Songs", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF222226))
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1E3A8A)))
                    Text("Daily Mix 1", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text("Recently Played", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)

        // Carousel items row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF24242A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Album, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Top Hits", color = Color.White, fontSize = 7.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun MiniSearchScreenMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Search", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        // Search Bar Mockup
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
            Text("What do you want to listen to?", color = Color(0xFF666666), fontSize = 8.sp)
        }

        Text("Browse all", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)

        // 2x2 Genre Cards Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFDC2626))
                    .padding(6.dp)
            ) {
                Text("Podcasts", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2563EB))
                    .padding(6.dp)
            ) {
                Text("Made For You", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF059669))
                    .padding(6.dp)
            ) {
                Text("Charts", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFD97706))
                    .padding(6.dp)
            ) {
                Text("Bollywood", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MiniLibraryScreenMockup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Library", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF28282E))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Playlists", color = Color.White, fontSize = 7.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("Downloaded", color = Color(0xFF10B981), fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Track items list
        repeat(3) { idx ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (idx == 0) Color(0xFF10B981) else Color(0xFF28282E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (idx == 0) Icons.Default.Favorite else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (idx) {
                            0 -> "Liked Songs"
                            1 -> "Offline Punjabi Hits"
                            else -> "Chill Lofi Beats"
                        },
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Playlist • Downloaded", color = Color(0xFF888888), fontSize = 7.sp)
                }
                Icon(Icons.Default.DownloadDone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
fun MockupDetailDialog(
    mockup: ScreenMockupInfo,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
            border = BorderStroke(1.dp, Color(0xFF33333A))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = mockup.accentColor.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = mockup.icon,
                                contentDescription = null,
                                tint = mockup.accentColor,
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = mockup.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = mockup.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = mockup.accentColor,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF888888))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = mockup.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCCCCCC)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Key Architectural Capabilities",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    mockup.keyFeatures.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = mockup.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = feature,
                                color = Color(0xFFB3B3B3),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282830))
                ) {
                    Text("Close Preview", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
