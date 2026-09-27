with open("app/src/main/java/com/example/MainScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    'onNavigateToArtist = { id -> navController.navigate("artist/$id") }',
    'onNavigateToArtist = { id -> navController.navigate("artist/$id") },\n                        onNavigateToAlbum = { id -> navController.navigate("album/$id") },\n                        onNavigateToSearch = { query -> navController.navigate("search?query=$query") }'
)

# And in artist route
old_artist = """                composable("artist/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    ArtistScreen(id, playerViewModel) { navController.popBackStack() }
                }"""

new_artist = """                composable("artist/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    ArtistScreen(id, playerViewModel, { navController.navigate("album/$it") }) { navController.popBackStack() }
                }
                composable("album/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    AlbumScreen(id, playerViewModel) { navController.popBackStack() }
                }"""

content = content.replace(old_artist, new_artist)

# Fix search route to accept optional query parameter
old_search = """composable("search") {
                    SearchScreen(playerViewModel, onNavigateToArtist = { id -> navController.navigate("artist/$id") }, onNavigateToPlaylist = { id -> navController.navigate("playlist/$id") }, onOpenProfile = { showProfileSheet = true })
                }"""

new_search = """composable("search?query={query}") { backStackEntry ->
                    val initialQuery = backStackEntry.arguments?.getString("query")
                    SearchScreen(playerViewModel, initialQuery, onNavigateToArtist = { id -> navController.navigate("artist/$id") }, onNavigateToAlbum = { id -> navController.navigate("album/$id") }, onNavigateToPlaylist = { id -> navController.navigate("playlist/$id") }, onOpenProfile = { showProfileSheet = true })
                }"""

content = content.replace(old_search, new_search)

with open("app/src/main/java/com/example/MainScreen.kt", "w") as f:
    f.write(content)
