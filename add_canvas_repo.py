with open('app/src/main/java/com/example/data/MusicRepository.kt', 'r') as f:
    content = f.read()

repo_method = """    suspend fun getCanvas(id: String): CanvasDto? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getCanvas(id)
                if (response.isSuccessful) response.body()?.data else null
            } catch (e: Exception) {
                null
            }
        }
    }
"""

if "fun getCanvas" not in content:
    content = content.replace('suspend fun getLyrics', repo_method + '\n    suspend fun getLyrics')
    with open('app/src/main/java/com/example/data/MusicRepository.kt', 'w') as f:
        f.write(content)
