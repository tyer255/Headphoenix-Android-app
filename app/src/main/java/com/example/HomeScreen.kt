package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AppDownloadManager
import com.example.data.PlaybackHistoryRepository
import com.example.data.PlaylistRepository
import com.example.data.remote.models.AlbumDto
import com.example.ui.CollectionArtworkImage
import com.example.ui.FavoriteHeartIcon
import com.example.data.remote.models.ArtistDto
import com.example.data.remote.models.HomeDto
import com.example.data.remote.models.ImagesDto
import com.example.data.remote.models.TrackDto
import com.example.ui.theme.GreenPrimary
import kotlin.math.absoluteValue

data class StationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val artistImages: List<String>,
    val query: String
)

data class FavoriteArtistItem(
    val id: String,
    val name: String,
    val image: String
)

data class PopularAlbumRelease(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String,
    val query: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlaylist: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToSearch: (String) -> Unit = {},
    trackMenu: (TrackMenuState) -> Unit = {},
    openProfileDrawer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentlyPlayedHistory by PlaybackHistoryRepository.recentlyPlayed.collectAsState()
    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val userProfile by PlaylistRepository.userProfile.collectAsState()
    val context = LocalContext.current
    val downloadedTracks by AppDownloadManager.downloadedTracks.collectAsState()
    val likedTracks by PlaylistRepository.likedTracks.collectAsState()
    val personalizedSection by viewModel.personalizedSection.collectAsState()
    val isOnline by com.example.util.NetworkMonitor.isOnline.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }

    // Followed Artists local state
    var followedArtistIds by remember {
        mutableStateOf(setOf("arijit_singh", "anuv_jain", "karan_aujla", "krsna"))
    }

    // 1. Featured Hero Tracks (Curated high-resolution matching screenshot 1)
    val heroTracks = remember {
        listOf(
            TrackDto(
                id = "hero_arz_kiya_hai",
                title = "Arz Kiya Hai",
                artist = "Anuv Jain",
                album = "Coke Studio Bharat",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "hero_riha",
                title = "Riha",
                artist = "Anuv Jain",
                album = "Riha",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/da3e4ae32545d975bddda426dbb2407d/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/da3e4ae32545d975bddda426dbb2407d/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/da3e4ae32545d975bddda426dbb2407d/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "hero_mishri",
                title = "Mishri",
                artist = "Anuv Jain",
                album = "Mishri",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "hero_baarishein",
                title = "Baarishein",
                artist = "Anuv Jain",
                album = "Baarishein",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/b4fcda10b32a70d8b9248ca7f6459903/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/b4fcda10b32a70d8b9248ca7f6459903/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/b4fcda10b32a70d8b9248ca7f6459903/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "hero_husn",
                title = "Husn",
                artist = "Anuv Jain",
                album = "Husn",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/250x250-000000-80-0-0.jpg"
                )
            )
        )
    }

    // 2. "Recommended for today"
    val curatedRecommended = remember {
        listOf(
            TrackDto(
                id = "rec_arz_kiya_hai",
                title = "Arz Kiya Hai | Coke Studio Bharat",
                artist = "Anuv Jain",
                album = "Coke Studio Bharat",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "rec_baarishein",
                title = "Baarishein",
                artist = "Anuv Jain",
                album = "Baarishein",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/b4fcda10b32a70d8b9248ca7f6459903/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/b4fcda10b32a70d8b9248ca7f6459903/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/b4fcda10b32a70d8b9248ca7f6459903/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "rec_jotum",
                title = "Jo Tum Mere Ho",
                artist = "Anuv Jain",
                album = "Jo Tum Mere Ho",
                images = ImagesDto(
                    large = "https://i.scdn.co/image/ab67616d0000b27372a77d038887cdc425f5ee55",
                    medium = "https://i.scdn.co/image/ab67616d00001e0272a77d038887cdc425f5ee55",
                    small = "https://i.scdn.co/image/ab67616d0000485172a77d038887cdc425f5ee55"
                )
            ),
            TrackDto(
                id = "rec_husn",
                title = "Husn",
                artist = "Anuv Jain",
                album = "Husn",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "rec_mishri",
                title = "Mishri",
                artist = "Anuv Jain",
                album = "Mishri",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/250x250-000000-80-0-0.jpg"
                )
            )
        )
    }

    // 3. "Start listening" (Exact from Screenshot 1 & 2)
    val startListeningTracks = remember {
        listOf(
            TrackDto(
                id = "sl_gangaram",
                title = "Gangaram Ki Samajh Men Na Aaye - Woh Pari Kahan Se Laoon",
                artist = "Sharda, Suman Kalyanpur, Mukesh",
                album = "Pehchan",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/08ef118382a7f9dc594a0a633ffdb749/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/08ef118382a7f9dc594a0a633ffdb749/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/08ef118382a7f9dc594a0a633ffdb749/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "sl_assamese",
                title = "Assamese",
                artist = "Bhonisha",
                album = "Folk Music of Northeast India",
                images = ImagesDto(
                    large = "https://c.saavncdn.com/568/Folk-Music-of-Northeast-India-Assamese-2021-20211024155834-500x500.jpg",
                    medium = "https://c.saavncdn.com/568/Folk-Music-of-Northeast-India-Assamese-2021-20211024155834-500x500.jpg",
                    small = "https://c.saavncdn.com/568/Folk-Music-of-Northeast-India-Assamese-2021-20211024155834-150x150.jpg"
                )
            ),
            TrackDto(
                id = "sl_popiya",
                title = "Popiya Tora",
                artist = "Zubeen Garg, Rahul Gautam",
                album = "Maa",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/fc6b076e07846043bfd564cfda83ea43/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/fc6b076e07846043bfd564cfda83ea43/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/fc6b076e07846043bfd564cfda83ea43/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "sl_raanjhan",
                title = "Raanjhan (From \"Do Patti\")",
                artist = "Sachet-Parampara, Parampara Tandon",
                album = "Do Patti",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "sl_iguess",
                title = "I Guess",
                artist = "KR\$NA",
                album = "Far From Over",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/9ee1cb0b85c5ca9278de92736d5dd927/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/9ee1cb0b85c5ca9278de92736d5dd927/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/9ee1cb0b85c5ca9278de92736d5dd927/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "sl_afsos",
                title = "Afsos",
                artist = "Anuv Jain, AP Dhillon",
                album = "Afsos",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/782384366b973c63eb1a2b0e50ba9910/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/782384366b973c63eb1a2b0e50ba9910/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/782384366b973c63eb1a2b0e50ba9910/250x250-000000-80-0-0.jpg"
                )
            )
        )
    }

    // 4. "BECAUSE YOU LISTEN TO Anuv Jain" (Exact from Screenshot 2)
    val becauseAnuvJainTracks = remember {
        listOf(
            TrackDto(
                id = "bj_arz",
                title = "Arz Kiya Hai | Coke Studio Bharat",
                artist = "Anuv Jain",
                album = "Coke Studio Bharat",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/269ee6cfef6451ce303541fae19f8fb6/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "bj_jotum",
                title = "Jo Tum Mere Ho",
                artist = "Anuv Jain",
                album = "Jo Tum Mere Ho",
                images = ImagesDto(
                    large = "https://i.scdn.co/image/ab67616d0000b27372a77d038887cdc425f5ee55",
                    medium = "https://i.scdn.co/image/ab67616d00001e0272a77d038887cdc425f5ee55",
                    small = "https://i.scdn.co/image/ab67616d0000485172a77d038887cdc425f5ee55"
                )
            ),
            TrackDto(
                id = "bj_husn",
                title = "Husn",
                artist = "Anuv Jain",
                album = "Husn",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/bdcf70737dc185ef7ec866fb29591137/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "bj_mishri",
                title = "Mishri",
                artist = "Anuv Jain",
                album = "Mishri",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/933af1fcb8e2a5a3f826fc36ce9d8511/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "bj_alagaasmaan",
                title = "Alag Aasmaan",
                artist = "Anuv Jain",
                album = "Alag Aasmaan",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/1ed1f36c80fe430ca97098f68fc074e6/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/1ed1f36c80fe430ca97098f68fc074e6/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/1ed1f36c80fe430ca97098f68fc074e6/250x250-000000-80-0-0.jpg"
                )
            )
        )
    }

    // 5. "Trending Hits" (Exact from Screenshot 2 & 3)
    val trendingHitsTracks = remember {
        listOf(
            TrackDto(
                id = "th_tauba",
                title = "Tauba Tauba",
                artist = "Karan Aujla, Mr Tuo...",
                album = "Bad Newz",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/ff6bb1420d9fcd2671cf6f86c2e49658/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/ff6bb1420d9fcd2671cf6f86c2e49658/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/ff6bb1420d9fcd2671cf6f86c2e49658/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "th_sajni",
                title = "Sajni (From \"Laapataa Ladies\")",
                artist = "Prashant Pandey, R...",
                album = "Laapataa Ladies",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/6abead2d0890caf99ec89d4edf4f3ca2/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/6abead2d0890caf99ec89d4edf4f3ca2/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/6abead2d0890caf99ec89d4edf4f3ca2/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "th_raanjhan",
                title = "Raanjhan (From \"Do Patti\")",
                artist = "Sachet-Parampara",
                album = "Do Patti",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/5bcfa70020e9c40bca60ee35b2b89860/250x250-000000-80-0-0.jpg"
                )
            ),
            TrackDto(
                id = "th_winning",
                title = "Winning Speech",
                artist = "Karan Aujla",
                album = "Four Me",
                images = ImagesDto(
                    large = "https://cdn-images.dzcdn.net/images/cover/b6ff41520784c1c1b8cbff7925817cd8/1000x1000-000000-80-0-0.jpg",
                    medium = "https://cdn-images.dzcdn.net/images/cover/b6ff41520784c1c1b8cbff7925817cd8/500x500-000000-80-0-0.jpg",
                    small = "https://cdn-images.dzcdn.net/images/cover/b6ff41520784c1c1b8cbff7925817cd8/250x250-000000-80-0-0.jpg"
                )
            )
        )
    }

    // 6. "Recommended Stations" (Exact from Screenshot 3 & 4)
    val recommendedStations = remember {
        listOf(
            StationItem(
                id = "st_arijit",
                title = "Arijit Singh Radio",
                subtitle = "With Atif Aslam, Pritam, Shrey...",
                gradientColors = listOf(Color(0xFF0F8A58), Color(0xFF07472A)),
                artistImages = listOf(
                    "https://cdn-images.dzcdn.net/images/artist/ac5350cff290edd5b69fa584b8b1bd4f/250x250-000000-80-0-0.jpg",
                    "https://cdn-images.dzcdn.net/images/artist/0ea90444148fff9c11d77f06a344724e/250x250-000000-80-0-0.jpg",
                    "https://cdn-images.dzcdn.net/images/artist/3bb832d37d10ff2affcfa9afdc7c68a0/250x250-000000-80-0-0.jpg"
                ),
                query = "Arijit Singh"
            ),
            StationItem(
                id = "st_kishore",
                title = "Kishore Kumar Radio",
                subtitle = "With Lata Mangeshkar, M...",
                gradientColors = listOf(Color(0xFF1E58A4), Color(0xFF0F3260)),
                artistImages = listOf(
                    "https://cdn-images.dzcdn.net/images/artist/5972263348ad902e29a4749e748ff452/250x250-000000-80-0-0.jpg",
                    "https://cdn-images.dzcdn.net/images/artist/837d46f90f541736e07817f463317c80/250x250-000000-80-0-0.jpg",
                    "https://cdn-images.dzcdn.net/images/artist/9e79b89a9b2073fae0cc2f6bce278abe/250x250-000000-80-0-0.jpg"
                ),
                query = "Kishore Kumar"
            ),
            StationItem(
                id = "st_anuv",
                title = "Anuv Jain Radio",
                subtitle = "With Prateek Kuhad, Zaeden...",
                gradientColors = listOf(Color(0xFF5B2C6F), Color(0xFF2C133B)),
                artistImages = listOf(
                    "https://cdn-images.dzcdn.net/images/artist/eb0c0e91c8ad621b41178e0d66c81057/250x250-000000-80-0-0.jpg",
                    "https://cdn-images.dzcdn.net/images/artist/7f69b3905ae630b3f7bcd826bd2dc46f/250x250-000000-80-0-0.jpg",
                    "https://cdn-images.dzcdn.net/images/artist/82a0de1d05a95734841fd4e8b77c83f6/250x250-000000-80-0-0.jpg"
                ),
                query = "Anuv Jain"
            )
        )
    }

    // 7. "Your favourite artists" (Exact from Screenshot 4 & 5)
    val favoriteArtists = remember {
        listOf(
            FavoriteArtistItem(
                id = "arijit_singh",
                name = "Arijit Singh",
                image = "https://cdn-images.dzcdn.net/images/artist/ac5350cff290edd5b69fa584b8b1bd4f/500x500-000000-80-0-0.jpg"
            ),
            FavoriteArtistItem(
                id = "anuv_jain",
                name = "Anuv Jain",
                image = "https://cdn-images.dzcdn.net/images/artist/eb0c0e91c8ad621b41178e0d66c81057/500x500-000000-80-0-0.jpg"
            ),
            FavoriteArtistItem(
                id = "karan_aujla",
                name = "Karan Aujla",
                image = "https://cdn-images.dzcdn.net/images/artist/a91a1d5ea91e85e4f0966569b50e8d6a/500x500-000000-80-0-0.jpg"
            ),
            FavoriteArtistItem(
                id = "krsna",
                name = "KR\$NA",
                image = "https://cdn-images.dzcdn.net/images/artist/44dcd9d1d0be03a355c2d93c31bd8015/500x500-000000-80-0-0.jpg"
            )
        )
    }

    // 8. "Popular albums and new releases" (Exact from Screenshot 5)
    val popularAlbums = remember {
        listOf(
            PopularAlbumRelease(
                id = "alb_val",
                title = "Valentine Hits 2025",
                subtitle = "2025 • Arijit Singh, ...",
                image = "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/9a/2f/2a/9a2f2aa2-5343-7c13-e3d3-5fd6f79ea2fb/8909024071657.png/600x600bb.jpg",
                query = "Valentine Hits 2025"
            ),
            PopularAlbumRelease(
                id = "alb_bolly",
                title = "Bollywood - Top Hits...",
                subtitle = "2015 • Various Artists",
                image = "https://is1-ssl.mzstatic.com/image/thumb/Music49/v4/cc/86/c3/cc86c3b9-aef5-fcb4-a417-fbf5ea4d480f/Bollywood_-_Top_Hits_of_Decade.jpg/600x600bb.jpg",
                query = "Bollywood Top Hits of Decade"
            ),
            PopularAlbumRelease(
                id = "alb_aashiqui",
                title = "Aashiqui 2",
                subtitle = "2013 • Mithoon, Ankit Tiwari",
                image = "https://cdn-images.dzcdn.net/images/cover/ad8ebbaa26ac316a96849f12eeb5f63d/500x500-000000-80-0-0.jpg",
                query = "Aashiqui 2"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current, top = 8.dp)
        ) {
            // 1. TOP HEADER: Fiery Profile Avatar + Filter Chips ("All", "Music", "Podcasts")
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar matching Left Slide Bar (WebsiteSidebar)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64B5F6))
                            .clickable { openProfileDrawer() },
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
                            val avatarLetter = if (userProfile.name.isNotBlank() && !userProfile.name.equals("Guest", ignoreCase = true) && !userProfile.name.startsWith("Guest", ignoreCase = true)) {
                                userProfile.name.first().uppercaseChar().toString()
                            } else {
                                "G"
                            }
                            Text(
                                text = avatarLetter,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    HomeFilterPill(
                        label = "All",
                        isSelected = selectedFilter == "All",
                        onClick = { selectedFilter = "All" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HomeFilterPill(
                        label = "Music",
                        isSelected = selectedFilter == "Music",
                        onClick = { selectedFilter = "Music" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HomeFilterPill(
                        label = "Podcasts",
                        isSelected = selectedFilter == "Podcasts",
                        onClick = { selectedFilter = "Podcasts" }
                    )
                }
            }

            // 1.5. OFFLINE BANNER & DOWNLOADED MUSIC (Displayed prominently when offline)
            if (!isOnline) {
                item(key = "offline_banner_card") {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F24)),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowCircleDown,
                                    contentDescription = "Offline Mode",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline Mode",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (downloadedTracks.isNotEmpty())
                                        "${downloadedTracks.size} downloaded songs ready to play offline"
                                    else
                                        "No internet connection. Connect to stream or download music.",
                                    color = Color(0xFFB0B0B8),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                if (downloadedTracks.isNotEmpty()) {
                    item(key = "offline_downloaded_row") {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            Text(
                                text = "Downloaded Songs",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(downloadedTracks, key = { "offline_${it.id}" }) { track ->
                                    CardTrackItem(
                                        track = track,
                                        tag = "Offline",
                                        isOffline = true,
                                        isLiked = likedTracks.any { it.id == track.id },
                                        isPlaying = currentTrack?.id == track.id && isPlaying,
                                        onCardClick = {
                                            playerViewModel.playTrack(track, downloadedTracks)
                                        },
                                        onLikeClick = {
                                            PlaylistRepository.toggleLikeTrack(track)
                                        },
                                        onMoreClick = {
                                            trackMenu(TrackMenuState(track = track))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. FEATURED HERO CAROUSEL CARD (Exactly matching upper screenshot)
            if (selectedFilter != "Podcasts") {
                item(key = "featured_hero_carousel") {
                    HeroCarouselCard(
                        heroTracks = heroTracks,
                        currentPlayingTrack = currentTrack,
                        isPlaying = isPlaying,
                        onPlayTrack = { track ->
                            playerViewModel.playTrack(track, heroTracks)
                        },
                        onTrackClick = { track ->
                            playerViewModel.playTrack(track, heroTracks)
                        }
                    )
                }
            }

            // 3. SECTION: "Recommended for today"
            if (selectedFilter != "Podcasts") {
                item(key = "recommended_section_header") {
                    Text(
                        text = "Recommended for today",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)
                    )

                    val feedRecommended: List<TrackDto> = (uiState as? HomeUiState.Success)?.data?.quickPicks ?: emptyList()
                    val combinedRecommended: List<TrackDto> = (curatedRecommended + feedRecommended.filter { ft ->
                        curatedRecommended.none { it.id == ft.id || it.title.equals(ft.title, ignoreCase = true) }
                    }).distinctBy { it.title }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(combinedRecommended, key = { "rec_${it.id}" }) { track ->
                            val isOffline = downloadedTracks.any { it.id == track.id }
                            val isLiked = likedTracks.any { it.id == track.id }

                            CardTrackItem(
                                track = track,
                                tag = "Single",
                                isOffline = isOffline,
                                isLiked = isLiked,
                                isPlaying = currentTrack?.id == track.id && isPlaying,
                                onCardClick = {
                                    playerViewModel.playTrack(track, combinedRecommended)
                                },
                                onLikeClick = {
                                    PlaylistRepository.toggleLikeTrack(track)
                                },
                                onMoreClick = {
                                    trackMenu(TrackMenuState(track = track))
                                }
                            )
                        }
                    }
                }
            }

            // 4. SECTION: "Start listening" (Screenshots 1 & 2)
            if (selectedFilter != "Podcasts") {
                item(key = "start_listening_section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = "Jump into a session based on your tastes",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start listening",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val liveStartListening = (uiState as? HomeUiState.Success)?.data?.popularSongs?.takeIf { it.isNotEmpty() }?.take(6) ?: startListeningTracks

                    // Vertical Column of items exactly matching screenshot
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        liveStartListening.forEach { track ->
                            val isCurrent = currentTrack?.id == track.id && isPlaying
                            StartListeningTrackRow(
                                track = track,
                                isPlaying = isCurrent,
                                onClick = {
                                    playerViewModel.playTrack(track, liveStartListening)
                                },
                                onMoreClick = {
                                    trackMenu(TrackMenuState(track = track))
                                }
                            )
                        }
                    }
                }
            }

            // 5. SECTION: "BECAUSE YOU LISTEN TO [Top Artist]" (Dynamic user personalization)
            if (selectedFilter != "Podcasts") {
                item(key = "because_you_listen_section") {
                    val displayArtist = personalizedSection.artistName.ifBlank { "Anuv Jain" }
                    val displayAvatar = personalizedSection.artistAvatar.ifBlank { "https://i.scdn.co/image/ab6761610000e5eba837a6cb82dd949d5e1f9b53" }
                    val displayTracks = if (personalizedSection.tracks.isNotEmpty()) personalizedSection.tracks else becauseAnuvJainTracks

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp)
                            .clickable { onNavigateToArtist(displayArtist) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = displayAvatar,
                            contentDescription = displayArtist,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "BECAUSE YOU LISTEN TO",
                                color = Color(0xFF8E8E93),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = displayArtist,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(displayTracks, key = { "disp_${it.id}" }) { track ->
                            val isLiked = likedTracks.any { it.id == track.id } ||
                                          likedTracks.any { it.title.equals(track.title, ignoreCase = true) }
                            val isCurrent = currentTrack?.id == track.id && isPlaying

                            CardTrackItem(
                                track = track,
                                tag = "Single",
                                isOffline = downloadedTracks.any { it.id == track.id },
                                isLiked = isLiked,
                                isPlaying = isCurrent,
                                onCardClick = {
                                    playerViewModel.playTrack(track, displayTracks)
                                },
                                onLikeClick = {
                                    PlaylistRepository.toggleLikeTrack(track)
                                },
                                onMoreClick = {
                                    trackMenu(TrackMenuState(track = track))
                                }
                            )
                        }
                    }
                }
            }

            // 6. SECTION: "Trending Hits" (Screenshots 2 & 3)
            if (selectedFilter != "Podcasts") {
                item(key = "trending_hits_section") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Trending Hits",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Global Pop",
                            color = Color(0xFF8E8E93),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    val liveTrending = (uiState as? HomeUiState.Success)?.data?.trending?.takeIf { it.isNotEmpty() }?.take(10) ?: trendingHitsTracks

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(liveTrending, key = { "trend_${it.id}" }) { track ->
                            val isLiked = likedTracks.any { it.id == track.id }
                            val isCurrent = currentTrack?.id == track.id && isPlaying

                            CardTrackItem(
                                track = track,
                                tag = "Single",
                                isOffline = downloadedTracks.any { it.id == track.id },
                                isLiked = isLiked,
                                isPlaying = isCurrent,
                                onCardClick = {
                                    playerViewModel.playTrack(track, liveTrending)
                                },
                                onLikeClick = {
                                    PlaylistRepository.toggleLikeTrack(track)
                                },
                                onMoreClick = {
                                    trackMenu(TrackMenuState(track = track))
                                }
                            )
                        }
                    }
                }
            }

            // 7. SECTION: "Recommended Stations" (Screenshots 3 & 4)
            if (selectedFilter != "Podcasts") {
                item(key = "recommended_stations_section") {
                    Text(
                        text = "Recommended Stations",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recommendedStations, key = { "st_${it.id}" }) { station ->
                            RadioStationCard(
                                station = station,
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "${station.title} is coming soon!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }

            // 8. SECTION: "Recents" (Screenshot 4)
            item(key = "recents_section") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recents",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Show all",
                        color = Color(0xFF8E8E93),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateToPlaylist("liked_songs") }
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Card 1: Liked Songs Card (Purple-indigo gradient)
                    item(key = "recents_liked_songs_card") {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
                            border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                            modifier = Modifier
                                .width(165.dp)
                                .clickable { onNavigateToPlaylist("liked_songs") }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                CollectionArtworkImage(
                                    tracks = likedTracks,
                                    placeholderIcon = Icons.Filled.Favorite,
                                    placeholderBackground = Color(0xFF4A148C),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(145.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Liked Songs",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1ED760))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${likedTracks.size.coerceAtLeast(3)} songs added",
                                        color = Color(0xFF8E8E93),
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: Blend Card
                    item(key = "recents_blend_card") {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
                            border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                            modifier = Modifier
                                .width(165.dp)
                                .clickable { onNavigateToPlaylist("blend") }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(145.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF222225)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.MusicNote,
                                        contentDescription = "Blend",
                                        tint = Color(0xFF55555A),
                                        modifier = Modifier.size(46.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Blend",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "A generated mix ba...",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Card 3+: Recently Played History if any
                    items(recentlyPlayedHistory.take(4), key = { "recent_${it.id}" }) { track ->
                        CardTrackItem(
                            track = track,
                            tag = "Single",
                            isOffline = downloadedTracks.any { it.id == track.id },
                            isLiked = likedTracks.any { it.id == track.id },
                            isPlaying = currentTrack?.id == track.id && isPlaying,
                            onCardClick = {
                                playerViewModel.playTrack(track, recentlyPlayedHistory)
                            },
                            onLikeClick = {
                                PlaylistRepository.toggleLikeTrack(track)
                            },
                            onMoreClick = {
                                trackMenu(TrackMenuState(track = track))
                            }
                        )
                    }
                }
            }

            // 9. SECTION: "Your favourite artists" (Screenshots 4 & 5)
            item(key = "favourite_artists_section") {
                Text(
                    text = "Your favourite artists",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp)
                )

                val liveArtists = (uiState as? HomeUiState.Success)?.data?.popularArtists?.takeIf { it.isNotEmpty() }?.map { art ->
                    FavoriteArtistItem(
                        id = art.id,
                        name = art.name,
                        image = art.image?.takeIf { it.isNotBlank() } ?: "https://cdn-images.dzcdn.net/images/artist/ac5350cff290edd5b69fa584b8b1bd4f/1000x1000-000000-80-0-0.jpg"
                    )
                } ?: favoriteArtists

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(liveArtists, key = { "art_${it.id}" }) { artist ->
                        val isFollowing = followedArtistIds.contains(artist.id)

                        ArtistFollowCard(
                            artist = artist,
                            isFollowing = isFollowing,
                            onCardClick = {
                                viewModel.selectArtistForPersonalization(artist.name)
                                onNavigateToArtist(artist.id)
                            },
                            onFollowToggle = {
                                followedArtistIds = if (isFollowing) {
                                    followedArtistIds - artist.id
                                } else {
                                    followedArtistIds + artist.id
                                }
                            }
                        )
                    }
                }
            }

            // 10. SECTION: "Popular albums and new releases" (Screenshot 5)
            item(key = "popular_albums_section") {
                Text(
                    text = "Popular albums and new releases",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp)
                )

                val liveAlbums = (uiState as? HomeUiState.Success)?.data?.recommendedAlbums?.takeIf { it.isNotEmpty() }?.map { alb ->
                    PopularAlbumRelease(
                        id = alb.id,
                        title = alb.name,
                        subtitle = "${alb.year ?: "2024"} • ${alb.artist ?: "Various Artists"}",
                        image = alb.images?.large ?: alb.images?.medium ?: "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/9a/2f/2a/9a2f2aa2-5343-7c13-e3d3-5fd6f79ea2fb/8909024071657.png/600x600bb.jpg",
                        query = alb.name
                    )
                } ?: popularAlbums

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(liveAlbums, key = { "alb_${it.id}" }) { album ->
                        PopularAlbumCard(
                            album = album,
                            onClick = {
                                onNavigateToSearch(album.query)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3D Featured Hero Carousel Card matching the user's screenshot.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCarouselCard(
    heroTracks: List<TrackDto>,
    currentPlayingTrack: TrackDto?,
    isPlaying: Boolean,
    onPlayTrack: (TrackDto) -> Unit,
    onTrackClick: (TrackDto) -> Unit
) {
    val actualCount = heroTracks.size
    val virtualMultiplier = 100_000
    val initialIndex = if (actualCount > 1) (virtualMultiplier / 2 * actualCount) + 1 else 0
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { if (actualCount > 1) virtualMultiplier * actualCount else actualCount }
    )

    // Smooth 3.5s Automatic Carousel Scrolling with loop
    LaunchedEffect(actualCount) {
        if (actualCount > 1) {
            while (true) {
                kotlinx.coroutines.delay(3500L)
                if (!pagerState.isScrollInProgress) {
                    try {
                        pagerState.animateScrollToPage(
                            page = pagerState.currentPage + 1,
                            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                        )
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val currentActualIndex = if (actualCount > 0) pagerState.currentPage % actualCount else 0

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 64.dp),
                pageSpacing = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) { page ->
                val trackIndex = if (actualCount > 0) page % actualCount else 0
                val track = heroTracks.getOrNull(trackIndex) ?: return@HorizontalPager
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                val scale = (1f - (pageOffset * 0.16f)).coerceIn(0.82f, 1f)
                val alpha = (1f - (pageOffset * 0.45f)).coerceIn(0.55f, 1f)
                val isCenter = page == pagerState.currentPage

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF202023))
                        .clickable {
                            if (isCenter) {
                                onTrackClick(track)
                            }
                        }
                ) {
                    val heroImg = track.images?.large?.takeIf { it.isNotBlank() }
                        ?: track.images?.medium?.takeIf { it.isNotBlank() }
                        ?: "https://i.scdn.co/image/ab67616d0000b27325fa2d19b2363a9520a34409"

                    // Clean, unobstructed artwork / thumbnail (no duplicate text overlay)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(heroImg)
                            .crossfade(true)
                            .build(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isCenter) {
                        val isThisPlaying = currentPlayingTrack?.id == track.id && isPlaying
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 14.dp, bottom = 14.dp)
                                .size(54.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(0xFF1ED760))
                                .clickable { onPlayTrack(track) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val activeTrack = heroTracks.getOrNull(currentActualIndex) ?: heroTracks.firstOrNull()
            
            // Clean Song Name displayed below the thumbnail
            Text(
                text = activeTrack?.title ?: "",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Clean Artist Name displayed below the title
            Text(
                text = activeTrack?.artist ?: "",
                color = Color(0xFF8E8E93),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroMetadataBadge(text = "Headphonix")
                HeroMetadataBadge(text = "Single")
                HeroMetadataBadge(text = "★ 5.0")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(actualCount) { index ->
                    if (index == currentActualIndex) {
                        Box(
                            modifier = Modifier
                                .size(width = 22.dp, height = 5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF1ED760))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4C4C50))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetadataBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF262629))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFD4D4D8),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Standard Card Track Item matching Screenshot 1, 2, 3:
 * Has artwork, Single badge, Title, Artist, and bottom row with Heart + 3-dots!
 */
@Composable
private fun CardTrackItem(
    track: TrackDto,
    tag: String = "Single",
    isOffline: Boolean = false,
    isLiked: Boolean = false,
    isPlaying: Boolean = false,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
        modifier = Modifier
            .width(165.dp)
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF222225))
            ) {
                val imageUrl = track.images?.medium?.takeIf { it.isNotBlank() }
                    ?: track.images?.large?.takeIf { it.isNotBlank() }
                    ?: track.images?.small?.takeIf { it.isNotBlank() }
                    ?: "https://i.scdn.co/image/ab67616d0000b27325fa2d19b2363a9520a34409"

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (isOffline) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xEE0B1E13))
                            .border(1.dp, Color(0xFF1ED760).copy(alpha = 0.65f), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowCircleDown,
                            contentDescription = "Offline",
                            tint = Color(0xFF1ED760),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Offline",
                            color = Color(0xFF1ED760),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1ED760)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.GraphicEq,
                                contentDescription = "Playing",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tag,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.title,
                color = if (isPlaying) Color(0xFF1ED760) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom row: Heart icon on left, 3-dots on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FavoriteHeartIcon(
                    isLiked = isLiked,
                    modifier = Modifier
                        .clickable { onLikeClick() },
                    iconSize = 19.dp,
                    unlikedColor = Color(0xFF8E8E93)
                )

                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier
                        .size(19.dp)
                        .clickable { onMoreClick() }
                )
            }
        }
    }
}

/**
 * Start Listening Track Row (Screenshots 1 & 2):
 * Album art + Title + Artist + 3-dots
 */
@Composable
private fun StartListeningTrackRow(
    track: TrackDto,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageUrl = track.images?.small?.takeIf { it.isNotBlank() }
            ?: track.images?.medium?.takeIf { it.isNotBlank() }
            ?: track.images?.large?.takeIf { it.isNotBlank() }
            ?: "https://c.saavncdn.com/905/Valentines-Special-Hindi-2026-20260122144848-500x500.jpg"

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF222225))
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) Color(0xFF1ED760) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = track.artist,
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Options",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Radio Station Card (Screenshots 3 & 4):
 * Gradient background, Spotify Radio badge, overlapping artist circles.
 */
@Composable
private fun RadioStationCard(
    station: StationItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(station.gradientColors))
                    .padding(12.dp)
            ) {
                // Top-Left: Radio Pill Badge
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_spotify),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RADIO",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Center: 3 Overlapping Artist Portraits
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    station.artistImages.forEachIndexed { idx, url ->
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .offset(x = if (idx == 0) 10.dp else if (idx == 2) (-10).dp else 0.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF282828))
                                .border(2.dp, Color(0xFF161619), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Bottom: Title Inside Card
                Text(
                    text = station.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = station.subtitle,
            color = Color(0xFF8E8E93),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Artist Card with Follow Toggle (Screenshots 4 & 5).
 */
@Composable
private fun ArtistFollowCard(
    artist: FavoriteArtistItem,
    isFollowing: Boolean,
    onCardClick: () -> Unit,
    onFollowToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
        modifier = Modifier
            .width(165.dp)
            .clickable(onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val artistImg = artist.image.takeIf { it.isNotBlank() }
                ?: "https://cdn-images.dzcdn.net/images/artist/ac5350cff290edd5b69fa584b8b1bd4f/1000x1000-000000-80-0-0.jpg"

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artistImg)
                    .crossfade(true)
                    .build(),
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(105.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222225))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = artist.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Artist",
                color = Color(0xFF8E8E93),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Following Capsule Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isFollowing) Color(0x1A1ED760) else Color(0xFF222225))
                    .border(
                        1.dp,
                        if (isFollowing) Color(0xFF1ED760).copy(alpha = 0.6f) else Color(0x33FFFFFF),
                        RoundedCornerShape(50)
                    )
                    .clickable { onFollowToggle() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFollowing) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color(0xFF1ED760),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        color = if (isFollowing) Color(0xFF1ED760) else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Popular Album Card (Screenshot 5).
 */
@Composable
private fun PopularAlbumCard(
    album: PopularAlbumRelease,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161619)),
        border = BorderStroke(1.dp, Color(0x14FFFFFF)),
        modifier = Modifier
            .width(165.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            val albumImg = album.image.takeIf { it.isNotBlank() }
                ?: "https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/9a/2f/2a/9a2f2aa2-5343-7c13-e3d3-5fd6f79ea2fb/8909024071657.png/600x600bb.jpg"

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(albumImg)
                    .crossfade(true)
                    .build(),
                contentDescription = album.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF222225))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = album.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = album.subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) Color.White else Color(0xFF262629))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
