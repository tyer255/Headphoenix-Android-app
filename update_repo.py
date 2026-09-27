import re

with open('app/src/main/java/com/example/data/PlaylistRepository.kt', 'r') as f:
    content = f.read()

find = '''    fun getPlaylistById(id: String): Playlist? {
        if (id == "liked_songs") {
            return Playlist(
                id = "liked_songs",
                title = "Liked Songs",
                description = "Your favorite tracks",
                coverImage = "",
                owner = _userProfile.value.name,
                tracks = _likedTracks.value,
                isCustom = false,
                colorHex = 0xFF5E35B1
            )
        }
        return _playlists.value.find { it.id == id }
    }'''

replace = '''    fun getPlaylistById(id: String): Playlist? {
        if (id == "liked_songs") {
            return Playlist(
                id = "liked_songs",
                title = "Liked Songs",
                description = "Your favorite tracks",
                coverImage = "",
                owner = _userProfile.value.name,
                tracks = _likedTracks.value,
                isCustom = false,
                colorHex = 0xFF5E35B1
            )
        }
        if (id == "downloaded") {
            // Need a context to get downloaded tracks, but we can't get context here easily.
            // Let's assume AppDownloadManager has a static list or we can get it from application context.
            // Wait, PlaylistRepository can't access Context.
            return null // we will handle this in PlaylistScreen instead!
        }
        return _playlists.value.find { it.id == id }
    }'''
content = content.replace(find, replace)
with open('app/src/main/java/com/example/data/PlaylistRepository.kt', 'w') as f:
    f.write(content)
