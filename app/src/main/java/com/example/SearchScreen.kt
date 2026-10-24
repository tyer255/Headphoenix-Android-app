package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.remote.models.AlbumDto
import com.example.data.remote.models.ArtistDto
import com.example.data.remote.models.PlaylistDto
import com.example.data.remote.models.TrackDto
import com.example.ui.CollectionArtworkImage
import com.example.ui.FavoriteHeartButton
import com.example.ui.FavoriteRed
import com.example.ui.SkeletonBox
import com.example.ui.SkeletonSearchScreen
import com.example.ui.SpotifyCirclePlayButton
import com.example.ui.SpotifyEqualizerIcon
import com.example.ui.theme.GreenPrimary
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale

// Rich Category Card definition with dual gradient stops
data class SpotifyCategoryCard(
    val id: String,
    val title: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val image: String,
    val query: String
)

private val SPOTIFY_BROWSE_ALL_CATEGORIES = listOf(
    SpotifyCategoryCard(
        "music",
        "Music",
        Color(0xFFE13300),
        Color(0xFF801A00),
        "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=350&auto=format&fit=crop&q=80",
        "Top Bollywood Songs"
    ),
    SpotifyCategoryCard(
        "podcasts",
        "Podcasts",
        Color(0xFF006450),
        Color(0xFF00332A),
        "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?w=350&auto=format&fit=crop&q=80",
        "Top Hindi Podcasts"
    ),
    SpotifyCategoryCard(
        "live_events",
        "Live Events",
        Color(0xFF8400E7),
        Color(0xFF45007A),
        "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=350&auto=format&fit=crop&q=80",
        "Concerts & Live Music"
    ),
    SpotifyCategoryCard(
        "ipop",
        "Home of I-Pop",
        Color(0xFF1E3264),
        Color(0xFF0E1833),
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=350&auto=format&fit=crop&q=80",
        "I-Pop Hits"
    ),
    SpotifyCategoryCard(
        "punjabi",
        "Punjabi Pop",
        Color(0xFFF59B23),
        Color(0xFF8C5209),
        "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=350&auto=format&fit=crop&q=80",
        "Punjabi Top Hits"
    ),
    SpotifyCategoryCard(
        "lofi",
        "Chill & Lofi",
        Color(0xFF608108),
        Color(0xFF2E3E04),
        "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=350&auto=format&fit=crop&q=80",
        "Lofi Chill Beats"
    ),
    SpotifyCategoryCard(
        "hiphop",
        "Desi Hip-Hop",
        Color(0xFFBA5D07),
        Color(0xFF5E2E03),
        "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=350&auto=format&fit=crop&q=80",
        "Desi Hip-Hop"
    ),
    SpotifyCategoryCard(
        "romantic",
        "Love & Romance",
        Color(0xFFE91429),
        Color(0xFF750A14),
        "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=350&auto=format&fit=crop&q=80",
        "Romantic Hindi Hits"
    ),
    SpotifyCategoryCard(
        "coke_studio",
        "Coke Studio",
        Color(0xFFD84000),
        Color(0xFF691F00),
        "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=350&auto=format&fit=crop&q=80",
        "Coke Studio Bharat"
    ),
    SpotifyCategoryCard(
        "devotional",
        "Sufi & Devotional",
        Color(0xFF503750),
        Color(0xFF251A25),
        "https://images.unsplash.com/photo-1528728329032-2972f65dfb3f?w=350&auto=format&fit=crop&q=80",
        "Sufi Hits"
    ),
    SpotifyCategoryCard(
        "party",
        "Party & Dance",
        Color(0xFFE8115B),
        Color(0xFF73082D),
        "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=350&auto=format&fit=crop&q=80",
        "Bollywood Party Club"
    ),
    SpotifyCategoryCard(
        "workout",
        "Workout & Gym",
        Color(0xFF777777),
        Color(0xFF333333),
        "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=350&auto=format&fit=crop&q=80",
        "Gym Motivation Beats"
    )
)

