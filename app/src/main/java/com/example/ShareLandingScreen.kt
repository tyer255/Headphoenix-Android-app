package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShareLandingScreen(
    shareType: String,
    shareId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEntity: (String, String) -> Unit
) {
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1500)
        loading = false
        onNavigateToEntity(shareType, shareId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color(0xFF1DB954))
        }
    }
}
