import re

with open("app/src/main/java/com/example/ArtistScreen.kt", "r") as f:
    content = f.read()

if "import androidx.compose.foundation.lazy.rememberLazyListState" not in content:
    content = content.replace("import androidx.compose.foundation.lazy.items", "import androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.lazy.rememberLazyListState\nimport androidx.compose.ui.graphics.graphicsLayer")

# Add list state
state_replace = """    val playlists by PlaylistRepository.playlists.collectAsState()
    
    val listState = rememberLazyListState()
    val headerTranslationY by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset * 0.5f
            } else {
                0f
            }
        }
    }
    val headerAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                1f - (listState.firstVisibleItemScrollOffset / 600f).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    val showTopBarName by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 400
        }
    }"""
content = content.replace("    val playlists by PlaylistRepository.playlists.collectAsState()", state_replace)

# Pass list state to LazyColumn
content = content.replace("LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {", "LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {")

# Add graphicsLayer to Box
box_replace = """                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .graphicsLayer {
                                        translationY = headerTranslationY
                                        alpha = headerAlpha
                                    }
                            ) {"""
content = content.replace("""                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {""", box_replace)

# Top Bar Header (absolute)
top_bar_replace = """        when (val state = uiState) {
            is ArtistUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            }
            is ArtistUiState.Success -> {
                val artist = state.data
                Box(modifier = Modifier.fillMaxSize()) {"""
content = content.replace("""        when (val state = uiState) {
            is ArtistUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            }
            is ArtistUiState.Success -> {
                val artist = state.data""", top_bar_replace)

bottom_replace = """                    // Singles Section"""
top_bar_end = """                    // Singles Section"""

top_bar_overlay = """                }
                
                // Overlay Top App Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(if (showTopBarName) Color(0xFF121212) else Color.Transparent)
                        .padding(top = 36.dp, start = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (showTopBarName) Color.Transparent else Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        if (showTopBarName) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = artist.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }"""
            
# Find end of Box around line 245
content = re.sub(r'            }\n        }\n    }\n}\n', r'            }\n                \n                // Overlay Top App Bar\n                Box(\n                    modifier = Modifier\n                        .fillMaxWidth()\n                        .height(84.dp)\n                        .background(if (showTopBarName) Color(0xFF121212) else Color.Transparent)\n                        .padding(top = 36.dp, start = 8.dp)\n                ) {\n                    Row(verticalAlignment = Alignment.CenterVertically) {\n                        IconButton(\n                            onClick = onBack,\n                            modifier = Modifier\n                                .size(44.dp)\n                                .clip(CircleShape)\n                                .background(if (showTopBarName) Color.Transparent else Color.Black.copy(alpha = 0.4f))\n                        ) {\n                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)\n                        }\n                        if (showTopBarName) {\n                            Spacer(modifier = Modifier.width(16.dp))\n                            Text(\n                                text = artist.name,\n                                color = Color.White,\n                                fontWeight = FontWeight.Bold,\n                                fontSize = 18.sp\n                            )\n                        }\n                    }\n                }\n            }\n        }\n    }\n}\n', content)

# Remove the old back button from LazyColumn
old_back = """                            // Back arrow button
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .padding(top = 40.dp, start = 12.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }"""
content = content.replace(old_back, "")

with open("app/src/main/java/com/example/ArtistScreen.kt", "w") as f:
    f.write(content)
print("Done")
