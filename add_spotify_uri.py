with open('app/src/main/java/com/example/data/remote/models/ApiModels.kt', 'r') as f:
    content = f.read()

content = content.replace('val playbackAvailability: Boolean? = true', 'val playbackAvailability: Boolean? = true,\n    val spotifyId: String? = null,\n    val spotifyUri: String? = null')

with open('app/src/main/java/com/example/data/remote/models/ApiModels.kt', 'w') as f:
    f.write(content)
