cat app/src/main/java/com/example/MainScreen.kt | sed '/val drawerState = rememberDrawerState/i\
    var globalTrackMenuFor by remember { mutableStateOf<com.example.data.remote.models.TrackDto?>(null) }\
    var globalAddToPlaylistFor by remember { mutableStateOf<com.example.data.remote.models.TrackDto?>(null) }\
    var globalCreatePlaylistFor by remember { mutableStateOf<com.example.data.remote.models.TrackDto?>(null) }\
    var showCreatePlaylistSheetOnly by remember { mutableStateOf(false) }\
    val savedTracks by playerViewModel.savedTracks.collectAsState()\
    val playlists by com.example.data.PlaylistRepository.playlists.collectAsState()\
' > app/src/main/java/com/example/MainScreen.tmp

# Now we need to wrap Scaffold in CompositionLocalProvider...
