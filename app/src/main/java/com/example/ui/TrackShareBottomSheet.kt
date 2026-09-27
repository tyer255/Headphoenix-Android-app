package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.remote.LyricsResolver
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackShareBottomSheet(
    track: TrackDto,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Pager for [0: Song Card, 1: Lyrics Card]
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Theme background colors
    val themeOptions = listOf(
        Color(0xFF1F2430), // 0: Pattern / Indigo
        Color(0xFF382928), // 1: Cocoa / Warm Rust
        Color(0xFF181818), // 2: Deep Charcoal
        Color(0xFF1E332A)  // 3: Dark Pine Emerald
    )
    var selectedThemeIndex by remember { mutableStateOf(0) }

    // Live or resolved lyrics
    var lyricsText by remember {
        mutableStateOf(
            "tere ishq da\njam haseen ae\nsubha haseen, meri\nsham haseen ae"
        )
    }
    var showEditLyricsDialog by remember { mutableStateOf(false) }

    // Fetch live lyrics if available
    LaunchedEffect(track) {
        try {
            val lyricsDto = LyricsResolver.resolveLyrics(track)
            if (lyricsDto != null && lyricsDto.lines.isNotEmpty()) {
                val lines = lyricsDto.lines.map { it.text.trim() }.filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    lyricsText = lines.take(4).joinToString("\n")
                }
            } else if (lyricsDto?.plainLyrics?.isNotBlank() == true) {
                val plainLines = lyricsDto.plainLyrics.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (plainLines.isNotEmpty()) {
                    lyricsText = plainLines.take(4).joinToString("\n")
                }
            }
        } catch (e: Exception) {
            // Keep default fallback
        }
    }

    val artworkUrl = track.images?.large ?: track.images?.medium ?: track.images?.small ?: ""
    val shareLink = "https://spotiz.app/track/${track.id}"
    val currentCardTheme = themeOptions.getOrElse(selectedThemeIndex) { Color(0xFF1F2430) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.DarkGray,
                width = 38.dp,
                height = 4.dp
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp, top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. SWIPEABLE SHARE CARDS (HorizontalPager)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(370.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 16.dp
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        currentCardTheme.copy(alpha = 0.95f),
                                        currentCardTheme,
                                        Color(0xFF121212)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        if (page == 0) {
                            // CARD 1: SONG CARD (Matches Screenshot_20260920_153601.jpg)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Spacer(modifier = Modifier.height(4.dp))

                                // Centered Artwork
                                Box(
                                    modifier = Modifier
                                        .size(175.dp)
                                        .shadow(12.dp, RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (artworkUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = artworkUrl,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }

                                // Song title and Artist
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = track.title,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = track.artist ?: "Unknown Artist",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Headphonix Branding
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.headphonix_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Headphonix",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        } else {
                            // CARD 2: LYRICS CARD (Matches Screenshot_20260920_153606.jpg)
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Header: Small Cover + Track Title + "Song • Artist"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.DarkGray)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Song • ${track.artist ?: "Unknown Artist"}",
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Large Bold Lyrics Text
                                Text(
                                    text = lyricsText,
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                )

                                // Headphonix Branding
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.headphonix_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Headphonix",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. PILL TOGGLE: [ Song ] | [ Lyrics ]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF282828))
                    .padding(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSong = pagerState.currentPage == 0

                    // Song button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSong) Color.White else Color.Transparent)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Song",
                            color = if (isSong) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Lyrics button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isSong) Color.White else Color.Transparent)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lyrics",
                            color = if (!isSong) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. THEME CIRCLES (Song tab) OR [ ✎ Edit lyrics ] BUTTON (Lyrics tab)
            if (pagerState.currentPage == 0) {
                // Color Theme circles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    themeOptions.forEachIndexed { index, color ->
                        val isSelected = selectedThemeIndex == index
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedThemeIndex = index }
                        )
                    }

                    // Edit style icon circle
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF282828))
                            .clickable {
                                selectedThemeIndex = (selectedThemeIndex + 1) % themeOptions.size
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit style",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                // Edit lyrics pill button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF282828))
                        .clickable { showEditLyricsDialog = true }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit lyrics",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. SOCIAL SHARING ACTIONS ROW
            val fullShareMessage = if (pagerState.currentPage == 0) {
                "Listen to \"${track.title}\" by ${track.artist} on Headphonix:\n$shareLink"
            } else {
                "\"$lyricsText\"\n\n— \"${track.title}\" by ${track.artist}\n$shareLink"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy link
                ShareButton(
                    icon = Icons.Filled.Link,
                    label = "Copy link",
                    color = Color(0xFF282828),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Headphonix Share Link", shareLink)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                // WhatsApp
                ShareButton(
                    icon = Icons.Filled.Send,
                    label = "WhatsApp",
                    color = Color(0xFF25D366),
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, fullShareMessage)
                            type = "text/plain"
                            `package` = "com.whatsapp"
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (e: Exception) {
                            val chooser = Intent.createChooser(
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, fullShareMessage)
                                    type = "text/plain"
                                },
                                "Share via WhatsApp"
                            )
                            context.startActivity(chooser)
                        }
                    }
                )

                // Stories
                ShareButton(
                    icon = Icons.Filled.CameraAlt,
                    label = "Stories",
                    color = Color(0xFFE1306C),
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, fullShareMessage)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share to Stories"))
                    }
                )

                // SMS
                ShareButton(
                    icon = Icons.Filled.Chat,
                    label = "SMS",
                    color = Color(0xFF282828),
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("sms:")
                            putExtra("sms_body", fullShareMessage)
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (e: Exception) {
                            val chooser = Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, fullShareMessage)
                                },
                                "Share via SMS"
                            )
                            context.startActivity(chooser)
                        }
                    }
                )

                // More / System Chooser
                ShareButton(
                    icon = Icons.Filled.MoreHoriz,
                    label = "Status",
                    color = Color(0xFF282828),
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, fullShareMessage)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                    }
                )
            }
        }
    }

    // Custom Lyrics Editor Dialog
    if (showEditLyricsDialog) {
        var tempLyrics by remember { mutableStateOf(lyricsText) }
        AlertDialog(
            onDismissRequest = { showEditLyricsDialog = false },
            containerColor = Color(0xFF242424),
            title = {
                Text("Edit Lyrics Snippet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                OutlinedTextField(
                    value = tempLyrics,
                    onValueChange = { tempLyrics = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF1ED760),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        lyricsText = tempLyrics.trim()
                        showEditLyricsDialog = false
                    }
                ) {
                    Text("Save", color = Color(0xFF1ED760), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditLyricsDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
private fun ShareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
