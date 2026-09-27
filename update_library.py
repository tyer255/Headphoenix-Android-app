import re

with open('app/src/main/java/com/example/LibraryScreen.kt', 'r') as f:
    content = f.read()

imports = """import com.example.data.AppDownloadManager
import androidx.compose.ui.platform.LocalContext
"""
content = content.replace('import com.example.data.PlaylistRepository', 'import com.example.data.PlaylistRepository\n' + imports)

states = """
    val context = LocalContext.current
    val downloadedTracks = AppDownloadManager.getDownloadedTracks(context)
"""
content = content.replace('val likedTracks by PlaylistRepository.likedTracks.collectAsState()', 'val likedTracks by PlaylistRepository.likedTracks.collectAsState()' + states)

# Now, we need to show the "Downloaded Tracks" tile in LibraryScreen when selectedFilter == "All" or "Downloaded"
# Let's find the `// Liked Songs Tile` and insert `// Downloaded Songs Tile`

liked_tile_grid = '''                // Liked Songs Tile
                item {
                    LibraryGridItem(
                        title = "Liked Songs",
                        subtitle = "Playlist • ${likedTracks.size} songs",
                        color = Color(0xFF5E35B1),
                        isLiked = true,
                        onClick = { onNavigateToPlaylist("liked_songs") }
                    )
                }'''

downloaded_tile_grid = '''                // Downloaded Songs Tile
                if (selectedFilter == "All" || selectedFilter == "Downloaded") {
                    item {
                        LibraryGridItem(
                            title = "Downloaded",
                            subtitle = "Playlist • ${downloadedTracks.size} songs",
                            color = Color(0xFF0F9D58),
                            isLiked = false,
                            onClick = { onNavigateToPlaylist("downloaded") }
                        )
                    }
                }'''

content = content.replace(liked_tile_grid, liked_tile_grid + "\n" + downloaded_tile_grid)


liked_tile_list = '''                // Liked Songs Tile
                item {
                    LibraryListItem(
                        title = "Liked Songs",
                        subtitle = "Playlist • ${likedTracks.size} songs",
                        color = Color(0xFF5E35B1),
                        isLiked = true,
                        onClick = { onNavigateToPlaylist("liked_songs") }
                    )
                }'''

downloaded_tile_list = '''                // Downloaded Songs Tile
                if (selectedFilter == "All" || selectedFilter == "Downloaded") {
                    item {
                        LibraryListItem(
                            title = "Downloaded",
                            subtitle = "Playlist • ${downloadedTracks.size} songs",
                            color = Color(0xFF0F9D58),
                            isLiked = false,
                            onClick = { onNavigateToPlaylist("downloaded") }
                        )
                    }
                }'''

content = content.replace(liked_tile_list, liked_tile_list + "\n" + downloaded_tile_list)


with open('app/src/main/java/com/example/LibraryScreen.kt', 'w') as f:
    f.write(content)
