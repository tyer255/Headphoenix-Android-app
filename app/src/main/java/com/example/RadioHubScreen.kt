package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radio
import com.example.data.remote.models.TrackDto

@Composable
fun RadioHubScreen(
    onNavigateToRadioStation: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()

    val dummyStations = listOf(
        Pair("1", "A.R. Rahman Radio"),
        Pair("2", "Arijit Singh Radio"),
        Pair("3", "Pritam Radio"),
        Pair("4", "Shreya Ghoshal Radio")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(288.dp)
                .background(Color(0xFF181818))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF1DB954).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Radio, contentDescription = "Radio", tint = Color(0xFF1DB954))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("RADIO HUB", color = Color(0xFF1DB954), fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Non-stop music.",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 52.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Endless personalized stations based on your favorite artists and songs.",
                    fontSize = 18.sp,
                    color = Color(0xFFA7A7A7)
                )
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Text("Popular Stations", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Curated stations featuring top artists", color = Color(0xFFA7A7A7), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                items(dummyStations) { station ->
                    Column(
                        modifier = Modifier.width(160.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(Color(0xFF282828))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(station.second, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Artist Radio", color = Color(0xFFA7A7A7), fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(com.example.ui.LocalBottomContentPadding.current))
        }
    }
}
