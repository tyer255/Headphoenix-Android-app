package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PlaylistRepository
import kotlinx.coroutines.delay

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onPlaylistCreated: (String) -> Unit
) {
    val existingPlaylistsCount = PlaylistRepository.playlists.collectAsState().value.size
    val defaultName = "My playlist #${existingPlaylistsCount + 1}"
    var playlistName by remember { mutableStateOf(defaultName) }
    
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF404040),
                            Color(0xFF242424),
                            Color(0xFF121212)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Give your playlist a name",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(64.dp))
                
                BasicTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            innerTextField()
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = Color(0xCC737373),
                                thickness = 1.5.dp,
                                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(0.8f)
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(56.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.width(300.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x661A1A1A)
                        ),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    
                    Button(
                        onClick = {
                            val name = if (playlistName.isBlank()) defaultName else playlistName
                            val newPl = PlaylistRepository.createPlaylist(name = name, description = "")
                            onPlaylistCreated(newPl.id)
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DB954),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Create", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
