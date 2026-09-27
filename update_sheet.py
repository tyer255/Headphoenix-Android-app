import sys

with open('app/src/main/java/com/example/TrackOptionsSheet.kt', 'r') as f:
    content = f.read()

content = content.replace('import com.example.data.remote.models.TrackDto', 'import com.example.data.remote.models.TrackDto\nimport com.example.data.AppDownloadManager\nimport com.example.data.DownloadState\nimport androidx.compose.material.icons.filled.Download\nimport androidx.compose.material.icons.filled.DownloadDone\n')
content = content.replace('val context = LocalContext.current', 'val context = LocalContext.current\n    val downloadStatusMap by AppDownloadManager.downloadStatus.collectAsState()\n    val downloadState = downloadStatusMap[track.id]')

add_code = """
                item {
                    ListItem(
                        headlineContent = { 
                            val text = when (downloadState) {
                                is DownloadState.Downloading -> "Downloading (${(downloadState.progress * 100).toInt()}%)"
                                is DownloadState.Completed -> "Downloaded"
                                is DownloadState.Failed -> "Download Failed"
                                else -> "Download"
                            }
                            Text(text, color = Color.White) 
                        },
                        leadingContent = { 
                            if (downloadState is DownloadState.Downloading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF1DB954), strokeWidth = 2.dp)
                            } else if (downloadState is DownloadState.Completed) {
                                Icon(Icons.Filled.DownloadDone, contentDescription = null, tint = Color(0xFF1DB954))
                            } else {
                                Icon(Icons.Filled.Download, contentDescription = null, tint = Color.LightGray)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable {
                            if (downloadState == null || downloadState is DownloadState.Failed) {
                                AppDownloadManager.downloadTrack(context, track)
                            }
                        }
                    )
                }
"""

content = content.replace('            LazyColumn {\n                item {', '            LazyColumn {' + add_code + '                item {')

with open('app/src/main/java/com/example/TrackOptionsSheet.kt', 'w') as f:
    f.write(content)
