with open('app/src/main/java/com/example/MainScreen.kt', 'r') as f:
    content = f.read()

# Replace ic_spotify with an official logo or standard icon. Let's just use Icons.Filled.WorkspacePremium for Premium since we need an icon for Premium.
content = content.replace('iconId = R.drawable.ic_spotify,\n            label = "Premium"', 'icon = androidx.compose.material.icons.Icons.Filled.WorkspacePremium,\n            label = "Premium"')
content = content.replace('import androidx.compose.material.icons.Icons', 'import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.WorkspacePremium')
with open('app/src/main/java/com/example/MainScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/SearchScreen.kt', 'r') as f:
    content = f.read()
# Let's replace Spotify logo in SearchScreen with our app logo or standard icon.
# Using standard music note for now, since we don't have a specific custom vector drawable except ic_launcher_foreground.png which is a PNG, not a vector. Wait, I can use a standard icon or Text.
# Let's use Icons.Filled.GraphicEq for the app logo
content = content.replace('painter = painterResource(id = R.drawable.ic_spotify)', 'imageVector = Icons.Filled.GraphicEq')
content = content.replace('import androidx.compose.material.icons.filled.*', 'import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.filled.GraphicEq')

with open('app/src/main/java/com/example/SearchScreen.kt', 'w') as f:
    f.write(content)

