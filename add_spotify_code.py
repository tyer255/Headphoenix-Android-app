import re

with open('app/src/main/java/com/example/TrackOptionsSheet.kt', 'r') as f:
    content = f.read()

find_header = '''        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.images?.small ?: track.images?.medium,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = track.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = track.artist ?: "Unknown Artist", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }'''

replace_header = '''        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
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

content = content.replace(find_header, replace_header)

with open('app/src/main/java/com/example/TrackOptionsSheet.kt', 'w') as f:
    f.write(content)
