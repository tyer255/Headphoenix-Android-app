package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.MusicRepository
import com.example.data.Playlist
import com.example.data.PlaylistRepository
import com.example.data.remote.models.ImagesDto
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.delay

@Composable
fun AddTracksToPlaylistScreen(
    playlist: Playlist,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val musicRepo = remember { MusicRepository() }

    // Live playlist state from repository
    val playlists by PlaylistRepository.playlists.collectAsState()
    val currentLivePlaylist = playlists.find { it.id == playlist.id } ?: playlist

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Songs") }
    var isLoadingRecommendations by remember { mutableStateOf(true) }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<TrackDto>>(emptyList()) }
    var defaultRecommendations by remember { mutableStateOf<List<TrackDto>>(emptyList()) }

    // Seed default offline fallback list matching Screenshot_20260920_143501.jpg
    val initialSeedTracks = remember {
        listOf(
            TrackDto(
                id = "sadqay_aashir",
                title = "Sadqay",
                artist = "Aashir Wajahat, NAYEL, Nehaal Naseer",
                images = ImagesDto(
                    small = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/c6/f3/53/c6f35354-8bc7-599e-f6d7-4368976ed991/196871742290.jpg/600x600bb.jpg",
                    medium = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/c6/f3/53/c6f35354-8bc7-599e-f6d7-4368976ed991/196871742290.jpg/600x600bb.jpg",
                    large = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/c6/f3/53/c6f35354-8bc7-599e-f6d7-4368976ed991/196871742290.jpg/600x600bb.jpg"
                ),
                duration = 205
            ),
            TrackDto(
                id = "kangna_slowed",
                title = "Kangna - Slowed",
                artist = "idxzz, Vrushabh Kathale, Dj Blitz",
                images = ImagesDto(
                    small = "https://c.saavncdn.com/396/Kangna-Slowed-Reverb-Hindi-2023-20230623120536-500x500.jpg",
                    medium = "https://c.saavncdn.com/396/Kangna-Slowed-Reverb-Hindi-2023-20230623120536-500x500.jpg",
                    large = "https://c.saavncdn.com/396/Kangna-Slowed-Reverb-Hindi-2023-20230623120536-500x500.jpg"
                ),
                duration = 195
            ),
            TrackDto(
                id = "dhundhala_yashraj",
                title = "Dhundhala",
                artist = "Yashraj, Dropped Out, Talwiinder",
                images = ImagesDto(
                    small = "https://c.saavncdn.com/965/Dhundhala-Hindi-2023-20231014132506-500x500.jpg",
                    medium = "https://c.saavncdn.com/965/Dhundhala-Hindi-2023-20231014132506-500x500.jpg",
                    large = "https://c.saavncdn.com/965/Dhundhala-Hindi-2023-20231014132506-500x500.jpg"
                ),
                duration = 210
            ),
            TrackDto(
                id = "vigdiyan_heeran_honey",
                title = "Vigdiyan Heeran (From \"Honey 3.0\")",
                artist = "Yo Yo Honey Singh, Rony Ajnali, Gill M...",
                images = ImagesDto(
                    small = "https://c.saavncdn.com/712/GLORY-Hindi-2024-20240826180327-500x500.jpg",
                    medium = "https://c.saavncdn.com/712/GLORY-Hindi-2024-20240826180327-500x500.jpg",
                    large = "https://c.saavncdn.com/712/GLORY-Hindi-2024-20240826180327-500x500.jpg"
                ),
                duration = 175
            ),
            TrackDto(
                id = "talwinder_wishes_remake",
                title = "Talwinder - Wishes - Remake",
                artist = "Raiyan, Twinkle Thareja, raaghavan",
                images = ImagesDto(
                    small = "https://c.saavncdn.com/392/Wishes-Urdu-2022-20221021111956-500x500.jpg",
                    medium = "https://c.saavncdn.com/392/Wishes-Urdu-2022-20221021111956-500x500.jpg",
                    large = "https://c.saavncdn.com/392/Wishes-Urdu-2022-20221021111956-500x500.jpg"
                ),
                duration = 220
            ),
            TrackDto(
                id = "samjho_na_aditya",
                title = "Samjho Na",
                artist = "Aditya Rikhari",
                images = ImagesDto(
                    small = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/36/53/78/365378ad-5527-2c1b-29a3-c15555465e31/8909024041773.png/600x600bb.jpg",
                    medium = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/36/53/78/365378ad-5527-2c1b-29a3-c15555465e31/8909024041773.png/600x600bb.jpg",
                    large = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/36/53/78/365378ad-5527-2c1b-29a3-c15555465e31/8909024041773.png/600x600bb.jpg"
                ),
                duration = 190
            ),
            TrackDto(
                id = "dard_kushagra",
                title = "Dard",
                artist = "Kushagra, Showkidd, Akash Rajput",
                images = ImagesDto(
                    small = "https://c.saavncdn.com/985/Dard-Hindi-2023-20230919172439-500x500.jpg",
                    medium = "https://c.saavncdn.com/985/Dard-Hindi-2023-20230919172439-500x500.jpg",
                    large = "https://c.saavncdn.com/985/Dard-Hindi-2023-20230919172439-500x500.jpg"
                ),
                duration = 185
            ),
            TrackDto(
                id = "ride_it_jay_sean",
                title = "Ride It (Kya Yehi Pyaar Hai) - ...",
                artist = "Jay Sean",
                images = ImagesDto(
                    small = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/63/32/cb/6332cbab-eafb-f087-c574-3c1239b18a89/5037300757797.jpg/600x600bb.jpg",
                    medium = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/63/32/cb/6332cbab-eafb-f087-c574-3c1239b18a89/5037300757797.jpg/600x600bb.jpg",
                    large = "https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/63/32/cb/6332cbab-eafb-f087-c574-3c1239b18a89/5037300757797.jpg/600x600bb.jpg"
                ),
                duration = 192
            ),
            TrackDto(
                id = "tu_hai_kahan_aur",
                title = "Tu hai kahan",
                artist = "AUR",
                images = ImagesDto(
                    small = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/58/44/66/584466db-9e8e-8690-97d1-8289cc7c4342/197877488397.jpg/600x600bb.jpg",
                    medium = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/58/44/66/584466db-9e8e-8690-97d1-8289cc7c4342/197877488397.jpg/600x600bb.jpg",
                    large = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/58/44/66/584466db-9e8e-8690-97d1-8289cc7c4342/197877488397.jpg/600x600bb.jpg"
                ),
                duration = 245
            )
        )
    }

    // Load rich live recommendations list on launch with skeleton loader
    LaunchedEffect(Unit) {
        isLoadingRecommendations = true
        try {
            val allSongs = mutableListOf<TrackDto>()
            allSongs.addAll(initialSeedTracks)

            // Add user's liked tracks
            val liked = PlaylistRepository.likedTracks.value
            allSongs.addAll(liked)

            // Fetch rich home recommendations
            val homeData = musicRepo.getHome()
            if (homeData != null) {
                homeData.quickPicks?.let { allSongs.addAll(it) }
                homeData.popularSongs?.let { allSongs.addAll(it) }
                homeData.trending?.let { allSongs.addAll(it) }
                homeData.madeForYou?.let { allSongs.addAll(it) }
                homeData.recentlyPlayed?.let { allSongs.addAll(it) }
            }

            // Fallback search to guarantee plenty of songs
            if (allSongs.size < 20) {
                val fallbackSearch = musicRepo.search("Hindi Top Hits")
                fallbackSearch?.songs?.let { allSongs.addAll(it) }
            }

            // Deduplicate by ID
            defaultRecommendations = allSongs.distinctBy { it.id }
        } catch (e: Exception) {
            defaultRecommendations = initialSeedTracks
        } finally {
            isLoadingRecommendations = false
        }
    }

    // Live search query handler with debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(250)
        try {
            val response = musicRepo.search(searchQuery.trim())
            searchResults = response?.songs ?: emptyList()
        } catch (e: Exception) {
            searchResults = emptyList()
        } finally {
            isSearching = false
        }
    }

    val displayList = if (searchQuery.isNotBlank()) searchResults else defaultRecommendations

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // 1. TOP HEADER & FILTER CHIPS (Matching Screenshot_20260920_143501.jpg)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF121212))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Back button + Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Add to this playlist",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prominent Spotify-Style Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF242424))
                        .clickable {
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "What would you like to add?",
                                color = Color(0xFFB3B3B3),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF1ED760)),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboardController?.hide() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFFB3B3B3),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Filter chips: Songs (Green), Episodes, Recently played
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Songs", "Episodes", "Recently played").forEach { chip ->
                        val isSelected = selectedFilter == chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFF1ED760) else Color(0xFF282828))
                                .clickable { selectedFilter = chip }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = chip,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 2. SCROLLABLE SONGS LIST WITH SKELETON LOADING SHIMMER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoadingRecommendations || isSearching) {
                    // Shimmering Skeleton Loader
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(8) {
                            SkeletonAddTracksRow()
                        }
                    }
                } else if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No songs found for \"$searchQuery\"" else "No songs available",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(displayList, key = { it.id }) { track ->
                            val isAlreadyInPlaylist = currentLivePlaylist.tracks.any { it.id == track.id }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isAlreadyInPlaylist) {
                                            PlaylistRepository.removeTrackFromPlaylist(playlist.id, track.id)
                                            Toast.makeText(context, "Removed from ${currentLivePlaylist.title}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            PlaylistRepository.addTrackToPlaylist(playlist.id, track)
                                            Toast.makeText(context, "Added to ${currentLivePlaylist.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover artwork with play triangle overlay
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SongArtworkImage(
                                        track = track,
                                        title = track.title,
                                        artist = track.artist,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Song title and Artist
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = track.artist ?: "Unknown Artist",
                                        color = Color(0xFFB3B3B3),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Add (+) or Added (✓) circular icon
                                IconButton(
                                    onClick = {
                                        if (isAlreadyInPlaylist) {
                                            PlaylistRepository.removeTrackFromPlaylist(playlist.id, track.id)
                                            Toast.makeText(context, "Removed from ${currentLivePlaylist.title}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            PlaylistRepository.addTrackToPlaylist(playlist.id, track)
                                            Toast.makeText(context, "Added to ${currentLivePlaylist.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    if (isAlreadyInPlaylist) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Added",
                                            tint = Color(0xFF1ED760),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.AddCircleOutline,
                                            contentDescription = "Add to playlist",
                                            tint = Color(0xFFB3B3B3),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. FLOATING CAPSULE SEARCH BAR AT BOTTOM (Exact match with Screenshot_20260920_143501.jpg)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF121212).copy(alpha = 0.85f),
                            Color(0xFF121212)
                        )
                    )
                )
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF242424))
                    .clickable {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "What would you like to add?",
                            color = Color(0xFFB3B3B3),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(Color(0xFF1ED760)),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFFB3B3B3),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
