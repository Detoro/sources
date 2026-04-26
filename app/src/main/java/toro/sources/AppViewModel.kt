package toro.sources

import android.util.Log
import android.net.Uri
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import toro.sources.DataModels.Chapter
import toro.sources.DataModels.Comic
import toro.sources.DataModels.Page
import toro.sources.DataModels.LoginCredentials
import toro.sources.DataModels.SignUpPayload

sealed interface ReaderUiState {
    object Idle : ReaderUiState
    object Loading : ReaderUiState
    data class Success(val pages: List<Page>, val startingPageIndex: Int) : ReaderUiState
    data class Error(val message: String) : ReaderUiState
}

class AppViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    // Home Scrren
    val myLibrary: StateFlow<List<Comic>> = repository.getMyLibrary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<Comic>()
        )

    // comic details
    private val _selectedComicId = MutableStateFlow<String?>(null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Comic>> = combine(
        myLibrary, _searchQuery
    ) { library, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            library.filter { comic ->
                comic.title.contains(query, ignoreCase = true) ||
                        comic.author.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeChapters: StateFlow<List<Chapter>> = _selectedComicId
        .filterNotNull()
        .flatMapLatest { comicId -> repository.getChaptersForComic(comicId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<Chapter>()
        )

    val activeComic: StateFlow<Comic?> = combine(myLibrary, _selectedComicId) { library, selectedId ->
        // Finds the specific comic in your library that matches the clicked ID
        library.find { it.id == selectedId }
    }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = null
    )

    // for pages and swiping
    private val _readerState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val readerState: StateFlow<ReaderUiState> = _readerState.asStateFlow()

    init {
        testServer()
    }

    fun selectComic(comicId: String) {
        _selectedComicId.value = comicId
    }

    fun uploadNewComic(
        title: String,
        author: String,
        description: String,
        comicUri: Uri,
        coverUri: Uri?
    ) {
        viewModelScope.launch {
            repository.importLocalComic(comicUri, title)
        }
    }

    fun openChapter(chapterId: String) {
        viewModelScope.launch {
            _readerState.value = ReaderUiState.Loading
            try {
                // Fetch the specific chapter from Room to get its comicId and lastReadPageIndex
                val chapter = repository.getChapterById(chapterId)

                // fetch the actual pages
                val pages = repository.getPagesForChapter(chapter.id, chapter.comicId)

                _readerState.value = ReaderUiState.Success(
                    pages = pages,
                    startingPageIndex = chapter.lastReadPageIndex
                )
            } catch (e: Exception) {
                _readerState.value = ReaderUiState.Error(e.message ?: "Failed to load pages")
            }
        }
    }

    fun onPageTurned(chapterId: String, newPageIndex: Int) {
        viewModelScope.launch {
            repository.updateProgress(chapterId, newPageIndex)
        }
    }

    fun loginUser(credentials: LoginCredentials, onSuccess: () -> Unit) {
        onSuccess()
        testServer()
    }

    fun registerNewUser(newUser: SignUpPayload, onSuccess: () -> Unit) {

    }

    fun onFileSelected(uri: Uri, title: String) {
        viewModelScope.launch {
            repository.importLocalComic(uri, title)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun importLocalComic(uri: Uri, title: String) {
        viewModelScope.launch {
            repository.importLocalComic(uri, title)
        }
    }

    fun syncRemoteCatalog() {
        viewModelScope.launch {
            // repository.fetchRemoteCatalog()
        }
    }

    fun logoutUser(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            _selectedComicId.value = null

            onLogoutComplete()
        }
    }

    fun testServer() {
        viewModelScope.launch {
            try {
                val catalog = RetrofitClient.comicApiService.getCatalog()
                Log.d("success", "Received ${catalog.size} comics from Ktor!")

                val testComic = catalog.first()
                val response = RetrofitClient.comicApiService.sayHello(testComic)
                Log.d("Meh meh", "Ktor says: $response")

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("Network Error", "Failed to connect to toro server.")
            }
        }
    }
}