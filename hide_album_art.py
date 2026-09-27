import re

with open('app/src/main/java/com/example/FullPlayerScreen.kt', 'r') as f:
    content = f.read()

find_album = '''            // Album Artwork (Spotify square card with 8dp rounded corners)
            AsyncImage(
                model = track?.images?.large ?: track?.images?.medium,
                contentDescription = track?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF282828))
            )'''

replace_album = '''            // Album Artwork (Spotify square card with 8dp rounded corners)
            if (currentCanvasUrl == null) {
                AsyncImage(
                    model = track?.images?.large ?: track?.images?.medium,
                    contentDescription = track?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF282828))
                )
            } else {
                Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }'''

content = content.replace(find_album, replace_album)
with open('app/src/main/java/com/example/FullPlayerScreen.kt', 'w') as f:
    f.write(content)