data class SpotifyLanguageItem(
    val name: String,
    val native: String,
    val category: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val query: String,
    val desc: String
)

private val ALL_SPOTIFY_LANGUAGES = listOf(
    SpotifyLanguageItem("Hindi", "हिन्दी", "Indian", Color(0xFFE13300), Color(0xFF751A00), "Hindi Top Songs", "Bollywood & Indie Hits"),
    SpotifyLanguageItem("Punjabi", "ਪੰਜਾਬੀ", "Indian", Color(0xFFF59B23), Color(0xFF804F0E), "Punjabi Top 50", "Desi Beats & Bhangra"),
    SpotifyLanguageItem("Tamil", "தமிழ்", "Indian", Color(0xFFE91429), Color(0xFF780A15), "Tamil Top Hits", "Kollywood Melodies"),
    SpotifyLanguageItem("Telugu", "తెలుగు", "Indian", Color(0xFF1E3264), Color(0xFF0F1B38), "Telugu Super Hits", "Tollywood Mass"),
    SpotifyLanguageItem("Bhojpuri", "भोजपुरी", "Indian", Color(0xFFB02897), Color(0xFF5A144D), "Bhojpuri Hits", "Desi Tadka"),
    SpotifyLanguageItem("Malayalam", "മലയാളം", "Indian", Color(0xFF283EA3), Color(0xFF142057), "Malayalam Hits", "Mollywood Vibes"),
    SpotifyLanguageItem("Kannada", "ಕನ್ನಡ", "Indian", Color(0xFFD84000), Color(0xFF6B2000), "Kannada Hits", "Sandalwood Tracks"),
    SpotifyLanguageItem("Marathi", "मराठी", "Indian", Color(0xFF503750), Color(0xFF281C28), "Marathi Songs", "Lavani & Modern"),
    SpotifyLanguageItem("Gujarati", "ગુજરાતી", "Indian", Color(0xFF148A08), Color(0xFF0A4704), "Gujarati Hits", "Garba & Folk"),
    SpotifyLanguageItem("Bengali", "বাংলা", "Indian", Color(0xFF477D95), Color(0xFF233E4A), "Bengali Hits", "Melody & Sangeet"),
    SpotifyLanguageItem("Haryanvi", "हरियाणवी", "Indian", Color(0xFFE91429), Color(0xFF780A15), "Haryanvi Songs", "Desi Raginis"),
    SpotifyLanguageItem("Korean", "한국어", "Global", Color(0xFFEB1E32), Color(0xFF730C16), "K-Pop Top Hits", "K-Pop & OSTs"),
    SpotifyLanguageItem("Japanese", "日本語", "Global", Color(0xFF904050), Color(0xFF451E26), "J-Pop Anime Hits", "J-Pop & Soundtracks"),
    SpotifyLanguageItem("English", "English", "Global", Color(0xFF2D46B9), Color(0xFF15225E), "English Pop Hits", "Global Top Charts"),
    SpotifyLanguageItem("Spanish", "Español", "Global", Color(0xFFE13300), Color(0xFF731A00), "Reggaeton Latin Hits", "Reggaeton & Latin Pop"),
    SpotifyLanguageItem("French", "Français", "Global", Color(0xFF503750), Color(0xFF281C28), "French Pop Hits", "French Pop & Chanson"),
    SpotifyLanguageItem("Arabic", "العربية", "Global", Color(0xFF006450), Color(0xFF003328), "Arabic Pop Hits", "Khaleeji & Middle East"),
    SpotifyLanguageItem("Brazilian", "Português", "Global", Color(0xFF148A08), Color(0xFF0A4704), "Brazilian Funk Hits", "Brazilian Funk & Samba")
)

