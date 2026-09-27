package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    var currentPage by remember { mutableStateOf("home") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    BackHandler {
        if (isSearching) {
            isSearching = false
        } else if (currentPage != "home") {
            currentPage = "home"
        } else {
            onNavigateBack()
        }
    }

    if (isSearching) {
        SettingsSearchPage(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onClose = { isSearching = false },
            onNavigate = {
                currentPage = it
                isSearching = false
                searchQuery = ""
            }
        )
        return
    }

    when (currentPage) {
        "home" -> SettingsHome(
            onNavigate = { currentPage = it },
            onGlobalBack = onNavigateBack,
            onSearch = { isSearching = true }
        )
        "account" -> SettingsSubpage("Account", { currentPage = "home" }) {
            SettingsCategory("Profile")
            SettingsRow("Username", "user123")
            SettingsRow("Email", "user@example.com")
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Close Account", color = Color.White)
            }
        }
        "content-display" -> SettingsSubpage("Content and display", { currentPage = "home" }) {
            SettingsCategory("Visuals")
            SettingsToggleItem("Canvas", "Show short, looping visuals on tracks", true)
            SettingsCategory("Languages")
            SettingsRow("Languages for music", "English, Hindi")
        }
        "privacy" -> SettingsSubpage("Privacy and social", { currentPage = "home" }) {
            SettingsCategory("Social")
            SettingsToggleItem("Private session", "Temporarily hide your listening activity", false)
            SettingsToggleItem("Listening activity", "Share what I listen to with my followers", true)
            SettingsToggleItem("Recently played artists", "Show my recently played artists on my public profile", true)
        }
        "playback" -> SettingsSubpage("Playback", { currentPage = "home" }) {
            SettingsCategory("Audio")
            SettingsToggleItem("Gapless playback", "Allows gapless playback", true)
            SettingsToggleItem("Autoplay", "Enjoy nonstop listening", true)
            SettingsToggleItem("Normalize volume", "Set the same volume level for all songs", true)
        }
        "notifications" -> SettingsSubpage("Notifications", { currentPage = "home" }) {
            SettingsCategory("Messages")
            SettingsToggleItem("Push Notifications", "Receive push notifications for new releases", true)
            SettingsToggleItem("Email Notifications", "Receive emails about your account", false)
        }
        "apps-devices" -> SettingsSubpage("Apps and devices", { currentPage = "home" }) {
            SettingsCategory("Connected")
            SettingsRow("Spotify Connect", "Control playback on other devices")
        }
        "data-saving" -> SettingsSubpage("Data-saving and offline", { currentPage = "home" }) {
            SettingsCategory("Data Saver")
            SettingsToggleItem("Data Saver", "Sets audio quality to low and hides Canvas", false)
            SettingsToggleItem("Download using cellular", "Allow downloads over cellular network", false)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                Text("Clear cache", color = Color.Black)
            }
        }
        "media-quality" -> SettingsSubpage("Media quality", { currentPage = "home" }) {
            SettingsCategory("Streaming quality")
            SettingsRow("Wi-Fi streaming", "Auto")
            SettingsRow("Cellular streaming", "Auto")
            SettingsCategory("Download quality")
            SettingsRow("Download", "Normal")
        }
        "advertisements" -> SettingsSubpage("Advertisements", { currentPage = "home" }) {
            SettingsCategory("Ads")
            SettingsToggleItem("Tailored ads", "Show personalized ads", true)
        }
        "hide-songs" -> SettingsSubpage("Hide Songs", { currentPage = "home" }) {
            SettingsCategory("Hidden Tracks")
            Text("You haven't hidden any songs yet.", color = Color(0xFFA7A7A7), fontSize = 14.sp, modifier = Modifier.padding(16.dp))
        }
        "about" -> SettingsSubpage("About and support", { currentPage = "home" }) {
            SettingsCategory("Information")
            SettingsRow("Version", "1.0.0")
            SettingsRow("Privacy Policy", "")
            SettingsRow("Terms of Service", "")
        }
        else -> SettingsHome(
            onNavigate = { currentPage = it },
            onGlobalBack = onNavigateBack,
            onSearch = { isSearching = true }
        )
    }
}

