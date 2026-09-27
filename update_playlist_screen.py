import re

with open('app/src/main/java/com/example/PlaylistScreen.kt', 'r') as f:
    content = f.read()

imports = """import com.example.data.AppDownloadManager
"""
content = content.replace('import com.example.data.PlaylistRepository', 'import com.example.data.PlaylistRepository\n' + imports)

# We find `var playlist by remember(playlistId, playlists, likedTracks) {`
find = '''    var playlist by remember(playlistId, playlists, likedTracks) {
        mutableStateOf(PlaylistRepository.getPlaylistById(playlistId))
    }'''

replace = '''    val downloadedTracks = AppDownloadManager.getDownloadedTracks(context)
    var playlist by remember(playlistId, playlists, likedTracks, downloadedTracks) {
        if (playlistId == "downloaded") {
            mutableStateOf(
                Playlist(
                    id = "downloaded",
                    title = "Downloaded",
                    description = "Songs available offline",
                    coverImage = null,
                    trackCount = downloadedTracks.size,
                    owner = "You",
                    tracks = downloadedTracks,
                    colorHex = 0xFF0F9D58,
                    isCustom = false
                )
            )
        } else {
            mutableStateOf(PlaylistRepository.getPlaylistById(playlistId))
        }
    }'''

content = content.replace(find, replace)

# Update icon
find_icon = '''                            if (currentPl.id == "liked_songs") {
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = "Liked Songs",
                                    tint = Color.White,
                                    modifier = Modifier.size(80.dp)
                                )
                            } else if (!currentPl.coverImage.isNullOrEmpty()) {'''

replace_icon = '''                            if (currentPl.id == "liked_songs") {
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = "Liked Songs",
                                    tint = Color.White,
                                    modifier = Modifier.size(80.dp)
                                )
                            } else if (currentPl.id == "downloaded") {
                                Icon(
                                    Icons.Filled.DownloadDone,
                                    contentDescription = "Downloaded",
                                    tint = Color.White,
                                    modifier = Modifier.size(80.dp)
                                )
                            } else if (!currentPl.coverImage.isNullOrEmpty()) {'''

content = content.replace(find_icon, replace_icon)

with open('app/src/main/java/com/example/PlaylistScreen.kt', 'w') as f:
    f.write(content)
