package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesSheet(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val devices by playerViewModel.devices.collectAsState()
    val currentDevice by playerViewModel.currentDevice.collectAsState()
    val volume by playerViewModel.volume.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.SpeakerGroup, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Connect to a device", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Listen to your music on other devices", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Devices list
            devices.forEach { device ->
                val isCurrent = device.id == currentDevice.id
                val icon: ImageVector = when (device.type) {
                    "Computer" -> Icons.Filled.Laptop
                    "Speaker" -> Icons.Filled.Speaker
                    else -> Icons.Filled.Smartphone
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            playerViewModel.selectDevice(device)
                        }
                        .background(if (isCurrent) Color(0x221DB954) else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = device.name,
                        tint = if (isCurrent) GreenPrimary else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            device.name,
                            color = if (isCurrent) GreenPrimary else Color.White,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                        if (isCurrent) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Spotify Connect • Active", color = GreenPrimary, fontSize = 12.sp)
                            }
                        } else {
                            Text("Available", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    if (isCurrent) {
                        Icon(Icons.Filled.Check, contentDescription = "Active", tint = GreenPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume Slider in Connect Sheet
            HorizontalDivider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { playerViewModel.toggleMute() }) {
                    Icon(
                        imageVector = if (volume <= 0f) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color.White
                    )
                }
                Slider(
                    value = volume,
                    onValueChange = { playerViewModel.setVolume(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = GreenPrimary,
                        inactiveTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(volume * 100).toInt()}%",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
