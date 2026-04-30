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
import kotlinx.coroutines.withContext
import toro.sources.DataModels.Chapter
import toro.sources.DataModels.Comic
import toro.sources.DataModels.LoginCredentials
import toro.sources.DataModels.AuthRequest
import toro.sources.DataModels.TokenManager
import toro.sources.DataModels.Post
import toro.sources.DataModels.ChatMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import toro.sources.DataModels.AuthResponse
import toro.sources.DataModels.ChatRequest
import toro.sources.DataModels.Comment
import toro.sources.db.ComicRepository
import toro.sources.network.RetrofitClient
import android.content.Context
import toro.sources.DataModels.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import toro.sources.DataModels.AuthorRequest
import toro.sources.DataModels.Bookmark
import toro.sources.DataModels.Conversation
import kotlin.String

@OptIn(FlowPreview::class)
class AppViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    val currentUser = MutableStateFlow(AuthResponse())

    val myLibrary: StateFlow<List<Comic>> = repository.getMyLibrary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
    private val _chatRequests = MutableStateFlow<List<ChatRequest>>(emptyList())
    val chatRequests = _chatRequests.asStateFlow()

    val pendingRequestsCount: StateFlow<Int> = _chatRequests
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _catalog = MutableStateFlow<List<Comic>>(emptyList())
    val catalog = _catalog.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters = _chapters.asStateFlow()

    private val _inbox = MutableStateFlow<List<Conversation>>(emptyList())
    val inbox: StateFlow<List<Conversation>> = _inbox

    private val _currentComic = MutableStateFlow<Comic?>(null)
    val currentComic = _currentComic.asStateFlow()

    private val _communityPosts = MutableStateFlow<List<Post>>(emptyList())
    val communityPosts = _communityPosts.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()
    private val _pageCount = MutableStateFlow(0)
    val pageCount = _pageCount.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed = _isSubscribed.asStateFlow()

    private var chapterPages: List<Page> = emptyList()

    init {
        getCatalog()
        getChatRequests()

        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .filter { it.isNotBlank() }
                .collectLatest { readyQuery ->
                    searchComics(readyQuery)
                }
        }
    }

    fun setCurrentComic(comic: Comic) {
        _currentComic.value = comic
    }

    fun importLocalComic(
        title: String,
        author: String,
        description: String,
        comicUri: Uri,
    ) {
        viewModelScope.launch {
            repository.importLocalComic(comicUri, title, author, description)
        }
    }

    fun removeComicFromLibrary(comicId: String, onRemoved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.removeComicFromLibrary(comicId)
                _catalog.value = _catalog.value.filter { it.id != comicId }
                onRemoved()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to remove comic: ${e.message}"
            }
        }
    }

    fun toggleComicSubscription(comicId: String) {
        _isSubscribed.value = !_isSubscribed.value

        viewModelScope.launch {
            try {
                val response = RetrofitClient.comicApiService.toggleComicSubscription(comicId)
                _isSubscribed.value = response.isSubscribed
            } catch (e: Exception) {
                _isSubscribed.value = !_isSubscribed.value
                Log.e("Subscription", "Failed to toggle: ${e.message}")
            }
        }
    }

    fun subscribeToAuthor(author: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.comicApiService.subscribeToAuthor(AuthorRequest(author))
                Log.i("Subscription", "Successfully subscribed to $author")
            } catch (e: Exception) {
                Log.e("Subscription", "Failed to subscribe to author: ${e.message}")
            }
        }
    }

    fun openChapter(comic: Comic, chapterId: String = "") {
        viewModelScope.launch {
            try {
                val pages = repository.getPagesForChapter(chapterId, comic.id)
                chapterPages = pages
                _pageCount.value = pages.size
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chapter: ${e.message}"
                Log.e("Reader", _errorMessage.value, e)
            }
        }
    }

    suspend fun getPageData(pageIndex: Int): Any? {
        return withContext(Dispatchers.IO) {
            val page = chapterPages.getOrNull(pageIndex)
            page?.localUri ?: page?.imageUrl
        }
    }

    fun onPageTurned(chapterId: String, newPageIndex: Int) {
        viewModelScope.launch {
            repository.updateProgress(chapterId, newPageIndex)
        }
    }

    fun loginUser(credentials: LoginCredentials, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val authRequest = AuthRequest(email = credentials.email, password = credentials.password)
                val response = RetrofitClient.comicApiService.login(authRequest)
                currentUser.value = response
                TokenManager.jwtToken = response.token
                _currentComic.value = null
                onSuccess()
                Log.i("Success", "Logged in successfully as ${response.username}!")

            } catch (e: Exception) {
                Log.e("Failure", "Login failed: ${e.message}")
            }
        }
    }

    fun logoutUser(onLogoutComplete: () -> Unit) {
        currentUser.value = AuthResponse()
        TokenManager.jwtToken = null
        _currentComic.value = null
        onLogoutComplete()
    }

    fun registerNewUser(newUser: AuthRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.comicApiService.signUp(newUser)
                currentUser.value = response
                TokenManager.jwtToken = response.token
                onSuccess()
                Log.i("Success", "Sign up successfully as ${response.username}!")
            } catch (e: Exception) {
                Log.e("Failure", "Signup failed: ${e.message}")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getCatalog() {
        viewModelScope.launch {
            try {
                _catalog.value = RetrofitClient.comicApiService.getCatalog()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load catalog: ${e.message}"
            }
        }
    }
    fun uploadComic(
        context: Context,
        title: String,
        author: String,
        description: String,
        comicUri: Uri,
        coverUri: Uri?
    ) {
        viewModelScope.launch {
            try {
                val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val authorPart = author.toRequestBody("text/plain".toMediaTypeOrNull())
                val descPart = description.toRequestBody("text/plain".toMediaTypeOrNull())

                val comicInputStream = context.contentResolver.openInputStream(comicUri)
                val comicBytes = comicInputStream?.readBytes() ?: throw Exception("Could not read comic file")
                val comicBody = comicBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())
                val comicFilePart = MultipartBody.Part.createFormData("file", "upload.cbz", comicBody)

                var coverFilePart: MultipartBody.Part? = null
                if (coverUri != null) {
                    val coverStream = context.contentResolver.openInputStream(coverUri)
                    val coverBytes = coverStream?.readBytes()
                    if (coverBytes != null) {
                        val coverBody = coverBytes.toRequestBody("image/*".toMediaTypeOrNull())
                        coverFilePart = MultipartBody.Part.createFormData("cover", "cover.jpg", coverBody)
                    }
                }

                val response = RetrofitClient.comicApiService.uploadComic(
                    file = comicFilePart,
                    title = titlePart,
                    author = authorPart,
                    description = descPart,
                    cover = coverFilePart
                )

                Log.i("Successful Upload", "Upload Success: ${response.message}")

            } catch (e: Exception) {
                _errorMessage.value = "Upload failed: ${e.message}"
                Log.e("Upload Error", "Upload failed", e)
            }
        }
    }
    fun getChatMessages(conversationId: String) {
        viewModelScope.launch {
            try {
                _chatMessages.value = RetrofitClient.comicApiService.getChatMessages(conversationId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chat: ${e.message}"
            }
        }
    }
    fun sendMessage(conversationId: String, content: String) {
        viewModelScope.launch {
            try {
                // Everything handled server side
                val newMessage = ChatMessage(
                    id = "",
                    senderId = "",
                    content = content,
                    timestamp = 0L,
                )
                RetrofitClient.comicApiService.sendMessage(conversationId, newMessage)
                getChatMessages(conversationId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send message: ${e.message}"
            }
        }
    }
    fun getChatRequests() {
        viewModelScope.launch {
            try {
                _chatRequests.value = RetrofitClient.comicApiService.getChatRequests()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load requests: ${e.message}"
            }
        }
    }

    fun acceptFriend(requestId: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.comicApiService.acceptChatRequest(requestId)
                _chatRequests.value = _chatRequests.value.filter { it.id != requestId }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to accept request: ${e.message}"
            }
        }
    }

    fun declineFriend(requestId: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.comicApiService.declineChatRequest(requestId)
                _chatRequests.value = _chatRequests.value.filter { it.id != requestId }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to decline request: ${e.message}"
            }
        }
    }
    fun getInbox() {
        viewModelScope.launch {
            try {
                val conversations = RetrofitClient.comicApiService.getInbox()
                _inbox.value = conversations

            } catch (e: Exception) {
                _errorMessage.value = "Failed to load inbox: ${e.message}"
                Log.e("ChatInbox", "Error loading conversations", e)
            }
        }
    }
    fun searchComics(query: String) {
        viewModelScope.launch {
            try {
                _catalog.value = RetrofitClient.comicApiService.searchComics(query)
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            }
        }
    }
    fun getChaptersForComic(comic: Comic) {
        viewModelScope.launch {
            try {
                repository.getChaptersForComic(comic.id).collect { localChapters ->
                    _chapters.value = localChapters
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chapters: ${e.message}"
            }
        }

        if (!comic.isLocalSideload) {
            viewModelScope.launch {
                try {
                    repository.syncRemoteChaptersForComic(comic)
                } catch (e: Exception) {
                    Log.e("Network", "Could not sync remote chapters: ${e.message}")
                }
            }
        }
    }

    fun getCommunityPosts() {
        viewModelScope.launch {
            try {
                _communityPosts.value = RetrofitClient.comicApiService.getCommunityPosts()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load community: ${e.message}"
            }
        }
    }
    fun getPostComments(postId: String) {
        viewModelScope.launch {
            try {
                _comments.value = RetrofitClient.comicApiService.getPostComments(postId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load comments: ${e.message}"
            }
        }
    }
    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.comicApiService.likePost(postId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to like comment: ${e.message}"
            }
        }
    }
    fun bookmarkPost(postId: String) {
        viewModelScope.launch {
            try {
                val newBookmark = Bookmark(
                    id = "",
                    userId = "",
                    postId = postId,
                    timestamp = 0L
                )
                RetrofitClient.comicApiService.bookmarkPost(postId, newBookmark)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to make post: ${e.message}"
            }
        }
    }
    fun makePost(postContent: String) {
        viewModelScope.launch {
            try {
                val newPost = Post(
                    id = "",
                    authorId = "",
                    authorName = "",
                    content = postContent,
                    timestamp = 0L
                )
                RetrofitClient.comicApiService.makePost(newPost)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to make post: ${e.message}"
            }
        }
    }
    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            try {
                val newComment = Comment(
                    id = "", authorId = "", authorName = "", content = content, timestamp = 0L
                )
                RetrofitClient.comicApiService.addComment(postId, newComment)
                getPostComments(postId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to post comment: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}