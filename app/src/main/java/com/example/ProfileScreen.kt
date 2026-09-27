package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Blue gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF285C8D), Color(0xFF121212)),
                            startY = 0f,
                            endY = 800f
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                // Profile Header (Left Aligned)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64B5F6)), // Light blue
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Guest Account", 
                            color = Color.White, 
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("0", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(" followers • ", color = Color(0xFFAAAAAA), fontSize = 13.sp, fontWeight = FontWeight.Normal)
                            Text("0", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(" following", color = Color(0xFFAAAAAA), fontSize = 13.sp, fontWeight = FontWeight.Normal)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Actions
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                            .clickable { /* No-op */ }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Edit", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Icon(
                        Icons.Outlined.Settings, 
                        contentDescription = "Settings", 
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Icon(
                        Icons.Filled.MoreVert, 
                        contentDescription = "More", 
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(64.dp))
                
                // Content Cards (Placeholder illustrations)
                // In screenshot: 3 slightly rotated cards
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    // Left Card
                    Box(
                        modifier = Modifier
                            .offset(x = (-70).dp, y = (-10).dp)
                            .size(90.dp, 100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222222))
                            .rotate(-10f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(android.R.drawable.ic_media_play), contentDescription = null, tint = Color(0xFF666666))
                    }
                    
                    // Right Card
                    Box(
                        modifier = Modifier
                            .offset(x = 70.dp, y = 10.dp)
                            .size(90.dp, 100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222222))
                            .rotate(10f),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(android.R.drawable.ic_menu_myplaces), contentDescription = null, tint = Color(0xFF666666))
                    }
                    
                    // Center Card
                    Box(
                        modifier = Modifier
                            .offset(y = 10.dp)
                            .size(90.dp, 100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(android.R.drawable.ic_menu_view), contentDescription = null, tint = Color(0xFF888888))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Share what you love",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Turn on playlist and artist sharing on your profile so others can discover what you're into.",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .clickable { /* No-op */ }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text("Manage settings", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