@Composable
fun SettingsSubpage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsHome(onNavigate: (String) -> Unit, onGlobalBack: () -> Unit, onSearch: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGlobalBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color.White)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Free account", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onNavigate("account") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ED760)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Premium Active", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("All high-fidelity audio & offline capabilities enabled", color = Color(0xFF4ADE80), fontSize = 12.sp)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsBox("Data Saver mode", "Always off", Icons.Filled.BarChart, false, Modifier.weight(1f))
                    SettingsBox("Private session", "Off", Icons.Filled.VisibilityOff, false, Modifier.weight(1f))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsNavigationRow(Icons.Filled.Person, "Account", "Username • Close account") { onNavigate("account") }
                SettingsNavigationRow(Icons.Filled.MusicNote, "Content and display", "Canvas • Languages for music") { onNavigate("content-display") }
                SettingsNavigationRow(Icons.Filled.Lock, "Privacy and social", "Private session • Public playlists") { onNavigate("privacy") }
                SettingsNavigationRow(Icons.Filled.VolumeUp, "Playback", "Gapless playback • Autoplay") { onNavigate("playback") }
                SettingsNavigationRow(Icons.Filled.Notifications, "Notifications", "Push • Email") { onNavigate("notifications") }
                SettingsNavigationRow(Icons.Filled.Devices, "Apps and devices", "Spotify Connect control") { onNavigate("apps-devices") }
                SettingsNavigationRow(Icons.Filled.ArrowDropDownCircle, "Data-saving and offline", "Data Saver mode • Downloads") { onNavigate("data-saving") }
                SettingsNavigationRow(Icons.Filled.HighQuality, "Media quality", "Wi-Fi streaming quality") { onNavigate("media-quality") }
                SettingsNavigationRow(Icons.Filled.Tv, "Advertisements", "Tailored ads") { onNavigate("advertisements") }
                SettingsNavigationRow(Icons.Filled.VisibilityOff, "Hide Songs", "Hidden tracks • Unhide songs") { onNavigate("hide-songs") }
                SettingsNavigationRow(Icons.Filled.Info, "About and support", "Version • Privacy Policy") { onNavigate("about") }
                
                Spacer(modifier = Modifier.height(32.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Log out", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun SettingsBox(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF242424))
            .clickable { }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = if (active) Color(0xFF4ADE80) else Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFFA7A7A7), fontSize = 11.sp)
        }
    }
}

@Composable
fun SettingsNavigationRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = Color(0xFFA7A7A7), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SettingsSearchPage(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
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
                    .background(Color(0xFF242424), CircleShape),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFFA7A7A7), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text("Search settings", color = Color(0xFFA7A7A7), fontSize = 14.sp)
                            }
                            innerTextField()
                        }
                    )
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFFA7A7A7), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        val categories = listOf(
            Triple("account", "Account", "Username, Email, Profile"),
            Triple("content-display", "Content and display", "Canvas, Theme"),
            Triple("privacy", "Privacy and social", "Private session"),
            Triple("playback", "Playback", "Gapless playback, Autoplay"),
            Triple("notifications", "Notifications", "Push, Email"),
            Triple("apps-devices", "Apps and devices", "Current device"),
            Triple("data-saving", "Data-saving and offline", "Data Saver, Storage"),
            Triple("media-quality", "Media quality", "Streaming quality"),
            Triple("advertisements", "Advertisements", "Ad preferences"),
            Triple("hide-songs", "Hide Songs", "Hidden tracks"),
            Triple("about", "About and support", "Version, Privacy")
        )

        val filtered = if (query.isEmpty()) emptyList() else categories.filter {
            it.second.contains(query, ignoreCase = true) || it.third.contains(query, ignoreCase = true)
        }

        if (query.isNotEmpty() && filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 64.dp), contentAlignment = Alignment.TopCenter) {
                Text("No results found for \"$query\"", color = Color(0xFFA7A7A7), fontSize = 16.sp)
            }
        } else {
            LazyColumn {
                items(filtered.size) { index ->
                    val item = filtered[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(item.first) }
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Column {
                            Text(item.second, color = Color.White, fontSize = 16.sp)
                            Text(item.third, color = Color(0xFFA7A7A7), fontSize = 14.sp)
                        }
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
        fontSize = 18.sp,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 16.sp)
        Text(value, color = Color(0xFFA7A7A7), fontSize = 14.sp)
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = Color(0xFFA7A7A7), fontSize = 14.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF1DB954),
                uncheckedThumbColor = Color(0xFFA7A7A7),
                uncheckedTrackColor = Color(0xFF282828)
            )
        )
    }
}
