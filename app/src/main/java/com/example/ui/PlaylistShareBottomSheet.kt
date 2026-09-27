package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Playlist
import com.example.data.remote.models.TrackDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistShareBottomSheet(
    playlist: Playlist,
    themeColor: Color,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isCollaboratorMode by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 2 })

    val shareText = remember(playlist, isCollaboratorMode) {
        if (isCollaboratorMode) {
            "Join and collaborate on my playlist '${playlist.title}' on Headphonix! https://headphonix.app/playlist/${playlist.id}?invite=true"
        } else {
            "Check out '${playlist.title}' by ${playlist.owner} on Headphonix: https://headphonix.app/playlist/${playlist.id}"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF161616),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.DarkGray,
                width = 36.dp,
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
            // Swipeable Share Cards Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                pageSpacing = 16.dp
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    themeColor.copy(alpha = 0.95f),
                                    themeColor.copy(alpha = 0.70f),
                                    Color(0xFF1E1E1E)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    if (page == 0) {
                        // Card 1: Single Large Cover Card
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(155.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CollectionArtworkImage(
                                    tracks = playlist.tracks,
                                    customCoverUrl = playlist.coverImage,
                                    contentDescription = playlist.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = playlist.title,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Playlist by ${playlist.owner}",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Headphonix branding row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
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
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    } else {
                        // Card 2: Multi-Track Preview Card
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Header: Small Cover + Title + Owner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CollectionArtworkImage(
                                        tracks = playlist.tracks,
                                        customCoverUrl = playlist.coverImage,
                                        contentDescription = playlist.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.title,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Playlist by ${playlist.owner}",
                                        color = Color.White.copy(alpha = 0.75f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Preview 3 tracks
                            val previewTracks = playlist.tracks.take(3)
                            if (previewTracks.isEmpty()) {
                                Text(
                                    text = "No tracks yet in this playlist",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    previewTracks.forEach { track ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.25f))
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val thumb = track.images?.small ?: track.images?.medium ?: track.images?.large
                                            AsyncImage(
                                                model = thumb,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.DarkGray)
                                            )

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.title,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = track.artist ?: "Unknown Artist",
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Logo at bottom right
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.headphonix_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Headphonix",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }
            }

            // Dot Indicator for page switching
            Row(
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(2) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.Gray)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                    )
                }
            }

            // "Invite as collaborator" Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Invite as collaborator",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Collaborators will be able to add songs",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = isCollaboratorMode,
                    onCheckedChange = { isCollaboratorMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1ED760),
                        uncheckedThumbColor = Color.LightGray,
                        uncheckedTrackColor = Color(0xFF333333)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Social / Share actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Copy link
                ShareTargetItem(
                    icon = Icons.Filled.Link,
                    label = "Copy link",
                    containerColor = Color(0xFF282828),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Headphonix Playlist Link", shareText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                // 2. WhatsApp
                ShareTargetItem(
                    icon = Icons.Filled.Send,
                    label = "WhatsApp",
                    containerColor = Color(0xFF25D366),
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                            `package` = "com.whatsapp"
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (e: Exception) {
                            val fallback = Intent.createChooser(
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                },
                                "Share via WhatsApp"
                            )
                            context.startActivity(fallback)
                        }
                    }
                )

                // 3. Stories
                ShareTargetItem(
                    icon = Icons.Filled.CameraAlt,
                    label = "Stories",
                    containerColor = Color(0xFFE1306C),
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share to Stories"))
                    }
                )

                // 4. SMS
                ShareTargetItem(
                    icon = Icons.Filled.Chat,
                    label = "SMS",
                    containerColor = Color(0xFF282828),
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("sms:")
                            putExtra("sms_body", shareText)
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (e: Exception) {
                            val chooser = Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                },
                                "Share via SMS"
                            )
                            context.startActivity(chooser)
                        }
                    }
                )

                // 5. More (System Chooser)
                ShareTargetItem(
                    icon = Icons.Filled.MoreHoriz,
                    label = "More",
                    containerColor = Color(0xFF282828),
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Playlist"))
                    }
                )
            }
        }
    }
}

@Composable
private fun ShareTargetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    containerColor: Color,
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
                .background(containerColor),
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
