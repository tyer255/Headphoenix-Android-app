import re

with open('app/src/main/java/com/example/TrackOptionsSheet.kt', 'r') as f:
    content = f.read()

# Restore the original header without the Spotify Code
find_header = '''            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spotify Code + Album Art (like real Spotify)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (track.spotifyUri != null && track.spotifyUri.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = track.images?.large ?: track.images?.medium ?: track.images?.small,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(Color.DarkGray)
                            )
                            AsyncImage(
                                model = "https://scannables.scdn.co/uri/plain/jpeg/000000/white/640/${track.spotifyUri}",
                                contentDescription = "Spotify Code",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(40.dp)
                                    .background(Color.Black)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = track.images?.large ?: track.images?.medium ?: track.images?.small,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = track.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = track.artist ?: "Unknown Artist", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }'''

replace_header = '''            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.images?.small ?: track.images?.medium ?: track.images?.large,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = track.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = track.artist ?: "Unknown Artist", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }'''

content = content.replace(find_header, replace_header)

# Add showCodeDialog state
content = content.replace('val downloadState = downloadStatusMap[track.id]', 'val downloadState = downloadStatusMap[track.id]\n    var showCodeDialog by remember { mutableStateOf(false) }')

# Add Spotify Code list item
find_share_item = '''                item {
                    ListItem(
                        headlineContent = { Text("Share", color = Color.White) },'''

replace_share_item = '''                if (track.spotifyUri != null && track.spotifyUri.isNotEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("Show Spotify Code", color = Color.White) },
                            leadingContent = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = Color.LightGray) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showCodeDialog = true
                            }
                        )
                    }
                }
                item {
                    ListItem(
                        headlineContent = { Text("Share", color = Color.White) },'''

content = content.replace(find_share_item, replace_share_item)

# Add QrCodeScanner icon import if not present
if "import androidx.compose.material.icons.filled.QrCodeScanner" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.DownloadDone", "import androidx.compose.material.icons.filled.DownloadDone\nimport androidx.compose.material.icons.filled.QrCodeScanner")


dialog_code = '''
        if (showCodeDialog) {
            AlertDialog(
                onDismissRequest = { showCodeDialog = false },
                confirmButton = {
                    TextButton(onClick = { showCodeDialog = false }) {
                        Text("Close", color = Color(0xFF1DB954))
                    }
                },
                containerColor = Color(0xFF282828),
                title = { Text("Spotify Code", color = Color.White) },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AsyncImage(
                            model = track.images?.large ?: track.images?.medium ?: track.images?.small,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.DarkGray)
                        )
                        AsyncImage(
                            model = "https://scannables.scdn.co/uri/plain/jpeg/000000/white/640/${track.spotifyUri}",
                            contentDescription = "Spotify Code",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .width(200.dp)
                                .height(50.dp)
                                .background(Color.Black)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scan this code to play the track", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            )
        }
'''

content = content.replace('    ) {\n        Column(', '    ) {\n' + dialog_code + '\n        Column(')

with open('app/src/main/java/com/example/TrackOptionsSheet.kt', 'w') as f:
    f.write(content)

