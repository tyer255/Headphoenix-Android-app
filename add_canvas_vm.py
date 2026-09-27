import re

with open('app/src/main/java/com/example/PlayerViewModel.kt', 'r') as f:
    content = f.read()

props = """    private val _currentCanvasUrl = MutableStateFlow<String?>(null)
    val currentCanvasUrl: StateFlow<String?> = _currentCanvasUrl.asStateFlow()
"""
content = content.replace('    private val _currentLyrics = MutableStateFlow<LyricsDto?>(null)', props + '\n    private val _currentLyrics = MutableStateFlow<LyricsDto?>(null)')

fetch_code = """
        viewModelScope.launch {
            try {
                val canvas = repository.getCanvas(track.id)
                _currentCanvasUrl.value = canvas?.canvasUrl ?: canvas?.videoUrl
            } catch (e: Exception) {
                _currentCanvasUrl.value = null
            }
        }
"""
content = content.replace('        viewModelScope.launch {\n            try {\n                val lyrics = repository.getLyrics(track.id)', fetch_code + '\n        viewModelScope.launch {\n            try {\n                val lyrics = repository.getLyrics(track.id)')

content = content.replace('        _currentLyrics.value = null', '        _currentLyrics.value = null\n        _currentCanvasUrl.value = null')

with open('app/src/main/java/com/example/PlayerViewModel.kt', 'w') as f:
    f.write(content)
