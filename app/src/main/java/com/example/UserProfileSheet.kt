package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaylistRepository
import com.example.ui.theme.GreenPrimary

@Composable
fun UserProfileDrawerContent(
    onClose: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val profile by PlaylistRepository.userProfile.collectAsState()
    
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF191414), // Spotify dark background
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header / Profile Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        onClose()
                        onNavigate("profile")
                    }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.firstOrNull()?.toString()?.uppercase() ?: "U",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = profile.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "View profile",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
            
            // Drawer Items
            DrawerItem(
                icon = Icons.Outlined.AccountCircle,
                label = "Account",
                onClick = { 
                    onClose()
                    onNavigate("account")
                }
            )
            
            DrawerItem(
                icon = Icons.Outlined.Add,
                label = "Add or switch account",
                onClick = { 
                    // Left as per user instructions
                    android.widget.Toast.makeText(context, "Switch account functionality coming soon", android.widget.Toast.LENGTH_SHORT).show() 
                }
            )
            
            DrawerItem(
                icon = Icons.Outlined.FlashOn,
                label = "What's new",
                onClick = { 
                    onClose()
                    onNavigate("whatsnew")
                }
            )
            
            DrawerItem(
                icon = Icons.Outlined.History,
                label = "Listening history",
                onClick = { 
                    onClose()
                    onNavigate("history")
                }
            )
            
            DrawerItem(
                icon = Icons.Outlined.Settings,
                label = "Settings and privacy",
                onClick = { 
                    onClose()
                    onNavigate("settings")
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            
            DrawerItem(
                icon = null, // No icon for log out in many drawer menus, or we can use one
                label = "Log out",
                onClick = { 
                    onClose()
                    android.widget.Toast.makeText(context, "Logged out", android.widget.Toast.LENGTH_SHORT).show() 
                },
                textColor = Color(0xFFFF5252) // Optional: red for logout or white
            )
        }
    }
}

@Composable
fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    onClick: () -> Unit,
    textColor: Color = Color.White
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.LightGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
