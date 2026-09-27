import re

with open("app/src/main/java/com/example/SearchViewModel.kt", "r") as f:
    content = f.read()

# Add suggestions state and getSearchSuggestions logic
suggestions = """    private val _searchSuggestions = MutableStateFlow<List<com.example.data.remote.models.SearchSuggestionDto>>(emptyList())
    val searchSuggestions: StateFlow<List<com.example.data.remote.models.SearchSuggestionDto>> = _searchSuggestions.asStateFlow()
    
    private val _searchAlbums = MutableStateFlow<List<com.example.data.remote.models.AlbumDto>>(emptyList())
    val searchAlbums: StateFlow<List<com.example.data.remote.models.AlbumDto>> = _searchAlbums.asStateFlow()
    
    private var searchJob: kotlinx.coroutines.Job? = null
"""

content = content.replace("    private val _searchArtists = MutableStateFlow<List<ArtistDto>>(emptyList())\n    val searchArtists: StateFlow<List<ArtistDto>> = _searchArtists.asStateFlow()", "    private val _searchArtists = MutableStateFlow<List<ArtistDto>>(emptyList())\n    val searchArtists: StateFlow<List<ArtistDto>> = _searchArtists.asStateFlow()\n" + suggestions)

update_query_new = """    fun updateQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.length > 1) {
            searchJob = viewModelScope.launch {
                kotlinx.coroutines.delay(300) // debounce
                val suggestions = repository.getSearchSuggestions(query)
                _searchSuggestions.value = suggestions
                performSearch(query)
            }
        } else {
            _searchSuggestions.value = emptyList()
            _searchTracks.value = emptyList()
            _searchArtists.value = emptyList()
            _searchAlbums.value = emptyList()
        }
    }
    
    fun performSearchNow(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        _searchSuggestions.value = emptyList()
        performSearch(query)
    }

    private suspend fun performSearch(query: String) {
        try {
            val res = repository.search(query)
            if (res != null) {
                _searchTracks.value = res.songs ?: emptyList()
                _searchArtists.value = res.artists ?: emptyList()
                _searchAlbums.value = res.albums ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }"""

# Need to replace updateQuery and performSearch entirely.
pattern = re.compile(r"    fun updateQuery\(query: String\) \{.*", re.DOTALL)
content = re.sub(pattern, update_query_new + "\n}", content)

with open("app/src/main/java/com/example/SearchViewModel.kt", "w") as f:
    f.write(content)
