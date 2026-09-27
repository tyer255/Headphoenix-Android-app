package com.example

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlaylistRepository
import com.example.ui.theme.GreenPrimary

@Composable
fun PremiumScreen() {
    val context = LocalContext.current
    val profile by PlaylistRepository.userProfile.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = com.example.ui.LocalBottomContentPadding.current)
    ) {
        item {
            HeroSection()
        }
        item {
            PremiumActiveCard()
        }
        item {
            WhyJoinSection()
        }
        item {
            AvailablePlansSection()
        }
    }
}

@Composable
fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3B1252),
                        Color(0xFF1E0E32),
                        Color.Black
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Premium", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Listen without limits. Unlimited Lifetime Access with Headphonix.",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xE6171717))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Permanent Active Entitlement", color = Color(0xFFE5E5E5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Unlimited Lifetime Access", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Permanent Unlimited Access. Your account has lifetime Headphonix Premium enabled with no expiration date. All features unlocked.",
                color = Color(0xFFA3A3A3),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun PremiumActiveCard() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x66064E3B), // emerald-900/40
                        Color(0xFF181818),
                        Color(0xFF181818)
                    )
                )
            )
            .border(1.dp, Color(0x8010B981), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Premium Active", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x3310B981))
                        .border(1.dp, Color(0x8010B981), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("UNLIMITED LIFETIME", color = Color(0xFF34D399), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(start = 40.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AllInclusive, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Unlimited Lifetime Access", color = Color(0xFFD1FAE5), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Never Expires · No renewal or payment required", color = Color(0xFFA3A3A3), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun WhyJoinSection() {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF181818))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Text("Why join Premium Standard?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(24.dp))

        WhyJoinItem(Icons.Filled.VolumeUp, "Ad-free music listening")
        WhyJoinItem(Icons.Filled.Shuffle, "Play songs in any order")
        WhyJoinItem(Icons.Filled.Headset, "Very high audio quality")
        WhyJoinItem(Icons.Filled.People, "Listen with friends in real time")
        WhyJoinItem(Icons.Filled.Download, "Download to listen offline")
        WhyJoinItem(Icons.Filled.VideoLibrary, "Watch videos with fewer ads")
    }
}

@Composable
fun WhyJoinItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFE5E5E5), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AvailablePlansSection() {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth()
    ) {
        Text("Available plans", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))

        // Standard Plan
        PlanCard(
            badge = "Unlimited for Lifetime",
            badgeBg = Color(0xFF34D399),
            badgeColor = Color.Black,
            planName = "Standard",
            planColor = Color(0xFF34D399),
            subtitle = "Lifetime Premium Access",
            caption = "Never Expires · Permanent Active Account",
            features = listOf(
                "1 Standard verified account",
                "Download to listen offline",
                "Very high audio quality (up to ~320kbps)",
                "Subscribe or one-time payment: Permanent Lifetime"
            ),
            button1Text = "Active Forever",
            button1Bg = Color(0xFF10B981),
            button1Color = Color.Black,
            button2Text = "Permanent Entitlement",
            footerText = "Your Headphonix Premium account is active permanently. Terms apply.",
            isBorderAnimated = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Platinum Plan
        PlanCard(
            planName = "Platinum",
            planColor = Color(0xFFFDE047),
            subtitle = "Unlimited Lossless Audio",
            features = listOf(
                "Up to 3 Platinum accounts",
                "Download to listen offline",
                "Lossless audio quality (up to ~24-bit/44.1kHz)",
                "Mix your playlists & Smart DJ blends",
                "Custom playlist creation & Connect your DJ software",
                "Included in your Unlimited Account"
            ),
            button1Text = "Included with Premium",
            button1Bg = Color(0xFFFDE047),
            button1Color = Color.Black
        )
    }
}

@Composable
fun PlanCard(
    badge: String? = null,
    badgeBg: Color = Color.Transparent,
    badgeColor: Color = Color.Black,
    planName: String,
    planColor: Color,
    subtitle: String,
    caption: String? = null,
    features: List<String>,
    button1Text: String,
    button1Bg: Color,
    button1Color: Color,
    button2Text: String? = null,
    footerText: String? = null,
    isBorderAnimated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition()
    val borderColor by infiniteTransition.animateColor(
        initialValue = Color.White.copy(alpha = 0.1f),
        targetValue = if (isBorderAnimated) Color(0x8010B981) else Color.White.copy(alpha = 0.1f),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF181818))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(badge, color = badgeColor, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color(0xFFD4D4D4), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Premium", color = Color(0xFFD4D4D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(planName, color = planColor, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        
        if (caption != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(caption, color = Color(0xFFA3A3A3), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        features.forEach { feature ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Text("•", color = Color(0xFFA3A3A3), fontSize = 14.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(feature, color = Color(0xFFE5E5E5), fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = button1Bg),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(button1Text, color = button1Color, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }

        if (button2Text != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(50))
            ) {
                Text(button2Text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }

        if (footerText != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                footerText,
                color = Color(0xFFA3A3A3),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
