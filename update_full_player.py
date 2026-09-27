import re

with open('app/src/main/java/com/example/FullPlayerScreen.kt', 'r') as f:
    content = f.read()

# 1. Imports
imports = """import com.example.data.AppDownloadManager
import com.example.data.DownloadState
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import com.example.data.MusicRepository
import androidx.compose.ui.platform.LocalContext
"""
content = content.replace('import com.example.data.PlaybackHistoryRepository', 'import com.example.data.PlaybackHistoryRepository\n' + imports)

# 2. Add local state inside FullPlayerScreen
states = """
    val context = LocalContext.current
    val downloadStatusMap by AppDownloadManager.downloadStatus.collectAsState()
    val downloadState = track?.let { downloadStatusMap[it.id] }
    
    var artistImage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(track?.artist) {
        val artistId = track?.artistId
        if (artistId != null && artistId.isNotEmpty()) {
            val cached = MusicRepository.artistCache[artistId]
            if (cached?.image != null && cached.image.isNotEmpty()) {
                artistImage = cached.image
            } else {
                try {
                    val repo = MusicRepository()
                    val data = repo.getArtist(artistId)
                    if (data?.image != null) {
                        artistImage = data.image
                    }
                } catch (e: Exception) {}
            }
        }
    }
"""
content = content.replace('val isShuffle by viewModel.isShuffle.collectAsState()', 'val isShuffle by viewModel.isShuffle.collectAsState()' + states)

# 3. Add fast download icon left of the heart icon
heart_row = """
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { track?.let { AppDownloadManager.downloadTrack(context, it) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (downloadState is DownloadState.Downloading) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { downloadState.progress },
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF1DB954),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "${(downloadState.progress * 100).toInt()}",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (downloadState is DownloadState.Completed) {
                            Icon(
                                imageVector = Icons.Filled.DownloadDone,
                                contentDescription = "Downloaded",
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Download",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { track?.let { viewModel.toggleSave(it) } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Save",
                            tint = if (isSaved) Color(0xFF1DB954) else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
"""

# Replace the existing single IconButton for heart with the row
# Careful, we need to find the right spot.
# In FullPlayerScreen:
#                 IconButton(
#                     onClick = { track?.let { viewModel.toggleSave(it) } },
#                     modifier = Modifier.size(36.dp)
#                 ) {
#                     Icon(
#                         imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
#                         contentDescription = "Save",
#                         tint = if (isSaved) Color(0xFF1DB954) else Color.White,
#                         modifier = Modifier.size(28.dp)
#                     )
#                 }
#             }
#             Spacer(modifier = Modifier.height(16.dp))

find_heart = '''                IconButton(
                    onClick = { track?.let { viewModel.toggleSave(it) } },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Save",
                        tint = if (isSaved) Color(0xFF1DB954) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }'''

content = content.replace(find_heart, heart_row.strip())

# 4. Replace artist image in "About the artist"
find_artist_image = '''                        AsyncImage(
                            model = track?.images?.large ?: track?.images?.medium,
                            contentDescription = track?.artist,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )'''

replace_artist_image = '''                        AsyncImage(
                            model = artistImage ?: track?.images?.large ?: track?.images?.medium,
                            contentDescription = track?.artist,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )'''

content = content.replace(find_artist_image, replace_artist_image)

with open('app/src/main/java/com/example/FullPlayerScreen.kt', 'w') as f:
    f.write(content)
