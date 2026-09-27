package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BlendInviteScreen(
    blendId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit
) {
    var scene by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(1000)
        scene = 1
        delay(2000)
        scene = 2
        delay(2500)
        scene = 3
    }

    val matchPercentage = 85

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F2EA))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = scene == 1,
                enter = fadeIn(tween(700))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Your taste\nmatch is $matchPercentage%",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 40.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "You two are relationship goals.",
                        fontSize = 20.sp,
                        color = Color(0xFF4B4B4B)
                    )
                }
            }

            AnimatedVisibility(
                visible = scene == 2,
                enter = fadeIn(tween(700))
            ) {
                Text(
                    "Get ready for a\nmix as unique as\nthe two of you.",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 40.sp
                )
            }

            AnimatedVisibility(
                visible = scene == 3,
                enter = fadeIn(tween(700))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "You + Creator",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        "$matchPercentage% taste match",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1DB954),
                        modifier = Modifier.padding(bottom = 48.dp)
                    )
                    Button(
                        onClick = { onNavigateToPlaylist(blendId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF121212)),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
                    ) {
                        Text("Go to your Blend", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
