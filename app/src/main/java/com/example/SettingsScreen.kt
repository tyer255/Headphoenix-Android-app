package com.example

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaylistRepository

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf("home") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    // Persistent interactive Spotify settings states
    var dataSaverEnabled by remember { mutableStateOf(false) }
    var privateSessionEnabled by remember { mutableStateOf(false) }
    var crossfadeSeconds by remember { mutableFloatStateOf(4f) }
    var gaplessPlayback by remember { mutableStateOf(true) }
    var automixEnabled by remember { mutableStateOf(true) }
    var allowExplicit by remember { mutableStateOf(true) }
    var showUnplayable by remember { mutableStateOf(false) }
    var normalizeVolume by remember { mutableStateOf(true) }
    var volumeLevel by remember { mutableStateOf("Normal") }
    var monoAudio by remember { mutableStateOf(false) }
    var wifiQuality by remember { mutableStateOf("Very high") }
    var cellularQuality by remember { mutableStateOf("Automatic") }
    var downloadQuality by remember { mutableStateOf("High") }
    var autoAdjustQuality by remember { mutableStateOf(true) }
    var cacheSizeMb by remember { mutableFloatStateOf(342f) }

    BackHandler {
        if (isSearching) {
            isSearching = false
        } else if (currentPage != "home") {
            currentPage = "home"
        } else {
            onNavigateBack()
        }
    }

    val navState = if (isSearching) "search" else currentPage

    AnimatedContent(
        targetState = navState,
        transitionSpec = {
            if (targetState == "home") {
                (slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(220))).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(180))
                )
            } else {
                (slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(220))).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(180))
                )
            }
        },
        label = "settingsPageTransition"
    ) { page ->
        when (page) {
            "search" -> SettingsSearchPage(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClose = { isSearching = false },
                onNavigate = {
                    currentPage = it
                    isSearching = false
                    searchQuery = ""
                }
            )
            "home" -> SettingsHome(
                onNavigate = { currentPage = it },
                onGlobalBack = onNavigateBack,
                onSearch = { isSearching = true },
                dataSaverEnabled = dataSaverEnabled,
                onToggleDataSaver = {
                    dataSaverEnabled = !dataSaverEnabled
                    Toast.makeText(
                        context,
                        if (dataSaverEnabled) "Data Saver turned on" else "Data Saver turned off",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                privateSessionEnabled = privateSessionEnabled,
                onTogglePrivateSession = {
                    privateSessionEnabled = !privateSessionEnabled
                    Toast.makeText(
                        context,
                        if (privateSessionEnabled) "Private session started" else "Private session ended",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            "account" -> SettingsSubpage("Account", { currentPage = "home" }) {
                SettingsCategory("Account details")
                SettingsRow("Username", "headphonix_user")
                SettingsRow("Email", "music@headphonix.app")
                SettingsRow("Subscription", "Premium Individual")
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Account management opened", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282828)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Manage Subscription", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            "content-display" -> SettingsSubpage("Content and display", { currentPage = "home" }) {
                SettingsCategory("Visuals")
                SettingsToggleItem("Canvas", "Display short, looping visuals on tracks", true)
                SettingsToggleItem("Allow explicit content", "Turn on to play explicit content (labeled with E tag)", allowExplicit) {
                    allowExplicit = it
                }
                SettingsToggleItem("Show unplayable songs", "Show songs that are unplayable in your playlists", showUnplayable) {
                    showUnplayable = it
                }
                SettingsCategory("Languages")
                SettingsRow("App language", "System default (English)")
                SettingsRow("Languages for music", "Hindi, Punjabi, English")
            }
            "privacy" -> SettingsSubpage("Privacy and social", { currentPage = "home" }) {
                SettingsCategory("Listening activity")
                SettingsToggleItem(
                    "Private session",
                    "Temporarily hide your listening activity from followers. Ends automatically after 6 hours.",
                    privateSessionEnabled
                ) { privateSessionEnabled = it }
                SettingsToggleItem("Listening activity", "Share what I listen to with my followers on Spotify", true)
                SettingsToggleItem("Recently played artists", "Show my recently played artists on my public profile", true)
            }
            "playback" -> SettingsSubpage("Playback", { currentPage = "home" }) {
                SettingsCategory("Transitions")
                SpotifyCrossfadeSliderSection(
                    seconds = crossfadeSeconds,
                    onSecondsChange = { crossfadeSeconds = it }
                )
                SettingsToggleItem("Gapless playback", "Allows gapless playback between tracks without silence", gaplessPlayback) {
                    gaplessPlayback = it
                }
                SettingsToggleItem("Automix", "Allow smooth transitions between songs in curated playlists", automixEnabled) {
                    automixEnabled = it
                }
                SettingsCategory("Volume & Audio")
                SettingsToggleItem("Normalize volume", "Set the same volume level for all tracks", normalizeVolume) {
                    normalizeVolume = it
                }
                SpotifyPillSelectorRow(
                    title = "Volume level",
                    subtitle = "Adjust the environment volume target",
                    options = listOf("Quiet", "Normal", "Loud"),
                    selected = volumeLevel,
                    onSelect = { volumeLevel = it }
                )
                SettingsToggleItem("Mono audio", "Makes the left and right speakers play the same audio", monoAudio) {
                    monoAudio = it
                }
                SettingsToggleItem("Autoplay", "Keep on listening to similar tracks when your music ends", true)
            }
            "notifications" -> SettingsSubpage("Notifications", { currentPage = "home" }) {
                SettingsCategory("Channels")
                SettingsToggleItem("New Music & Releases", "Push notifications for new albums from artists you follow", true)
                SettingsToggleItem("Playlist Updates", "Notify when playlists you follow add new songs", true)
                SettingsToggleItem("Concert & Live Events", "Alerts for live shows near your location", false)
            }
            "apps-devices" -> SettingsSubpage("Devices", { currentPage = "home" }) {
                SettingsCategory("Connect")
                SettingsToggleItem("Show local devices only", "Only show devices on your local Wi-Fi menu", true)
                SettingsToggleItem("Spotify Connect in background", "Allow Spotify Connect to keep running when app is in background", true)
            }
            "data-saving" -> SettingsSubpage("Data-saving and storage", { currentPage = "home" }) {
                SettingsCategory("Data Saver")
                SettingsToggleItem(
                    "Data Saver",
                    "Sets audio quality to low (equivalent to 24 kbit/s) and disables artist canvases",
                    dataSaverEnabled
                ) { dataSaverEnabled = it }
                SettingsCategory("Offline & Downloads")
                SettingsToggleItem("Download using cellular", "Allow downloading playlists when not connected to Wi-Fi", false)
                SettingsCategory("Storage")
                SpotifyStorageSection(
                    cacheSizeMb = cacheSizeMb,
                    onClearCache = {
                        cacheSizeMb = 12f
                        Toast.makeText(context, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            "media-quality" -> SettingsSubpage("Audio Quality", { currentPage = "home" }) {
                SettingsCategory("Streaming quality")
                SpotifyQualitySelectorCard(
                    title = "Wi-Fi streaming",
                    selected = wifiQuality,
                    onSelect = { wifiQuality = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                SpotifyQualitySelectorCard(
                    title = "Cellular streaming",
                    selected = cellularQuality,
                    onSelect = { cellularQuality = it }
                )
                SettingsToggleItem(
                    "Auto-adjust quality",
                    "Recommended setting: We adjust your audio quality when your internet bandwidth is slow",
                    autoAdjustQuality
                ) { autoAdjustQuality = it }
                SettingsCategory("Downloads")
                SpotifyQualitySelectorCard(
                    title = "Download quality",
                    selected = downloadQuality,
                    options = listOf("Normal", "High", "Very high"),
                    onSelect = { downloadQuality = it }
                )
            }
            "advertisements" -> SettingsSubpage("Advertisements", { currentPage = "home" }) {
                SettingsCategory("Ad Preferences")
                SettingsToggleItem("Tailored ads", "Allow personalized audio and display experience", true)
            }
            "hide-songs" -> SettingsSubpage("Hidden Songs", { currentPage = "home" }) {
                SettingsCategory("Hidden Tracks")
                Text(
                    "Songs you hide in playlists won't play automatically.",
                    color = Color(0xFFB3B3B3),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            "about" -> SettingsSubpage("About", { currentPage = "home" }) {
                SettingsCategory("Application")
                SettingsRow("Version", "8.9.84.594 (Spotify UI Engine)")
                SettingsRow("Audio Engine", "ExoPlayer + YouTube IFrame Bridge")
                SettingsRow("Terms and Conditions of Use", "")
                SettingsRow("Privacy Policy", "")
                SettingsRow("Third-Party Software Licenses", "")
            }
            else -> SettingsHome(
                onNavigate = { currentPage = it },
                onGlobalBack = onNavigateBack,
                onSearch = { isSearching = true },
                dataSaverEnabled = dataSaverEnabled,
                onToggleDataSaver = { dataSaverEnabled = !dataSaverEnabled },
                privateSessionEnabled = privateSessionEnabled,
                onTogglePrivateSession = { privateSessionEnabled = !privateSessionEnabled }
            )
        }

        if (currentPage == "cookies") {
            CookiesManagementModal(onDismiss = { currentPage = "home" })
        }
        if (currentPage == "media-engine") {
            MediaEngineSetupDialog(onDismiss = { currentPage = "home" })
        }
    }
}

@Composable
fun SettingsSubpage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF181818))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 64.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsHome(
    onNavigate: (String) -> Unit,
    onGlobalBack: () -> Unit,
    onSearch: () -> Unit,
    dataSaverEnabled: Boolean = false,
    onToggleDataSaver: () -> Unit = {},
    privateSessionEnabled: Boolean = false,
    onTogglePrivateSession: () -> Unit = {}
) {
    val userProfile by PlaylistRepository.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF181818))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGlobalBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Settings",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Official Spotify Profile Banner Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("account") }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1ED760), Color(0xFF117A37))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile.name.firstOrNull()?.uppercase() ?: "H",
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1ED760))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "PREMIUM",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View profile", color = Color(0xFFB3B3B3), fontSize = 13.sp)
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFB3B3B3)
                    )
                }
            }

            // Quick Action Tiles (Data Saver & Private Session)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SettingsBox(
                        title = "Data Saver",
                        subtitle = if (dataSaverEnabled) "On" else "Off",
                        icon = Icons.Filled.DataSaverOn,
                        active = dataSaverEnabled,
                        onClick = onToggleDataSaver,
                        modifier = Modifier.weight(1f)
                    )
                    SettingsBox(
                        title = "Private session",
                        subtitle = if (privateSessionEnabled) "Active" else "Off",
                        icon = Icons.Filled.Lock,
                        active = privateSessionEnabled,
                        onClick = onTogglePrivateSession,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsNavigationRow(Icons.Filled.Person, "Account", "Username • Subscription • Email") { onNavigate("account") }
                SettingsNavigationRow(Icons.Filled.Key, "Authorized cookies", "Manage session credentials • Speed booster") { onNavigate("cookies") }
                SettingsNavigationRow(Icons.Filled.Tune, "Media engine & setup", "Native MediaCodec • Updates • Codecs") { onNavigate("media-engine") }
                SettingsNavigationRow(Icons.Filled.GraphicEq, "Content and display", "Canvas • Explicit filter • Languages") { onNavigate("content-display") }
                SettingsNavigationRow(Icons.Filled.Tune, "Playback", "Crossfade • Gapless playback • Automix") { onNavigate("playback") }
                SettingsNavigationRow(Icons.Filled.HighQuality, "Audio Quality", "Wi-Fi & cellular streaming • Downloads") { onNavigate("media-quality") }
                SettingsNavigationRow(Icons.Filled.Lock, "Privacy and social", "Private session • Listening activity") { onNavigate("privacy") }
                SettingsNavigationRow(Icons.Filled.Storage, "Data-saving and storage", "Data Saver • Storage bar • Clear cache") { onNavigate("data-saving") }
                SettingsNavigationRow(Icons.Filled.Devices, "Devices", "Connect to a device • Local Wi-Fi") { onNavigate("apps-devices") }
                SettingsNavigationRow(Icons.Filled.Notifications, "Notifications", "Push • New releases") { onNavigate("notifications") }
                SettingsNavigationRow(Icons.Filled.VisibilityOff, "Hidden Songs", "Manage hidden tracks") { onNavigate("hide-songs") }
                SettingsNavigationRow(Icons.Filled.Info, "About", "Version • Audio engine • Terms") { onNavigate("about") }

                Spacer(modifier = Modifier.height(28.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "logoutScale")
                    Button(
                        onClick = { onGlobalBack() },
                        interactionSource = interactionSource,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                        modifier = Modifier.scale(scale)
                    ) {
                        Text("Log out", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Interactive Spotify Crossfade Slider (`0s` to `12s`) matching `PlayingBar.scss` `.progress-bar` & `.slider-active`.
 */
@Composable
fun SpotifyCrossfadeSliderSection(
    seconds: Float,
    onSecondsChange: (Float) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    val progress = (seconds / 12f).coerceIn(0f, 1f)

    val activeLineColor by animateColorAsState(
        targetValue = if (isDragging || seconds > 0f) Color(0xFF1ED760) else Color.White,
        animationSpec = tween(150),
        label = "crossfadeColor"
    )
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 18.dp else 14.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "crossfadeThumb"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Crossfade", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Allows you to crossfade between songs",
                    color = Color(0xFFB3B3B3),
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (seconds > 0f) Color(0x261ED760) else Color(0xFF282828))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (seconds.toInt() == 0) "Off" else "${seconds.toInt()} s",
                    color = if (seconds > 0f) Color(0xFF1ED760) else Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Off", color = Color(0xFFB3B3B3), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val newProg = (offset.x / size.width).coerceIn(0f, 1f)
                            onSecondsChange(kotlin.math.round(newProg * 12f))
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val newProg = (offset.x / size.width).coerceIn(0f, 1f)
                                onSecondsChange(kotlin.math.round(newProg * 12f))
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val newProg = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSecondsChange(kotlin.math.round(newProg * 12f))
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val widthPx = constraints.maxWidth.toFloat()
                val thumbRadPx = with(LocalDensity.current) { (thumbSize / 2).toPx() }

                // Inactive track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF4D4D4D))
                )
                // Active track
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(activeLineColor)
                )
                // Thumb
                val thumbOffsetDp = with(LocalDensity.current) {
                    ((progress * widthPx) - thumbRadPx).coerceIn(0f, (widthPx - thumbRadPx * 2).coerceAtLeast(0f)).toDp()
                }
                Box(
                    modifier = Modifier
                        .offset(x = thumbOffsetDp)
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Text("12 s", color = Color(0xFFB3B3B3), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SpotifyPillSelectorRow(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, color = Color(0xFFB3B3B3), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSel = option == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSel) Color(0xFF1ED760) else Color(0xFF282828))
                        .clickable { onSelect(option) }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = option,
                        color = if (isSel) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SpotifyQualitySelectorCard(
    title: String,
    selected: String,
    options: List<String> = listOf("Automatic", "Low", "Normal", "High", "Very high"),
    onSelect: (String) -> Unit
) {
    val descriptions = mapOf(
        "Automatic" to "Depends on your network connection",
        "Low" to "Equivalent to ~24 kbit/s",
        "Normal" to "Equivalent to ~96 kbit/s",
        "High" to "Equivalent to ~160 kbit/s",
        "Very high" to "Equivalent to ~320 kbit/s"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF181818))
            .padding(14.dp)
    ) {
        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { opt ->
            val isSel = opt == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(opt) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = opt,
                        color = if (isSel) Color(0xFF1ED760) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                    )
                    descriptions[opt]?.let { desc ->
                        Text(desc, color = Color(0xFFB3B3B3), fontSize = 12.sp)
                    }
                }
                if (isSel) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF1ED760),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpotifyStorageSection(
    cacheSizeMb: Float,
    onClearCache: () -> Unit
) {
    val animatedCacheFraction by animateFloatAsState(
        targetValue = (cacheSizeMb / 2000f).coerceIn(0.02f, 0.35f),
        animationSpec = tween(400),
        label = "cacheBarFraction"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Multi-segment Spotify storage bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF282828))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.28f)
                    .background(Color(0xFF1ED760))
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedCacheFraction)
                    .background(Color(0xFFF59B23))
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF1ED760)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Downloads", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("512 MB", color = Color(0xFFB3B3B3), fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59B23)))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cache", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("${cacheSizeMb.toInt()} MB", color = Color(0xFFB3B3B3), fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181818))
                .clickable { onClearCache() }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Clear cache", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "You can free up storage by clearing your cache. Your downloads won't be removed.",
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Color(0xFF727272), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Clear", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsBox(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (active) Color(0xFF1B2E22) else Color(0xFF181818),
        animationSpec = tween(200),
        label = "settingsBoxBg"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                1.dp,
                if (active) Color(0xFF1ED760).copy(alpha = 0.5f) else Color(0xFF282828),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) Color(0xFF1ED760) else Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    color = if (active) Color(0xFF1ED760) else Color(0xFFB3B3B3),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SettingsNavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = Color(0xFFB3B3B3), fontSize = 13.sp)
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF727272),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSearchPage(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFF181818))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
                    .height(40.dp)
                    .background(Color(0xFF282828), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFB3B3B3), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color(0xFF1ED760)),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text("Search settings", color = Color(0xFFB3B3B3), fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFFB3B3B3), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        val categories = listOf(
            Triple("account", "Account", "Username, Email, Subscription"),
            Triple("content-display", "Content and display", "Canvas, Explicit content, Languages"),
            Triple("playback", "Playback", "Crossfade, Gapless playback, Automix, Volume"),
            Triple("media-quality", "Audio Quality", "Wi-Fi & cellular streaming quality, Downloads"),
            Triple("privacy", "Privacy and social", "Private session, Listening activity"),
            Triple("data-saving", "Data-saving and storage", "Data Saver, Storage bar, Clear cache"),
            Triple("apps-devices", "Devices", "Spotify Connect, Local devices"),
            Triple("notifications", "Notifications", "Push notifications, Releases"),
            Triple("hide-songs", "Hidden Songs", "Hidden tracks in playlists"),
            Triple("about", "About", "Version, Audio engine, Terms")
        )

        val filtered = if (query.isEmpty()) categories else categories.filter {
            it.second.contains(query, ignoreCase = true) || it.third.contains(query, ignoreCase = true)
        }

        if (query.isNotEmpty() && filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp), contentAlignment = Alignment.TopCenter) {
                Text("No settings found for \"$query\"", color = Color(0xFFB3B3B3), fontSize = 16.sp)
            }
        } else {
            LazyColumn {
                items(filtered.size) { index ->
                    val item = filtered[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(item.first) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.second, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                            Text(item.third, color = Color(0xFFB3B3B3), fontSize = 13.sp)
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF727272)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 6.dp)
    )
}

@Composable
fun SettingsRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 15.5.sp)
        if (value.isNotEmpty()) {
            Text(value, color = Color(0xFFB3B3B3), fontSize = 14.sp)
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF727272)
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    initialValue: Boolean,
    onValueChanged: ((Boolean) -> Unit)? = null
) {
    var checked by remember(initialValue) { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                checked = !checked
                onValueChanged?.invoke(checked)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, color = Color(0xFFB3B3B3), fontSize = 13.sp, lineHeight = 17.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onValueChanged?.invoke(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1ED760),
                uncheckedThumbColor = Color(0xFFB3B3B3),
                uncheckedTrackColor = Color(0xFF535353),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
