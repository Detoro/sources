package toro.sources.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.toro.models.AuthorRequest
import com.toro.models.Chapter
import com.toro.models.Comic
import com.toro.models.Creator
import com.toro.models.Genre
import com.toro.models.Page
import com.toro.models.PgRating
import com.toro.models.RegisterChaptersRequest
import com.toro.models.RegisterComicRequest
import com.toro.models.ScrollDirection
import com.toro.models.SearchSource
import com.toro.models.UserProfile
import com.toro.models.writtenBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toro.sources.Screen
import toro.sources.db.ComicRepository
import toro.sources.media.MediaUploadManager
import toro.sources.navigation.NavigationState
import toro.sources.network.RetrofitClient
import toro.sources.session.SessionManager
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
@HiltViewModel
class ComicsViewModel @Inject constructor(
    application: Application,
    private val sessionManager: SessionManager,
    private val navigationState: NavigationState,
    private val repository: ComicRepository,
    private val mediaUploadManager: MediaUploadManager
) : AndroidViewModel(application) {

    private val _comicsUiState = MutableStateFlow(ComicsUiState())
    val comicsUiState = _comicsUiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val localLibrary: StateFlow<List<Comic>> = sessionManager.userProfile
        .flatMapLatest { user ->
            if (user != null) repository.getMyLibrary() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val subscribedComics: StateFlow<List<Comic>> = sessionManager.userProfile
        .flatMapLatest { user ->
            if (user != null) repository.getSubscribedComics() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val recentlyReadComics: StateFlow<List<Comic>> = sessionManager.userProfile
        .flatMapLatest { user ->
            if (user != null) repository.getRecentlyReadComics() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _onlineLibrary = MutableStateFlow<List<Comic>>(emptyList())
    val onlineLibrary = _onlineLibrary.asStateFlow()

    private val _trending = MutableStateFlow<List<Comic>>(emptyList())
    val trending = _trending.asStateFlow()

    // Search State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchSource = MutableStateFlow(SearchSource.LOCAL)
    val searchSource = _searchSource.asStateFlow()

    private val _searchFilter = MutableStateFlow("All")
    val searchFilter = _searchFilter.asStateFlow()

    val searchResults: StateFlow<List<Comic>> = combine(
        localLibrary, _onlineLibrary, _searchQuery, _searchSource, _searchFilter
    ) { library, online, query, source, filter ->
        if (query.isBlank()) return@combine emptyList()

        val baseList = if (source == SearchSource.LOCAL) library else online

        baseList.filter { comic ->
            when (filter) {
                "Authors" -> comic.writtenBy.contains(query, ignoreCase = true)
                "Tags" -> comic.genres.any { it.name.contains(query, ignoreCase = true) }
                "Comics" -> comic.title.contains(query, ignoreCase = true)
                else -> {
                    comic.title.contains(query, ignoreCase = true) ||
                            comic.writtenBy.contains(query, ignoreCase = true) ||
                            comic.genres.any { it.name.contains(query, ignoreCase = true) }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Comic & Chapter State
    private val _currentComic = MutableStateFlow<Comic?>(null)
    val currentComic = _currentComic.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters = _chapters.asStateFlow()

    private var chaptersJob: Job? = null

    // Reading State
    private val _pageCount = MutableStateFlow(0)
    val pageCount = _pageCount.asStateFlow()

    private var chapterPages: List<Page> = emptyList()

    // Upload State
    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess = _uploadSuccess.asStateFlow()

    // Author Filters
    @OptIn(ExperimentalCoroutinesApi::class)
    val subscribedAuthors: StateFlow<List<UserProfile>> = sessionManager.userProfile
        .flatMapLatest { user ->
            if (user != null) repository.getSubscribedAuthors() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAuthorIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedAuthorIds = _selectedAuthorIds.asStateFlow()

    init {
        clearComicsError()
        viewModelScope.launch {
            combine(_searchQuery, _searchSource) { query, source -> query to source }
                .debounce(500.milliseconds)
                .filter { it.first.isNotBlank() && it.second == SearchSource.ONLINE }
                .collectLatest { (readyQuery, _) ->
                    if (sessionManager.userProfile.value != null) {
                        searchComics(readyQuery)
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSearchSource(source: SearchSource) { _searchSource.value = source }
    fun updateSearchFilter(filter: String) { _searchFilter.value = filter }

    fun searchComics(query: String) {
        viewModelScope.launch {
            try {
                _onlineLibrary.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.searchComics(query)
                }
            } catch (e: Exception) {
                Log.e("ComicsViewModel", "Search failed: ${e.message}")
            }
        }
    }

    fun fetchTrendingAndRecommendations() {
        viewModelScope.launch {
            try {
                val trending =
                    withContext(Dispatchers.IO) { RetrofitClient.comicApiService.getTrending() }
                val recommended =
                    withContext(Dispatchers.IO) { RetrofitClient.comicApiService.getRecommendation() }
                _trending.value = trending
                _onlineLibrary.value = recommended
            } catch (e: Exception) {
                Log.e("ComicsViewModel", "Failed to fetch trending/recommendations: ${e.message}")
            }
        }
    }

    fun setCurrentComic(comic: Comic) {
        viewModelScope.launch {
            val localComic = withContext(Dispatchers.IO) { repository.getComicByIdSync(comic.id) }
            _currentComic.value = localComic ?: comic
            getChaptersForComic(comic)
        }
    }

    fun getChaptersForComic(comic: Comic) {
        chaptersJob?.cancel()
        if (!comic.isLocalSideload) {
            chaptersJob = viewModelScope.launch {
                try {
                    val remoteChapters = withContext(Dispatchers.IO) {
                        RetrofitClient.comicApiService.getChaptersForComic(comic.id)
                    }
                    _chapters.value = remoteChapters
                    withContext(Dispatchers.IO) { repository.syncRemoteChaptersForComic(comic) }
                } catch (e: Exception) {
                    if (e !is CancellationException) Log.e("ComicsViewModel", "Failed to sync chapters: ${e.message}")
                }
            }
        } else {
            viewModelScope.launch {
                repository.getChaptersForComic(comic.id).collect { _chapters.value = it }
            }
        }
    }

    fun toggleComicSubscription(comicId: String) {
        viewModelScope.launch {
            try {
                val current = _currentComic.value
                val response = withContext(Dispatchers.IO) {
                    val res = RetrofitClient.comicApiService.toggleComicSubscription(comicId)
                    if (current?.id == comicId) {
                        repository.insertComics(listOf(current.copy(isSubscribed = res.isSuccessful)))
                    } else {
                        repository.toggleLocalSubscription(comicId, res.isSuccessful)
                    }
                    res
                }
                if (current?.id == comicId) {
                    _currentComic.value = current.copy(isSubscribed = response.isSuccessful)
                }
            } catch (e: Exception) {
                _comicsUiState.update { it.copy(errorMessage = "Subscription toggle failed: ${e.message}") }
            }
        }
    }

    fun removeComicFromLibrary(comicId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.removeComicFromLibrary(comicId) }
            } catch (e: Exception) {
                _comicsUiState.update { it.copy(errorMessage = "Failed to remove comic: ${e.message}") }
            }
        }
    }

    fun importLocalComic(title: String, author: String, description: String, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.importLocalComic(
                    uri,
                    title,
                    author,
                    description
                )
            }
        }
    }

    fun rateComic(comicId: String, rating: Float) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.rateComic(
                        comicId,
                        rating
                    )
                }
                _currentComic.update { if (it?.id == comicId) it.copy(rating = rating) else it }
            } catch (e: Exception) {
                _comicsUiState.update { it.copy(errorMessage = "Rating failed: ${e.message}") }
            }
        }
    }

    // Reading Logic
    fun openChapter(comic: Comic, chapterId: String) {
        viewModelScope.launch {
            _pageCount.value = 0
            val targetChapter = _chapters.value.find { it.id == chapterId }
            val loadLocally = comic.isLocalSideload || targetChapter?.isDownloaded == true

            if (comic.isLocalSideload) {
                withContext(Dispatchers.IO) { repository.updateLastRead(comic.id) }
            }
            try {
                val pages = withContext(Dispatchers.IO) {
                    repository.getPagesForChapter(chapterId, comic.id, loadLocally)
                }
                chapterPages = pages
                _pageCount.value = pages.size
            } catch (e: Exception) {
                _comicsUiState.update { it.copy(errorMessage = "Failed to load chapter: ${e.message}") }
            }
        }
    }

    fun getPageData(index: Int): Any? {
        val page = chapterPages.getOrNull(index)
        return page?.localUri ?: page?.imageUrl
    }

    fun preloadAdjacentPages(currentIndex: Int) {
        val pagesToPreload = listOf(currentIndex - 1, currentIndex + 1, currentIndex + 2, currentIndex + 3)
        pagesToPreload.forEach { index ->
            getPageData(index)?.let { data ->
                val request = ImageRequest.Builder(getApplication()).data(data).build()
                getApplication<Application>().imageLoader.enqueue(request)
            }
        }
    }

    fun onPageTurned(chapterId: String, newIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateProgress(chapterId, newIndex) }
    }

    fun markChapterAsRead(comicId: String, chapterId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.markChapterAsRead(
                        comicId,
                        chapterId
                    )
                }
                _chapters.update { list -> list.map { if (it.id == chapterId) it.copy(isRead = true) else it } }
            } catch (e: Exception) {
                Log.e("ComicsViewModel", "Failed to mark chapter as read: ${e.message}")
            }
        }
    }

    fun likeChapter(comicId: String, chapterId: String) {
        viewModelScope.launch {
            try {
                val currentChapters = _chapters.value
                val chapter = currentChapters.find { it.id == chapterId } ?: return@launch
                val newLikedState = !chapter.isLiked
                _chapters.value = currentChapters.map { if (it.id == chapterId) it.copy(isLiked = newLikedState) else it }
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.likeChapter(comicId, chapterId)
                    repository.updateChapterLikeState(chapterId, newLikedState)
                }
            } catch (e: Exception) {
                Log.e("ComicsViewModel", "Like chapter failed: ${e.message}")
            }
        }
    }

    // Upload Logic
    fun uploadNewChapters(
        title: String = "",
        authors: List<Creator> = emptyList(),
        scrollDirection: ScrollDirection = ScrollDirection.VERTICAL,
        pgRating: PgRating = PgRating.PG13,
        description: String = "",
        comicId: String? = null,
        chapterUris: List<Uri>,
        genres: List<Genre> = emptyList(),
        selectedCover: Uri? = null,
        audioUris: List<Uri> = emptyList(),
        startingChapterNumber: Float = 0f
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadSuccess.value = false
            try {
                val uploadedAudioUrls = if (audioUris.isNotEmpty()) {
                    audioUris.map { uri -> async { mediaUploadManager.uploadFileToCloudinary(uri) } }.awaitAll()
                } else emptyList()
                val primaryAudioUrl = uploadedAudioUrls.firstOrNull()

                val chaptersData = chapterUris.mapIndexed { index, uri ->
                    mediaUploadManager.processAndUploadChapter(uri, startingChapterNumber + index + 1f).apply {
                        this.audioUrl = uploadedAudioUrls.getOrNull(index) ?: primaryAudioUrl
                    }
                }

                if (comicId == null) {
                    val coverUrl = selectedCover?.let { mediaUploadManager.uploadFileToCloudinary(it) }
                    RetrofitClient.comicApiService.registerNewComic(
                        RegisterComicRequest(
                            title = title, authors = authors, description = description,
                            coverUrl = coverUrl, scrollDirection = scrollDirection,
                            pgRating = pgRating, genres = genres, chapters = chaptersData,
                            audioUrl = primaryAudioUrl
                        )
                    )
                } else {
                    RetrofitClient.comicApiService.registerChapters(comicId,
                        RegisterChaptersRequest(chaptersData)
                    )
                }
                _uploadSuccess.value = true
            } catch (e: Exception) {
                _comicsUiState.update { it.copy(errorMessage = e.message ?: "Upload failed") }
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun resetUploadState() {
        _isUploading.value = false
        _uploadSuccess.value = false
    }

    // Authors
    fun subscribeToAuthor(authorId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.subscribeToAuthor(
                        AuthorRequest(authorId)
                    )
                }
                getSubscribedAuthors()
            } catch (e: Exception) {
                _comicsUiState.update { it.copy(errorMessage = "Author subscription failed: ${e.message}") }
            }
        }
    }

    fun getSubscribedAuthors() {
        viewModelScope.launch {
            try {
                val authors =
                    withContext(Dispatchers.IO) { RetrofitClient.comicApiService.getSubscribedAuthors() }
                repository.saveAuthors(authors)
            } catch (e: Exception) {
                Log.e("ComicsViewModel", "Failed to fetch subscribed authors: ${e.message}")
            }
        }
    }

    fun toggleAuthorFilter(authorId: String) {
        _selectedAuthorIds.update { if (it.contains(authorId)) it - authorId else it + authorId }
    }

    fun loadAndNavigateToComic(comicId: String) {
        viewModelScope.launch {
            val comicToLoad = onlineLibrary.value.find { it.id == comicId }
            if (comicToLoad != null) {
                setCurrentComic(comicToLoad)
                navigationState.handleNavigation(Screen.Overview.createRoute(comicId))
            } else {
                try {
                    loadComicById(comicId)
                    navigationState.handleNavigation(Screen.Overview.createRoute(comicId))
                } catch (e: Exception) {
                    val error = e.message
                    _comicsUiState.update { it.copy(errorMessage = error) }
                    Log.e("Navigation", "Error fetching comic $comicId: $error")
                }
            }
        }
    }

    fun loadComicById(comicId: String) {
        val trimmedId = comicId.trim()
        viewModelScope.launch {
            _currentComic.value = null
            _chapters.value = emptyList()

            var comicToLoad = onlineLibrary.value.find { it.id == trimmedId }

            if (comicToLoad == null) {
                try {
                    comicToLoad = withContext(Dispatchers.IO) {
                        RetrofitClient.comicApiService.getComicById(trimmedId)
                    }
                } catch (e: Exception) {
                    Log.e("ComicsViewModel", "Failed to fetch comic $trimmedId from API", e)
                    comicToLoad = trending.value.find { it.id == trimmedId }
                        ?: subscribedComics.value.find { it.id == trimmedId }
                                ?: recentlyReadComics.value.find { it.id == trimmedId }
                }
            }
            if (comicToLoad != null) {
                _currentComic.value = comicToLoad
                getChaptersForComic(comicToLoad)
            } else {
                Log.e("Comic error", "Comic not found or unavailable.")
                _comicsUiState.update { it.copy(errorMessage = "Failed to load comic details.") }
            }
        }
    }

    fun clearComicsError() { _comicsUiState.update { it.copy(errorMessage = null) } }
}

data class ComicsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)