import re

with open("app/src/main/java/com/example/SearchScreen.kt", "r") as f:
    content = f.read()

# Update signature
old_sig = """fun SearchScreen(
    playerViewModel: PlayerViewModel,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {"""

new_sig = """fun SearchScreen(
    playerViewModel: PlayerViewModel,
    initialQuery: String? = null,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {"""
content = content.replace(old_sig, new_sig)

# Add state variables
state_vars = """    val searchViewModel: SearchViewModel = viewModel()
    
    LaunchedEffect(initialQuery) {
        if (initialQuery != null) {
            searchViewModel.performSearchNow(initialQuery)
        }
    }
    
    val query by searchViewModel.searchQuery.collectAsState()
    val suggestions by searchViewModel.searchSuggestions.collectAsState()
    val tracks by searchViewModel.searchTracks.collectAsState()
    val artists by searchViewModel.searchArtists.collectAsState()
    val albums by searchViewModel.searchAlbums.collectAsState()"""

content = re.sub(r"    val searchViewModel: SearchViewModel = viewModel\(\)\n    val query by searchViewModel.searchQuery.collectAsState\(\)\n    val tracks by searchViewModel.searchTracks.collectAsState\(\)\n    val artists by searchViewModel.searchArtists.collectAsState\(\)", state_vars, content)


# Find where query length is checked to show results vs categories
old_search_results = """        if (query.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, bottom = 120.dp)
            ) {
                if (tracks.isNotEmpty()) {
                    item {
                        Text("Songs", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp))
                    }
                    items(tracks) { track ->"""

new_search_results = """        if (query.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, bottom = 120.dp)
            ) {
                if (suggestions.isNotEmpty() && tracks.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
                    item {
                        Text("Suggestions", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp))
                    }
                    items(suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { searchViewModel.performSearchNow(suggestion.title) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(suggestion.title, color = Color.White, fontSize = 16.sp)
                                if (suggestion.artist != null) {
                                    Text(suggestion.artist, color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    if (tracks.isNotEmpty()) {
                        item {
                            Text("Songs", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp, top = 8.dp))
                        }
                        items(tracks) { track ->"""

content = content.replace(old_search_results, new_search_results)

# Add Album results just after Artists block
old_artists_end = """                            }
                        }
                    }
                }
            }
        } else {"""

new_artists_end = """                            }
                        }
                    }
                }
                
                if (albums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Albums", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    items(albums) { album ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToAlbum(album.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = album.images?.small ?: album.images?.medium,
                                contentDescription = album.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Album • ${album.artist}",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        } else {"""

content = content.replace(old_artists_end, new_artists_end)

with open("app/src/main/java/com/example/SearchScreen.kt", "w") as f:
    f.write(content)
