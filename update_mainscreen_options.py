import re

with open("app/src/main/java/com/example/MainScreen.kt", "r") as f:
    content = f.read()

old_call = """            TrackOptionsSheet(
                track = globalTrackMenuFor!!.track,
                isSaved = savedTracks.contains(globalTrackMenuFor!!.track.id),
                onDismiss = { globalTrackMenuFor = null },
                onToggleSave = { PlaylistRepository.toggleLikeTrack(globalTrackMenuFor!!.track) },
                onAddToPlaylist = { globalAddToPlaylistFor = globalTrackMenuFor!!.track; globalTrackMenuFor = null },
                onAddToQueue = { playerViewModel.addToQueue(globalTrackMenuFor!!.track); globalTrackMenuFor = null },
                onViewArtist = { artistId -> 
                    globalTrackMenuFor = null
                    navController.navigate("artist/$artistId")
                },
                onRemoveFromPlaylist = globalTrackMenuFor!!.onRemoveFromPlaylist
            )"""

new_call = """            TrackOptionsSheet(
                track = globalTrackMenuFor!!.track,
                isSaved = savedTracks.contains(globalTrackMenuFor!!.track.id),
                onDismiss = { globalTrackMenuFor = null },
                onToggleSave = { PlaylistRepository.toggleLikeTrack(globalTrackMenuFor!!.track) },
                onAddToPlaylist = { globalAddToPlaylistFor = globalTrackMenuFor!!.track; globalTrackMenuFor = null },
                onAddToQueue = { playerViewModel.addToQueue(globalTrackMenuFor!!.track); globalTrackMenuFor = null },
                onViewArtist = { artistId -> 
                    globalTrackMenuFor = null
                    navController.navigate("artist/$artistId")
                },
                onViewAlbum = { albumId -> 
                    globalTrackMenuFor = null
                    navController.navigate("album/$albumId")
                },
                onRemoveFromPlaylist = globalTrackMenuFor!!.onRemoveFromPlaylist
            )"""

content = content.replace(old_call, new_call)

with open("app/src/main/java/com/example/MainScreen.kt", "w") as f:
    f.write(content)
