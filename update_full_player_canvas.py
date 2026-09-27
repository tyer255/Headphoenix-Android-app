import re

with open('app/src/main/java/com/example/FullPlayerScreen.kt', 'r') as f:
    content = f.read()

imports = """
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
"""
content = content.replace('import androidx.compose.ui.platform.LocalContext', 'import androidx.compose.ui.platform.LocalContext' + imports)

# Find `val themeColor = rememberTrackThemeColor(track)`
# and insert `val currentCanvasUrl by viewModel.currentCanvasUrl.collectAsState()`
find_state = 'val themeColor = rememberTrackThemeColor(track)'
replace_state = '''val themeColor = rememberTrackThemeColor(track)
    val currentCanvasUrl by viewModel.currentCanvasUrl.collectAsState()'''
content = content.replace(find_state, replace_state)

# Find where Box modifier = Modifier.fillMaxSize().background(...) is
find_box = '''    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.6f),
                        Color(0xFF121212)
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 50) {
                            onClose()
                        }
                    }
                )
            }
    ) {'''

replace_box = '''    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColor.copy(alpha = 0.6f),
                        Color(0xFF121212)
                    )
                )
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 50) {
                            onClose()
                        }
                    }
                )
            }
    ) {
        if (currentCanvasUrl != null) {
            val exoPlayer = remember(currentCanvasUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(currentCanvasUrl!!))
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = 0f
                    playWhenReady = true
                    prepare()
                }
            }
            DisposableEffect(exoPlayer) {
                onDispose {
                    exoPlayer.release()
                }
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Add a dark overlay so text is still readable
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }
'''

content = content.replace(find_box, replace_box)

with open('app/src/main/java/com/example/FullPlayerScreen.kt', 'w') as f:
    f.write(content)
