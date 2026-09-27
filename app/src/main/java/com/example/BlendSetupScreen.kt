package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlendSetupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit
) {
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "Create a Blend",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(end = 48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .width(208.dp)
                .height(128.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .align(Alignment.CenterEnd)
                    .border(4.dp, Color(0xFF121212), CircleShape)
                    .background(Color(0xFF3E3E3E), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color(0xFFA7A7A7), modifier = Modifier.size(48.dp))
            }
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .align(Alignment.CenterStart)
                    .border(4.dp, Color(0xFF121212), CircleShape)
                    .background(Color(0xFF4285F4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = Color.Black, fontSize = 48.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Invite friends to Blend",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Invite up to 10 friends to a Blend, a shared playlist that gives you social recommendations based on all of your music tastes.",
            color = Color(0xFFA7A7A7),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                loading = true
                coroutineScope.launch {
                    delay(1000)
                    loading = false
                    onNavigateToPlaylist("blend-1")
                }
            },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, disabledContainerColor = Color.Gray),
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(48.dp).padding(horizontal = 32.dp)
        ) {
            Text(if (loading) "Creating..." else "Invite", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
