package com.example

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Playlist

@Composable
fun WebsiteSidebar(
    currentRoute: String?,
    playlists: List<Playlist>,
    likedTracksCount: Int,
    downloadedTracksCount: Int,
    onNavigate: (String) -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = Color(0xFF1E1E1E), // Dark grey
        drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            
            // Header Profile Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigate("profile")
                        onCloseDrawer()
                    }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF64B5F6)), // Light blue
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        color = Color.Black,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Guest Account",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "View profile",
                        color = Color(0xFFAAAAAA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 0.dp),
                thickness = 1.dp,
                color = Color(0xFF333333)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Menu Items
            MobileSidebarNavItem(
                icon = Icons.Outlined.Add,
                label = "Add account",
                onClick = { /* No-op */ }
            )
            MobileSidebarNavItem(
                icon = Icons.Outlined.History,
                label = "Recents",
                onClick = { 
                    onNavigate("history")
                    onCloseDrawer() 
                }
            )
            MobileSidebarNavItem(
                icon = Icons.Outlined.Notifications,
                label = "Your Updates",
                onClick = { /* No-op */ }
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
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
