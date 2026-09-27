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
import com.example.ui.theme.GreenPrimary

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

    var discoverCards by remember { mutableStateOf<List<RealDiscoverCard>>(emptyList()) }
    var discoverLoading by remember { mutableStateOf(true) }

    // Recent search history pills
    var recentSearches by remember {
        mutableStateOf(listOf("Anuv Jain", "Arijit Singh", "Karan Aujla", "Husn", "Tauba Tauba", "Coke Studio"))
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
            .background(Color(0xFF0F0F11))
            .statusBarsPadding()
    ) {
        // 1. Premium Header with Ambient Glow & Profile Avatar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
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
                        modifier = Modifier.size(22.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            IconButton(
                onClick = { onOpenProfile() },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF202024))
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Official Spotify Search Input Box with Ambient Shadow & Clear / Voice affordances
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Clear",
                        tint = Color(0xFF121212),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                searchViewModel.updateQuery("")
                                isSubmitted = false
                                focusManager.clearFocus()
                            }
                    )
                } else {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF121212),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                BasicTextField(
                    value = query,
                    onValueChange = { newQuery ->
                        searchViewModel.updateQuery(newQuery)
                        isSubmitted = false
                    },
                    textStyle = TextStyle(
                        color = Color(0xFF121212),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color(0xFF1DB954)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (query.trim().isNotEmpty()) {
                                searchViewModel.performSearchNow(query.trim())
                                isSubmitted = true
                                focusManager.clearFocus()
                                if (!recentSearches.contains(query.trim())) {
                                    recentSearches = (listOf(query.trim()) + recentSearches).take(8)
                                }
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                "What do you want to listen to?",
                                color = Color(0xFF666666),
                                fontSize = 14.5.sp,
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
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear text",
                            tint = Color(0xFF444444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF121212))
                            .clickable {
                                searchViewModel.performSearchNow(query.trim())
                                isSubmitted = true
                                focusManager.clearFocus()
                                if (!recentSearches.contains(query.trim())) {
                                    recentSearches = (listOf(query.trim()) + recentSearches).take(8)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Submit",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Spotify Filter Chips (Shown when user types or submits search)
        if (query.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterList = listOf("All", "Songs", "Artists", "Albums", "Playlists")
                items(filterList, key = { it }) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) Color(0xFF1ED760) else Color(0xFF222226))
                            .border(
                                1.dp,
                                if (isSelected) Color.Transparent else Color(0x24FFFFFF),
                                RoundedCornerShape(50)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.Black else Color(0xFFE2E2E2),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 4. Content Area
        if (query.isEmpty()) {
            // Enhanced Spotify Browse & Discovery Grid
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
            ) {
                // Section: Recent Searches (Quick Pills)
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.History,
                                        contentDescription = null,
                                        tint = Color(0xFFAAAAAA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Recent Searches",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                                Text(
                                    text = "Clear",
                                    color = Color(0xFF888888),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { recentSearches = emptyList() }
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(recentSearches, key = { "recent_$it" }) { item ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF1E1E22))
                                            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(50))
                                            .clickable {
                                                searchViewModel.performSearchNow(item)
                                                isSubmitted = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
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

                // Section: Browse All (Official Spotify 2-Column Angled Card Grid)
                item {
                    Text(
                        text = "Browse all",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
                    )
                }

                val categoryRows = SPOTIFY_BROWSE_ALL_CATEGORIES.chunked(2)
                items(categoryRows, key = { it.firstOrNull()?.title ?: "" }) { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        pair.forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .shadow(6.dp, RoundedCornerShape(10.dp), spotColor = cat.primaryColor.copy(alpha = 0.35f))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(cat.primaryColor, cat.secondaryColor)
                                        )
                                    )
                                    .clickable {
                                        searchViewModel.performSearchNow(cat.query)
                                        isSubmitted = true
                                    }
                            ) {
                                Text(
                                    text = cat.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.5.sp,
                                    lineHeight = 19.sp,
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
                                        .size(68.dp)
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 12.dp, y = 10.dp)
                                        .rotate(25f)
                                        .shadow(8.dp, RoundedCornerShape(6.dp))
                                        .clip(RoundedCornerShape(6.dp))
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

                // Top Result Card (Authentic Spotify Spotlight)
                if (tracks.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Songs")) {
                    val topTrack = tracks.first()
                    item {
                        Text(
                            text = "Top result",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color.Black)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF222226), Color(0xFF161619))
                                    )
                                )
                                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                                .clickable {
                                    playerViewModel.playTrack(topTrack, tracks)
                                }
                                .padding(18.dp)
                        ) {
                            Column {
                                AsyncImage(
                                    model = topTrack.images?.large ?: topTrack.images?.medium ?: topTrack.images?.small,
                                    contentDescription = topTrack.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(86.dp)
                                        .shadow(8.dp, RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = topTrack.title,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.4).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0x33FFFFFF))
                                            .padding(horizontal = 9.dp, vertical = 3.dp)
                                    ) {
                                        Text("Song", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = topTrack.artist ?: "Unknown Artist",
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Spotify Green Play Button FAB in bottom right
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .align(Alignment.BottomEnd)
                                    .shadow(8.dp, CircleShape, spotColor = Color(0xFF1ED760).copy(alpha = 0.5f))
                                    .clip(CircleShape)
                                    .background(Color(0xFF1ED760))
                                    .clickable {
                                        playerViewModel.playTrack(topTrack, tracks)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val isThisPlaying = currentPlayingTrack?.id == topTrack.id && isPlaying
                                Icon(
                                    if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                // Songs Section
                if (tracks.isNotEmpty() && (selectedFilter == "All" || selectedFilter == "Songs")) {
                    item {
                        Text(
                            text = "Songs",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 22.dp, bottom = 12.dp)
                        )
                    }

                    items(tracks, key = { "search_tr_${it.id}" }) { track ->
                        val isPlayingThis = currentPlayingTrack?.id == track.id
                        val isSaved = savedTracks.contains(track.id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    playerViewModel.playTrack(track, tracks)
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.images?.small ?: track.images?.medium,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF262628))
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            val isPrefArtist = track.artist?.contains("Anuv Jain", ignoreCase = true) == true ||
                                               track.artist?.contains("Arijit Singh", ignoreCase = true) == true ||
                                               track.artist?.contains("Karan Aujla", ignoreCase = true) == true ||
                                               track.artist?.contains("KR\$NA", ignoreCase = true) == true

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = track.title,
                                        color = if (isPlayingThis) Color(0xFF1ED760) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isSaved) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x331ED760))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("♥ Saved", color = Color(0xFF1ED760), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (isPrefArtist) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x221ED760))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("★ For You", color = Color(0xFF1ED760), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = track.artist ?: "Unknown Artist",
                                    color = Color(0xFFAAAAAA),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Like / Favorite Heart button
                            FavoriteHeartButton(
                                isLiked = isSaved,
                                onToggleLike = { playerViewModel.toggleSave(track) },
                                modifier = Modifier.size(38.dp),
                                iconSize = 20.dp,
                                unlikedColor = Color.Gray,
                                likedColor = FavoriteRed
                            )

                            // 3-dot menu button
                            IconButton(
                                onClick = {
                                    trackMenu(TrackMenuState(track = track))
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.Gray,
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
                                        text = "By Spotify",
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
}