data class RealDiscoverCard(
    val id: String,
    val tag: String,
    val title: String,
    val artist: String,
    val image: String,
    val track: TrackDto?,
    val query: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    playerViewModel: PlayerViewModel,
    initialQuery: String? = null,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val searchViewModel: SearchViewModel = viewModel()
    val focusManager = LocalFocusManager.current

    val query by searchViewModel.searchQuery.collectAsState()
    val suggestions by searchViewModel.searchSuggestions.collectAsState()
    val tracks by searchViewModel.searchTracks.collectAsState()
    val artists by searchViewModel.searchArtists.collectAsState()
    val albums by searchViewModel.searchAlbums.collectAsState()
    val playlists by searchViewModel.searchPlaylists.collectAsState()
    val searchIntent by searchViewModel.searchIntent.collectAsState()
    val isSearchLoading by searchViewModel.isLoading.collectAsState()

    val currentPlayingTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val savedTracks by playerViewModel.savedTracks.collectAsState()
    val trackMenu = LocalTrackMenuProvider.current

    var selectedFilter by remember { mutableStateOf("All") }
    var languageTab by remember { mutableStateOf("All") }
    var isSubmitted by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showScannerModal by remember { mutableStateOf(false) }

    var discoverCards by remember { mutableStateOf<List<RealDiscoverCard>>(emptyList()) }
    var discoverLoading by remember { mutableStateOf(true) }

    // Recent search history with rich metadata (matching Spotify Android Recent Searches rows)
    var recentSearches by remember {
        mutableStateOf(
            listOf(
                "Anuv Jain",
                "DOPAMINE",
                "Arijit Singh",
                "Karan Aujla",
                "Husn",
                "Tauba Tauba",
                "Coke Studio"
            )
        )
    }

    androidx.activity.compose.BackHandler(enabled = isSearchActive || query.isNotEmpty()) {
        if (query.isNotEmpty()) {
            searchViewModel.updateQuery("")
            isSubmitted = false
        } else {
            isSearchActive = false
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        try {
            val repo = com.example.data.MusicRepository()
            val feed = repo.getHome()
            val realTracks = mutableListOf<TrackDto>()
            feed?.quickPicks?.let { realTracks.addAll(it) }
            feed?.trending?.let { realTracks.addAll(it) }
            feed?.popularSongs?.let { realTracks.addAll(it) }
            feed?.madeForYou?.let { realTracks.addAll(it) }

            if (realTracks.size < 6) {
                val searchRes = repo.search("Arz Kiya Hai Anuv Jain")
                searchRes?.songs?.let { realTracks.addAll(it) }
            }

            val tags = listOf("#hindi acoustic", "#trending pop", "#heartbreak", "#punjabi drill", "#soulful", "#indie vibe")
            val queries = listOf("Arz Kiya Hai", "Husn", "Pal Pal Dil Ke Paas", "Tauba Tauba", "Kesariya", "Wavy")

            val cards = mutableListOf<RealDiscoverCard>()
            for (i in 0 until 6.coerceAtMost(realTracks.size.coerceAtLeast(1))) {
                val trk = realTracks.getOrNull(i)
                val img = trk?.images?.large ?: trk?.images?.medium ?: trk?.images?.small
                    ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                cards.add(
                    RealDiscoverCard(
                        id = trk?.id ?: "disc-$i",
                        tag = tags[i % tags.size],
                        title = trk?.title ?: "Trending Song",
                        artist = trk?.artist ?: "Popular Artist",
                        image = img,
                        track = trk,
                        query = queries[i % queries.size]
                    )
                )
            }
            discoverCards = cards
        } catch (e: Exception) {
            // Offline fallback
        } finally {
            discoverLoading = false
        }
    }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            searchViewModel.performSearchNow(initialQuery)
            isSubmitted = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
    ) {
        // 1. Header (Hidden when in full-screen Active Search mode, just like Spotify Android)
        AnimatedVisibility(
            visible = !isSearchActive && query.isEmpty(),
            enter = fadeIn(tween(180)) + expandVertically(tween(180)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1ED760), Color(0xFF159643))
                                )
                            )
                            .clickable { onOpenProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.headphonix_logo),
                            contentDescription = "Headphonix",
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Spotify Android Camera / Code Scanner icon in top-right of Search
                IconButton(
                    onClick = { showScannerModal = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = "Scan Code",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. Official Spotify Search Input Box (White bar on Browse home; Dark #242424 header bar in Active Search mode)
        val isDarkSearchHeader = isSearchActive || query.isNotEmpty()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isDarkSearchHeader) 0.dp else 16.dp,
                    vertical = if (isDarkSearchHeader) 0.dp else 6.dp
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isDarkSearchHeader) 56.dp else 48.dp)
                    .clip(RoundedCornerShape(if (isDarkSearchHeader) 0.dp else 8.dp))
                    .background(if (isDarkSearchHeader) Color(0xFF242424) else Color.White)
                    .clickable(
                        enabled = !isDarkSearchHeader
                    ) {
                        isSearchActive = true
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDarkSearchHeader) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                searchViewModel.updateQuery("")
                                isSubmitted = false
                                isSearchActive = false
                                focusManager.clearFocus()
                            }
                    )
                } else {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF121212),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                BasicTextField(
                    value = query,
                    onValueChange = { newQuery ->
                        isSearchActive = true
                        searchViewModel.updateQuery(newQuery)
                        isSubmitted = false
                    },
                    textStyle = TextStyle(
                        color = if (isDarkSearchHeader) Color.White else Color(0xFF121212),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF1ED760)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.trim().isNotEmpty()) {
                                searchViewModel.performSearchNow(query.trim())
                                isSubmitted = true
                                focusManager.clearFocus()
                                if (!recentSearches.contains(query.trim())) {
                                    recentSearches = (listOf(query.trim()) + recentSearches).take(10)
                                }
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                "What do you want to listen to?",
                                color = if (isDarkSearchHeader) Color(0xFFB3B3B3) else Color(0xFF535353),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchViewModel.updateQuery("")
                            isSubmitted = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear text",
                            tint = if (isDarkSearchHeader) Color.White else Color(0xFF333333),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 3. Spotify Filter Chips (Shown when user types or submits search)
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterList = listOf("All", "Songs", "Artists", "Playlists", "Albums", "Podcasts & Shows", "Profiles")
                items(filterList, key = { it }) { filter ->
                    val displayLabel = if (filter == "All") "Top" else filter
                    val isSelected = selectedFilter == filter
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF1ED760) else Color(0xFF2A2A2A),
                        animationSpec = tween(180),
                        label = "filterChipBg"
                    )
                    val chipScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.03f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "filterChipScale"
                    )
                    Box(
                        modifier = Modifier
                            .scale(chipScale)
                            .clip(RoundedCornerShape(50))
                            .background(chipBg)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = displayLabel,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 4. Content Area
        if (isSearchActive && query.isEmpty()) {
            // Authentic Spotify Android Active Search State: Vertical Recent Searches List with 48dp Thumbnails
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF121212))
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = com.example.ui.LocalBottomContentPadding.current)
            ) {
                item {
                    Text(
                        text = "Recent searches",
                        color = Color.White,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (recentSearches.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Play what you love",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Search for artists, songs, podcasts, and more.",
                                    color = Color(0xFFB3B3B3),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(recentSearches, key = { "active_recent_$it" }) { item ->
                        val isArtistItem = item.equals("Anuv Jain", true) ||
                            item.equals("Arijit Singh", true) ||
                            item.equals("Karan Aujla", true)
                        val itemSubtitle = when {
                            isArtistItem -> "Artist"
                            item.equals("DOPAMINE", true) -> "Song • Guru Randhawa"
                            item.equals("Husn", true) -> "Song • Anuv Jain"
                            item.equals("Tauba Tauba", true) -> "Song • Karan Aujla"
                            else -> "Playlist • Headphonix"
                        }
                        val itemThumb = when {
                            item.equals("Anuv Jain", true) || item.equals("Husn", true) ->
                                "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/250x250-000000-80-0-0.jpg"
                            item.equals("Karan Aujla", true) || item.equals("Tauba Tauba", true) ->
                                "https://cdn-images.dzcdn.net/images/cover/ff6bb1420d9fcd2671cf6f86c2e49658/250x250-000000-80-0-0.jpg"
                            item.equals("DOPAMINE", true) ->
                                "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=300&auto=format&fit=crop&q=80"
                            else ->
                                "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/250x250-000000-80-0-0.jpg"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    searchViewModel.performSearchNow(item)
                                    isSubmitted = true
                                    focusManager.clearFocus()
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = itemThumb,
                                contentDescription = item,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(if (isArtistItem) CircleShape else RoundedCornerShape(4.dp))
                                    .background(Color(0xFF282828))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = itemSubtitle,
                                    color = Color(0xFFB3B3B3),
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = {
                                    recentSearches = recentSearches.filter { it != item }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove $item from history",
                                    tint = Color(0xFFB3B3B3),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { recentSearches = emptyList() },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color(0xFF535353)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Clear recent searches",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else if (query.isEmpty()) {
            // Enhanced Spotify Browse & Discovery Grid
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
            ) {
                // Section: Recent Searches (`SearchPage.scss` with per-item `.item-delete-button`)
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent searches",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Clear recent searches",
                                    color = Color(0xFFB3B3B3),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { recentSearches = emptyList() }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(recentSearches, key = { "recent_$it" }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF282828))
                                            .clickable {
                                                searchViewModel.performSearchNow(item)
                                                isSubmitted = true
                                            }
                                            .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.History,
                                            contentDescription = null,
                                            tint = Color(0xFFB3B3B3),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item,
                                            color = Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.35f))
                                                .clickable {
                                                    recentSearches = recentSearches.filter { it != item }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Remove $item",
                                                tint = Color(0xFFB3B3B3),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Personalized Listening Taste
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 14.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF14261B), Color(0xFF121B16))
                                )
                            )
                            .border(1.dp, Color(0x261ED760), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Headphones,
                                contentDescription = null,
                                tint = Color(0xFF1ED760),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Based on your music taste",
                                color = Color(0xFF1ED760),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val personalizedChips = listOf(
                                "Anuv Jain",
                                "Arijit Singh",
                                "Karan Aujla",
                                "KR\$NA",
                                "Soulful Acoustic",
                                "Late Night Indie",
                                "Coke Studio Bharat",
                                "Desi Lofi"
                            )
                            items(personalizedChips, key = { "chip_$it" }) { chip ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF1E2D23))
                                        .border(1.dp, Color(0x401ED760), RoundedCornerShape(50))
                                    .clickable {
                                        searchViewModel.performSearchNow(chip)
                                        isSubmitted = true
                                    }
                                    .padding(horizontal = 13.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = chip,
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: Start Browsing (Official Spotify Android 2x2 Compact Top Section)
                item {
                    Text(
                        text = "Start browsing",
                        color = Color.White,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                    )
                    val startBrowsingItems = SPOTIFY_BROWSE_ALL_CATEGORIES.take(4).chunked(2)
                    startBrowsingItems.forEach { rowPair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPair.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(cat.primaryColor)
                                        .clickable {
                                            searchViewModel.performSearchNow(cat.query)
                                            isSubmitted = true
                                        }
                                ) {
                                    Text(
                                        text = cat.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .align(Alignment.TopStart)
                                            .fillMaxWidth(0.68f)
                                    )
                                    AsyncImage(
                                        model = cat.image,
                                        contentDescription = cat.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 10.dp, y = 6.dp)
                                            .rotate(25f)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                // Section: Browse All (Official Spotify 2-Column Angled Card Grid)
                item {
                    Text(
                        text = "Browse all",
                        color = Color.White,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )
                }

                val categoryRows = SPOTIFY_BROWSE_ALL_CATEGORIES.chunked(2)
                items(categoryRows, key = { it.firstOrNull()?.title ?: "" }) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { cat ->
                            val cardInteraction = remember { MutableInteractionSource() }
                            val isCardPressed by cardInteraction.collectIsPressedAsState()
                            val cardScale by animateFloatAsState(
                                targetValue = if (isCardPressed) 0.96f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "browseCardScale"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .scale(cardScale)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cat.primaryColor)
                                    .clickable(
                                        interactionSource = cardInteraction,
                                        indication = null
                                    ) {
                                        searchViewModel.performSearchNow(cat.query)
                                        isSubmitted = true
                                    }
                            ) {
                                Text(
                                    text = cat.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier
                                        .padding(14.dp)
                                        .align(Alignment.TopStart)
                                        .fillMaxWidth(0.68f)
                                )
                                AsyncImage(
                                    model = cat.image,
                                    contentDescription = cat.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 14.dp, y = 6.dp)
                                        .rotate(25f)
                                        .shadow(8.dp, RoundedCornerShape(4.dp))
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Section: Discover Something New - Real Track Cards with Quick Play
                item {
                    Text(
                        text = "Discover something new",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(top = 20.dp, bottom = 14.dp)
                    )
                }

                if (discoverLoading && discoverCards.isEmpty()) {
                    items(2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SkeletonBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(220.dp),
                                shape = RoundedCornerShape(16.dp)
                            )
                            SkeletonBox(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(220.dp),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                } else {
                    val discoverRows = discoverCards.chunked(2)
                    items(discoverRows, key = { it.firstOrNull()?.id ?: "" }) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            pair.forEach { item ->
                                val isCardPlaying = item.track != null && currentPlayingTrack?.id == item.track.id && isPlaying
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(220.dp)
                                        .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF18181B))
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                        .clickable {
                                            if (item.track != null) {
                                                playerViewModel.playTrack(item.track, listOf(item.track))
                                            } else {
                                                searchViewModel.performSearchNow(item.query)
                                                isSubmitted = true
                                            }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF24242A),
                                                        Color(0xFF18181B),
                                                        Color(0xFF101012)
                                                    )
                                                )
                                            )
                                    )
                                    // Content
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xD9000000))
                                                    .border(1.dp, Color(0x4D1ED760), RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = item.tag,
                                                    color = Color(0xFF1ED760),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }

                                            // Spotify Green Play/Pause FAB
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .shadow(6.dp, CircleShape, spotColor = Color(0xFF1ED760).copy(alpha = 0.5f))
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF1ED760))
                                                    .clickable {
                                                        if (item.track != null) {
                                                            if (isCardPlaying) {
                                                                playerViewModel.togglePlayPause()
                                                            } else {
                                                                playerViewModel.playTrack(item.track, listOf(item.track))
                                                            }
                                                        } else {
                                                            searchViewModel.performSearchNow(item.query)
                                                            isSubmitted = true
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isCardPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = "Play",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        AsyncImage(
                                            model = item.image,
                                            contentDescription = item.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(105.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF262626))
                                        )

                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = item.title,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.artist,
                                                color = Color(0xFFB3B3B3),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Section: Explore by Language
                item {
                    Text(
                        text = "Explore by Language",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        listOf(
                            "All" to "All (${ALL_SPOTIFY_LANGUAGES.size})",
                            "Indian" to "Indian",
                            "Global" to "Global & International"
                        ).forEach { (tabKey, tabLabel) ->
                            val isSel = languageTab == tabKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSel) Color(0x331ED760) else Color(0xFF202024))
                                    .border(
                                        1.dp,
                                        if (isSel) Color(0x661ED760) else Color.Transparent,
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { languageTab = tabKey }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tabLabel,
                                    color = if (isSel) Color(0xFF1ED760) else Color(0xFFB3B3B3),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                val filteredLanguages = ALL_SPOTIFY_LANGUAGES.filter {
                    if (languageTab == "Indian") it.category == "Indian"
                    else if (languageTab == "Global") it.category == "Global"
                    else true
                }
                val languageRows = filteredLanguages.chunked(2)
                items(languageRows, key = { it.firstOrNull()?.name ?: "" }) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { lang ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = lang.primaryColor.copy(alpha = 0.35f))
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(lang.primaryColor, lang.secondaryColor)
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        searchViewModel.performSearchNow(lang.query)
                                        isSubmitted = true
                                    }
                                    .padding(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = lang.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.5.sp
                                        )
                                        Text(
                                            text = lang.native,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = lang.desc,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else if (!isSubmitted && suggestions.isNotEmpty()) {
            // Predictive Search Suggestions with Instant Tap
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
            ) {
                item {
                    // Quick submit row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E22))
                            .clickable {
                                searchViewModel.performSearchNow(query.trim())
                                isSubmitted = true
                                focusManager.clearFocus()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF1ED760),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search for \"$query\"",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "Matching suggestions",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(suggestions, key = { "sug_${it.id ?: it.title}" }) { sug ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                searchViewModel.performSearchNow(sug.title)
                                isSubmitted = true
                                focusManager.clearFocus()
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!sug.image.isNullOrBlank()) {
                            AsyncImage(
                                model = sug.image,
                                contentDescription = sug.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF262628)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color(0xFF888888))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sug.title,
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Song • ${sug.artist ?: "Unknown"}",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = {
                                searchViewModel.updateQuery(sug.title)
                            }
                        ) {
                            Icon(
                                Icons.Filled.NorthWest,
                                contentDescription = "Fill query",
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        } else if (isSearchLoading && tracks.isEmpty()) {
            SkeletonSearchScreen()
        } else {
            // Enhanced Real Search Results (Top Result, Songs, Artists, Albums, Playlists)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
            ) {
                // Personalized Intent Banner
                if (searchIntent != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF14241B)),
                            border = BorderStroke(1.dp, Color(0x331ED760)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1ED760).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Equalizer,
                                        contentDescription = null,
                                        tint = Color(0xFF1ED760),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = searchIntent?.personalizedTag ?: "Tuned to your listening style",
                                        color = Color(0xFF1ED760),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    searchIntent?.explanation?.let { exp ->
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = exp,
                                            color = Color(0xFFA0A0A5),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Top Result Card (Authentic Spotify Spotlight from `SearchPage.scss`)
                if (tracks.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Songs")) {
                    val topTrack = tracks.first()
                    item {
                        Text(
                            text = "Top result",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
                        )

                        val topCardInteraction = remember { MutableInteractionSource() }
                        val isTopPressed by topCardInteraction.collectIsPressedAsState()
                        val topCardBg by animateColorAsState(
                            targetValue = if (isTopPressed) Color(0xFF282828) else Color(0xFF181818),
                            animationSpec = tween(150),
                            label = "topResultBg"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(topCardBg)
                                .clickable(
                                    interactionSource = topCardInteraction,
                                    indication = null
                                ) {
                                    playerViewModel.playTrack(topTrack, tracks)
                                }
                                .padding(20.dp)
                        ) {
                            Column {
                                AsyncImage(
                                    model = topTrack.images?.large ?: topTrack.images?.medium ?: topTrack.images?.small,
                                    contentDescription = topTrack.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(92.dp)
                                        .shadow(12.dp, RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = topTrack.title,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF121212).copy(alpha = 0.6f))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("SONG", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = topTrack.artist ?: "Unknown Artist",
                                        color = Color(0xFFB3B3B3),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Spotify Green Play Button FAB in bottom right with spring scale bounce
                            val isThisPlaying = currentPlayingTrack?.id == topTrack.id && isPlaying
                            SpotifyCirclePlayButton(
                                isPlaying = isThisPlaying,
                                buttonSize = 50.dp,
                                iconSize = 26.dp,
                                modifier = Modifier.align(Alignment.BottomEnd),
                                onClick = {
                                    if (currentPlayingTrack?.id == topTrack.id) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playTrack(topTrack, tracks)
                                    }
                                }
                            )
                        }
                    }
                }

                // Songs Section (`SearchPage.scss` `.search-songs`)
                if (tracks.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Songs")) {
                    item {
                        Text(
                            text = "Songs",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
                        )
                    }

                    items(tracks, key = { "search_tr_${it.id}" }) { track ->
                        val isPlayingThis = currentPlayingTrack?.id == track.id
                        val isSaved = savedTracks.contains(track.id)
                        val durSec = (track.duration ?: 198).coerceAtLeast(1)
                        val formattedDur = "${durSec / 60}:${(durSec % 60).toString().padStart(2, '0')}"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPlayingThis) Color(0xFF282828).copy(alpha = 0.7f) else Color.Transparent)
                                .clickable {
                                    playerViewModel.playTrack(track, tracks)
                                }
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 48dp Cover with overlay play / animated equalizer
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF282828)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = track.images?.small ?: track.images?.medium,
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isPlayingThis) {
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
                                    text = track.title,
                                    color = if (isPlayingThis) Color(0xFF1ED760) else Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Explicit 'E' badge matching `.explicit` in `SearchPage.scss`
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

                            // Formatted duration
                            Text(
                                text = formattedDur,
                                color = Color(0xFFB3B3B3),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            // Like / Favorite Heart button with Spotify scale pulse
                            FavoriteHeartButton(
                                isLiked = isSaved,
                                onToggleLike = { playerViewModel.toggleSave(track) },
                                modifier = Modifier.size(34.dp),
                                iconSize = 19.dp,
                                unlikedColor = Color(0xFFB3B3B3),
                                likedColor = FavoriteRed
                            )

                            // 3-dot menu button
                            IconButton(
                                onClick = {
                                    trackMenu(TrackMenuState(track = track))
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color(0xFFB3B3B3),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Artists Section
                if (artists.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Artists")) {
                    item {
                        Text(
                            text = "Artists",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(artists, key = { "search_art_${it.id}" }) { artist ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(115.dp)
                                        .clickable {
                                            onNavigateToArtist(artist.id)
                                        }
                                ) {
                                    AsyncImage(
                                        model = artist.image,
                                        contentDescription = artist.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(105.dp)
                                            .shadow(8.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(Color(0xFF262628))
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = artist.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Artist",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Albums Section
                if (albums.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Albums")) {
                    item {
                        Text(
                            text = "Albums",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(albums, key = { "search_alb_${it.id}" }) { album ->
                                Column(
                                    modifier = Modifier
                                        .width(135.dp)
                                        .clickable {
                                            onNavigateToAlbum(album.id)
                                        }
                                ) {
                                    AsyncImage(
                                        model = album.images?.medium ?: album.images?.small,
                                        contentDescription = album.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(135.dp)
                                            .shadow(6.dp, RoundedCornerShape(10.dp))
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF262628))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = album.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${album.year ?: ""} • ${album.artist}",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Playlists Section
                if (playlists.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Playlists")) {
                    item {
                        Text(
                            text = "Playlists",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(playlists, key = { "search_pl_${it.id}" }) { pl ->
                                Column(
                                    modifier = Modifier
                                        .width(135.dp)
                                        .clickable {
                                            onNavigateToPlaylist(pl.id)
                                        }
                                ) {
                                    CollectionArtworkImage(
                                        tracks = emptyList(),
                                        customCoverUrl = pl.coverImage ?: pl.image,
                                        contentDescription = pl.title,
                                        modifier = Modifier
                                            .size(135.dp)
                                            .shadow(6.dp, RoundedCornerShape(10.dp))
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pl.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "By Headphonix",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty result state if query was executed but all lists are empty
                if (tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() && playlists.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 54.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222226)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    tint = Color(0xFFAAAAAA),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No results found for \"$query\"",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Please make sure your words are spelled correctly, or try searching for artists, songs, or genres.",
                                color = Color(0xFFAAAAAA),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Functional Camera / Code Scanner Dialog (When user taps Camera icon in Search header)
    if (showScannerModal) {
        AlertDialog(
            onDismissRequest = { showScannerModal = false },
            containerColor = Color(0xFF242424),
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF1ED760),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Scan Headphonix Code",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.5.dp, Color(0xFF1ED760), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF1ED760),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Point your camera at a code",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan any shared song, artist, or playlist code to jump straight to it.",
                        color = Color(0xFFB3B3B3),
                        fontSize = 12.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScannerModal = false
                        searchViewModel.performSearchNow("Top Hits Hindi")
                        isSubmitted = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1ED760),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Scan Demo Track", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScannerModal = false }) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}
