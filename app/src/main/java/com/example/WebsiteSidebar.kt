package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Playlist
import com.example.data.PlaylistRepository

@Composable
fun WebsiteSidebar(
    currentRoute: String?,
    playlists: List<Playlist>,
    likedTracksCount: Int,
    downloadedTracksCount: Int,
    onNavigate: (String) -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    onOpenCookies: () -> Unit = {},
    onOpenMediaEngine: () -> Unit = {},
    onCloseDrawer: () -> Unit
) {
    val userProfile by PlaylistRepository.userProfile.collectAsState()

    ModalDrawerSheet(
        modifier = Modifier
            .width(310.dp)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF1F1F1F),
        drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Profile Section (Spotify Android Left Drawer Header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate("profile")
                        onCloseDrawer()
                    }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1ED760)),
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
                        Text(
                            text = userProfile.name.take(1).uppercase(),
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = userProfile.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "View profile",
                        color = Color(0xFFA7A7A7),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Spotify Android Profile Drawer Menu Items
            MobileSidebarNavItem(
                icon = Icons.Outlined.PersonAddAlt,
                label = "Add account",
                onClick = {
                    onNavigate("account")
                    onCloseDrawer()
                }
            )
            MobileSidebarNavItem(
                icon = Icons.Outlined.Bolt,
                label = "What's new",
                onClick = {
                    onNavigate("whatsnew")
                    onCloseDrawer()
                }
            )
            MobileSidebarNavItem(
                icon = Icons.Outlined.History,
                label = "Listening history",
                onClick = {
                    onNavigate("history")
                    onCloseDrawer()
                }
            )
            MobileSidebarNavItem(
                icon = Icons.Outlined.Key,
                label = "Authorized cookies",
                onClick = {
                    onOpenCookies()
                    onCloseDrawer()
                }
            )
            MobileSidebarNavItem(
                icon = Icons.Outlined.Tune,
                label = "Media engine & setup",
                onClick = {
                    onOpenMediaEngine()
                    onCloseDrawer()
                }
            )
            MobileSidebarNavItem(
                icon = Icons.Outlined.Settings,
                label = "Settings and privacy",
                onClick = {
                    onNavigate("settings")
                    onCloseDrawer()
                }
            )
        }
    }
}

@Composable
fun MobileSidebarNavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
