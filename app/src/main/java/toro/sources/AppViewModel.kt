package toro.sources

import ChapterUploadData
import RegisterChaptersRequest
import RegisterComicRequest
import Chapter
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
import toro.sources.db.ComicRepository
import toro.sources.network.RetrofitClient
import android.content.Context
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.String
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.UploadCallback
import com.cloudinary.android.callback.ErrorInfo
import com.google.firebase.messaging.FirebaseMessaging
import toro.sources.notifications.NotificationEventBus
import toro.sources.crypto.CryptoUtils
import com.toro.models.*
import kotlinx.coroutines.flow.update
import java.util.zip.ZipInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.Boolean
import kotlin.collections.map
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlinx.serialization.json.Json
import okhttp3.Response
import toro.sources.db.CanvasDatabase
import java.util.UUID

enum class SearchSource {
    LOCAL, ONLINE
}

@OptIn(FlowPreview::class)
class AppViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private var chatWebSocket: WebSocket? = null
    private val socketJson = Json { ignoreUnknownKeys = true }

    val myLibrary: StateFlow<List<Comic>> = repository.getMyLibrary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSource = MutableStateFlow(SearchSource.LOCAL)
    val searchSource: StateFlow<SearchSource> = _searchSource.asStateFlow()

    fun updateSearchSource(source: SearchSource) {
        _searchSource.value = source
    }

    private val _catalog = MutableStateFlow<List<Comic>>(emptyList())
    val catalog = _catalog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _trending = MutableStateFlow<List<Comic>>(emptyList())
    val trending = _trending.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchFilter = MutableStateFlow("All")
    val searchFilter = _searchFilter.asStateFlow()

    val searchResults: StateFlow<List<Comic>> = combine(
        myLibrary, _catalog, _searchQuery, _searchSource, searchFilter
    ) { library, online, query, source, filter ->
        if (query.isBlank()) {
            return@combine emptyList()
        }
        val baseList = when (source) {
            SearchSource.LOCAL -> library
            SearchSource.ONLINE -> online
        }

        baseList.filter { comic ->
            val matchesText = comic.title.contains(query, ignoreCase = true) ||
                    comic.writtenBy.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "Authors" -> comic.writtenBy.contains(query, ignoreCase = true)
                "Tags" -> comic.genres.any { it.name.contains(query, ignoreCase = true) }
                "Comics" -> true
                else -> true
            }

            matchesText && matchesFilter
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

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters = _chapters.asStateFlow()

    val inbox: StateFlow<List<Conversation>> = repository.getConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessage>> = _currentConversationId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForConversation(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentComic = MutableStateFlow<Comic?>(null)
    val currentComic = _currentComic.asStateFlow()

    private val _communityPosts = MutableStateFlow<List<Post>>(emptyList())
    val communityPosts = _communityPosts.asStateFlow()

    private val _postComments = MutableStateFlow<List<Comment>>(emptyList())
    val postComments = _postComments.asStateFlow()

    private val _replyingToMessage = MutableStateFlow<ChatMessage?>(null)
    val replyingToMessage = _replyingToMessage.asStateFlow()

    private val _chapterComments = MutableStateFlow<List<Comment>>(emptyList())
    val chapterComments = _chapterComments.asStateFlow()

    val combinedComments: StateFlow<List<Comment>> = chapterComments
        .combine(postComments) { chapterComment, postComment ->
            chapterComment + postComment
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _pageCount = MutableStateFlow(0)
    val pageCount = _pageCount.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess = _uploadSuccess.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _targetUserProfile = MutableStateFlow<UserProfile?>(null)
    val targetUserProfile = _targetUserProfile.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts = _userPosts.asStateFlow()

    private val _targetUserPosts = MutableStateFlow<List<Post>>(emptyList())
    val targetUserPosts = _targetUserPosts.asStateFlow()

    private val _userWorks = MutableStateFlow<List<Comic>>(emptyList())
    val userWorks = _userWorks.asStateFlow()

    private val _targetUserWorks = MutableStateFlow<List<Comic>>(emptyList())
    val targetUserWorks = _targetUserWorks.asStateFlow()

    private val _userSuggestions = MutableStateFlow<List<UserProfile>>(emptyList())
    val userSuggestions = _userSuggestions.asStateFlow()

    private val _subscribedAuthors = MutableStateFlow<List<UserProfile>>(emptyList())
    val subscribedAuthors = _subscribedAuthors.asStateFlow()

    private val _selectedAuthorIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedAuthorIds = _selectedAuthorIds.asStateFlow()

    val notifications: StateFlow<List<Notification>> = repository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme = _isDarkTheme.asStateFlow()

    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation = _pendingNavigation.asStateFlow()

    private val _sharedContent = MutableStateFlow<SharedContent?>(null)
    val sharedContent = _sharedContent.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage: StateFlow<ChatMessage?> = _editingMessage.asStateFlow()

    private val _showShareDialog = MutableStateFlow(false)
    val showShareDialog = _showShareDialog.asStateFlow()

    fun showShareDialog(show: Boolean) {
        _showShareDialog.value = show
    }

    val subscribedComics: StateFlow<List<Comic>> = repository.getSubscribedComics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyReadComics: StateFlow<List<Comic>> = repository.getRecentlyReadComics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var chapterPages: List<Page> = emptyList()
    private var chaptersJob: kotlinx.coroutines.Job? = null

    private val _inboxSearchQuery = MutableStateFlow("")
    val inboxSearchQuery = _inboxSearchQuery.asStateFlow()

    val filteredInbox: StateFlow<List<Conversation>> = combine(inbox, _inboxSearchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { convo ->
                val nameMatch = convo.otherUserName.contains(query, ignoreCase = true)

                val messageMatch = try {
                    decryptMessage(convo.lastMessage ?: "").contains(query, ignoreCase = true)
                } catch (e: Exception) {
                    convo.lastMessage?.contains(query, ignoreCase = true)
                    Log.i("filtered inbox", "${e.message}")
                }

                nameMatch || messageMatch == true
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        clearErrorMessage()
        viewModelScope.launch {
            RetrofitClient.preferenceManager.isDarkTheme.collect { enabled ->
                _isDarkTheme.value = enabled
            }
        }

        viewModelScope.launch {
            NotificationEventBus.notifications.collect { newNotification ->
                withContext(Dispatchers.IO) {
                    repository.saveNotification(newNotification)
                }
            }
        }
        viewModelScope.launch {
            NotificationEventBus.friendRequests.collect {
                getChatRequests()
            }
        }

        viewModelScope.launch {
            combine(_searchQuery, _searchSource) { query, source ->
                query to source
            }
                .debounce(500L)
                .filter { it.first.isNotBlank() && it.second == SearchSource.ONLINE }
                .collectLatest { (readyQuery, _) ->
                    if (_userProfile.value?.id?.isNotEmpty() == true) {
                        searchComics(readyQuery)
                    }
                }
        }

        val userData = RetrofitClient.preferenceManager.getUserDataSync()
        val hasTokens = RetrofitClient.preferenceManager.getAccessTokenSync() != null
        if (userData.userId != null && hasTokens) {
            _userProfile.value = UserProfile(
                id = userData.userId,
                username = userData.username ?: "User",
                avatarUrl = userData.avatarUrl,
                bio = userData.bio,
                isPrivate = false
            )
            onUserAuthenticated(userData.userId)
        }
    }

    private fun onUserAuthenticated(userId: String) {
        viewModelScope.launch {
            repository.syncSubscriptions()
            getUserProfile(userId)
            connectChatSocket()
            fetchAndRegisterFcmToken()
            getRecommendation()
            getTrending()
            getChatRequests()
            getSubscribedAuthors()
            getInbox()
        }
    }

    fun setCurrentComic(comic: Comic) {
        viewModelScope.launch {
            val localComic = myLibrary.value.find { it.id == comic.id }
            _currentComic.value = localComic ?: comic
        }
    }
    fun setEditingMessage(message: ChatMessage?) {
        _editingMessage.value = message
    }
    fun setReplyTarget(message: ChatMessage?) {
        _replyingToMessage.value = message
    }
    fun updateSearchFilter(filter: String) {
        _searchFilter.value = filter
    }
    fun importLocalComic(
        title: String,
        author: String,
        description: String,
        comicUri: Uri,
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.importLocalComic(comicUri, title, author, description)
            }
        }
    }
    fun removeComicFromLibrary(comicId: String, onRemoved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.removeComicFromLibrary(comicId)
                }
                _catalog.value = _catalog.value.filter { it.id != comicId }
                onRemoved()
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Comic error", "Failed to remove comic: $error")
            }
        }
    }
    fun toggleComicSubscription(comicId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val res = RetrofitClient.comicApiService.toggleComicSubscription(comicId)
                    repository.toggleLocalSubscription(comicId, res.isSubscribed)
                    res
                }
                _currentComic.value = _currentComic.value?.copy(isSubscribed = response.isSubscribed)
                val topicName = "comic_$comicId"
                if (response.isSubscribed) {
                    FirebaseMessaging.getInstance().subscribeToTopic(topicName)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) Log.i("FCM Topic", "Subscribed to $topicName")
                        }
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topicName)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) Log.i("FCM Topic", "Unsubscribed from $topicName")
                        }
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Subscription", "Failed to toggle: $error")
            }
        }
    }
    fun subscribeToAuthor(authorId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.subscribeToAuthor(AuthorRequest(authorId))
                }
                getSubscribedAuthors()
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Subscription", "Failed to subscribe to author: $error")
            }
        }
    }
    fun openChapter(comic: Comic, chapterId: String = "") {
        viewModelScope.launch {
            _pageCount.value = 0
            withContext(Dispatchers.IO) {
                repository.updateLastRead(comic.id)
            }
            try {
                val pages = withContext(Dispatchers.IO) {
                    repository.getPagesForChapter(chapterId, comic.id)
                }
                chapterPages = pages
                _pageCount.value = pages.size
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Reader", "Failed to load chapter: $error")
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
            withContext(Dispatchers.IO) {
                repository.updateProgress(chapterId, newPageIndex)
            }
        }
    }
    fun loginUser(credentials: LoginCredentials, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                clearProfileData()
                val authRequest = AuthRequest(email = credentials.email, password = credentials.password)
                val response = withContext(Dispatchers.IO) {
                    val res = RetrofitClient.comicApiService.login(authRequest)
                    RetrofitClient.preferenceManager.saveTokens(res.accessToken, res.refreshToken)
                    res
                }

                resetDatabaseAndRepository()

                val userId = response.userId
                onUserAuthenticated(userId)
                _userProfile.first { it != null }
                _currentComic.value = null
                onSuccess()
                Log.i("Success", "Logged in successfully")
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Failure", "Login failed: $error")
            }
        }
    }
    fun logoutUser(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val refreshToken = RetrofitClient.preferenceManager.getRefreshTokenSync()
                    if (refreshToken != null) {
                        RetrofitClient.comicApiService.logout(RefreshTokenRequest(refreshToken))
                    }
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Logout", "Server logout failed: $error")
            } finally {
                withContext(Dispatchers.IO) {
                    RetrofitClient.preferenceManager.clearTokens()
                    resetDatabaseAndRepository()
                }
                FirebaseMessaging.getInstance().deleteToken()
                clearProfileData()
                disconnectChatSocket()
                _currentComic.value = null
                onLogoutComplete()
            }
        }
    }
    fun registerNewUser(newUser: AuthRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                clearProfileData()
                val response = withContext(Dispatchers.IO) {
                    val res = RetrofitClient.comicApiService.signUp(newUser)
                    RetrofitClient.preferenceManager.saveTokens(res.accessToken, res.refreshToken)
                    res
                }

                resetDatabaseAndRepository()

                val userId = response.userId
                onUserAuthenticated(userId)
                _userProfile.first { it != null }
                onSuccess()
                Log.i("Success", "Signed up successfully")
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Failure", "Signup failed: $error")
            }
        }
    }

    private fun clearProfileData() {
        _userProfile.value = null
        _userPosts.value = emptyList()
        _userWorks.value = emptyList()
        _targetUserProfile.value = null
        _targetUserPosts.value = emptyList()
        _targetUserWorks.value = emptyList()
    }

    private suspend fun resetDatabaseAndRepository() {
        withContext(Dispatchers.IO) {
            CanvasDatabase.resetDatabase()
            repository.refreshDAOs()
        }
    }

    fun updateBio(bio: String) {
        viewModelScope.launch {
            try {
                val userId = _userProfile.value?.id
                if (userId != null) {
                    if (userId.isEmpty()) return@launch
                    val response = withContext(Dispatchers.IO) {
                        val res =
                            RetrofitClient.comicApiService.updateBio(userId, UpdateBioRequest(bio))
                        res
                    }
                    _userProfile.value = _userProfile.value?.copy(bio = response.message)

                    Log.i("Success", "Bio updated successfully")
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("AppViewModel", "Failed to update bio: $error")
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            try {
                val userId = _userProfile.value?.id
                if (userId?.isEmpty() == true) return@launch

                if (userId != null) {
                    val response = withContext(Dispatchers.IO) {
                        val res = RetrofitClient.comicApiService.updateUsername(
                            userId,
                            UpdateUsernameRequest(newUsername)
                        )
                        res
                    }
                    _userProfile.update { currentProfile ->
                        currentProfile?.copy(username = response.message)
                    }
                    _userProfile.value = _userProfile.value?.copy(username = response.message)
                    Log.i("Success", "Username updated successfully")
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("AppViewModel", "Failed to update username: $error")
            }
        }
    }
    fun updateInterests(interests: List<String>) {
        viewModelScope.launch {
            try {
                val userId = _userProfile.value?.id
                if (userId?.isEmpty() == true) return@launch

                if (userId != null) {
                    withContext(Dispatchers.IO) {
                        RetrofitClient.comicApiService.updateInterests(
                            userId,
                            UpdateInterestsRequest(interests)
                        )
                    }
                }
                Log.i("AppViewModel", "Interests updated successfully")
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("AppViewModel", "Failed to update interests: $error")
            }
        }
    }
    fun updateAvatar(avatarUrl: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.comicApiService.updateAvatar(avatarUrl)
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Avatar", "Failed to update Avatar $error")
            }
        }
    }

    fun uploadAvatar(selectedUri: Uri) {
        viewModelScope.launch {
            try {
                MediaManager.get().upload(selectedUri)
                    .unsigned(BuildConfig.CLOUDINARY_PRESET)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.i("Cloudinary", "Upload started")
                        }
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        }
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val publicUrl = resultData["secure_url"] as String
                            Log.i("Cloudinary", "Upload success: $publicUrl")

                            viewModelScope.launch {
                                _userProfile.value = _userProfile.value?.copy(avatarUrl = publicUrl)

                                try {
                                    withContext(Dispatchers.IO) {
                                        updateAvatar(publicUrl)
                                    }

                                        Log.i("Cloudinary", "Avatar updated to $publicUrl")
                                } catch (e: Exception) {
                                    val error = e.message
                                    _errorMessage.value = error
                                    Log.e("Cloudinary", "Failed to sync avatar with server $error")
                                }
                            }
                        }
                        override fun onError(requestId: String, error: ErrorInfo) {
                            val err = error.description
                            _errorMessage.value = err
                            Log.e("Cloudinary", "Upload error: $err")
                        }
                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                        }
                    }).dispatch()

            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("AvatarUpload", "Failed to upload avatar: $error")
            }
        }
    }
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    fun getRecommendation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _catalog.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getRecommendation()
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Comic error", "Failed to load catalog: $error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _trending.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getTrending()
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Trending Comic", " Failed to load trending comic: $error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getComicById(comicId: String) {
        viewModelScope.launch {
            try {
                _currentComic.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getComicById(comicId)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Comic error", "Failed to get Comic: $error")
            }
        }
    }

    fun uploadNewChapters(
        context: Context,
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
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadSuccess.value = false
            try {
                val uploadedAudioUrls = if (audioUris.isNotEmpty()) {
                    audioUris.map { uri ->
                        async { uploadFileToCloudinary(uri) }
                    }.awaitAll()
                } else {
                    emptyList()
                }
                
                val primaryAudioUrl = uploadedAudioUrls.firstOrNull()
                
                val startingChapterNumber = if (comicId != null) {
                    val existingChapters = withContext(Dispatchers.IO) {
                        repository.getChaptersForComic(comicId).first()
                    }
                    existingChapters.maxOfOrNull { it.chapterNumber ?: 0f } ?: 0f
                } else {
                    0f
                }
                val chaptersData = chapterUris.mapIndexed { index, uri ->
                    processAndUploadChapter(context, uri, startingChapterNumber + index + 1f).apply {
                        // Assign audio from the list if available, otherwise fallback to primary
                        this.audioUrl = uploadedAudioUrls.getOrNull(index) ?: primaryAudioUrl
                    }
                }

                if (comicId == null) {
                    val coverUrl = selectedCover?.let { uploadFileToCloudinary(it) }
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.comicApiService.registerNewComic(
                            RegisterComicRequest(
                                title = title,
                                authors = authors,
                                description = description,
                                coverUrl = coverUrl,
                                scrollDirection = scrollDirection,
                                pgRating = pgRating,
                                genres = genres,
                                chapters = chaptersData,
                                audioUri = primaryAudioUrl
                            )
                        )
                    }
                    Log.i("Successful Upload", "New Comic Success: ${response.message}")
                } else {
                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.comicApiService.registerChapters(
                            comicId = comicId,
                            request = RegisterChaptersRequest(chaptersData)
                        )
                    }
                    Log.i("Successful Upload", "Chapter Upload Success: ${response.message}")
                }
                _uploadSuccess.value = true
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Upload Error", "Upload failed", e)
            } finally {
                _isUploading.value = false
            }
        }
    }

    private suspend fun processAndUploadChapter(
        context: Context,
        uri: Uri,
        chapterNumber: Float
    ): ChapterUploadData = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "upload_extract_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        var chapterTitle = "Chapter ${chapterNumber.toInt()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                chapterTitle = cursor.getString(nameIndex).substringBeforeLast(".")
            }
        }

        val pageFiles = mutableListOf<File>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        val file = File(tempDir, entry.name.split("/").last())
                        FileOutputStream(file).use { output -> zipInput.copyTo(output) }
                        pageFiles.add(file)
                    }
                    entry = zipInput.nextEntry
                }
            }
        }
        pageFiles.sortBy { it.name }

        val pageUrls = pageFiles.map { file ->
            async {
                uploadFileToCloudinary(Uri.fromFile(file))
            }
        }.awaitAll()

        tempDir.deleteRecursively()

        ChapterUploadData(
            title = chapterTitle,
            chapterNumber = chapterNumber,
            pageCount = pageUrls.size,
            pageUrls = pageUrls
        )
    }

    private suspend fun uploadFileToCloudinary(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .unsigned(BuildConfig.CLOUDINARY_PRESET)
            .option("resource_type", "auto")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    continuation.resume(resultData["secure_url"] as String)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(Exception(error.description))
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    fun likeChapter(comicId: String, chapterId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.likeChapter(comicId, chapterId)
                }
                val currentChapters = _chapters.value
                _chapters.value = currentChapters.map { chapter ->
                    if (chapter.id == chapterId) {
                        chapter.copy(
                            isLiked = !chapter.isLiked,
                        )
                    } else chapter
                }
                Log.d("Chapter like", "Success ${response.message}")
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Chapter like", "Failed to like Chapter $error")
            }
        }
    }

    fun resetUploadState() {
        _uploadSuccess.value = false
        _isUploading.value = false
    }
    fun getChatMessages(conversationId: String) {
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val messages = RetrofitClient.comicApiService.getChatMessages(conversationId)
                    repository.saveMessages(messages.map { it.copy(conversationId = conversationId) })
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Chat", "Failed to fetch messages: $error")
            }
        }
    }

    fun resetChatState() {
        _currentConversationId.value = null
    }

    fun clearChatHistory(conversationId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteMessagesForConversation(conversationId)
            }
        }
    }
    fun sendMessage(
        conversationId: String,
        content: String,
        isSpoiler: Boolean = false,
        sharedId: String? = null,
        sharedType: ShareType? = null,
        attachment: Uri? = null
    ) {
        viewModelScope.launch {
            try {
                var mediaUrl: String? = null
                var mediaType: String? = null

                if (attachment != null) {
                    _isUploading.value = true
                    mediaUrl = uploadFileToCloudinary(attachment)
                    mediaType = if (mediaUrl.endsWith(".mp4") || mediaUrl.endsWith(".mov")) "VIDEO" else "IMAGE"
                    _isUploading.value = false
                }

                val encryptedContent = encryptMessage(content)
                val tempMessageId = UUID.randomUUID().toString()

                val newMessage = ChatMessage(
                    id = tempMessageId,
                    conversationId = conversationId,
                    senderId = userProfile.value?.id ?: "",
                    content = encryptedContent,
                    timestamp = 0L,
                    isEncrypted = true,
                    isSpoiler = isSpoiler,
                    replyToMessageId = _replyingToMessage.value?.id,
                    sharedId = sharedId,
                    sharedType = sharedType,
                    imageUrl = if (mediaType == "IMAGE") mediaUrl else null,
                    videoUrl = if (mediaType == "VIDEO") mediaUrl else null,
                    mediaType = mediaType
                )

                repository.saveMessage(newMessage)
                val jsonMessage = socketJson.encodeToString(newMessage)
                chatWebSocket?.send(jsonMessage)

                _replyingToMessage.value = null
                _sharedContent.value = null
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Message error", "Failed to send message over socket: $error")
            }
        }
    }
    private fun encryptMessage(content: String): String {
        return CryptoUtils.encrypt(content)
    }

    fun decryptMessage(content: String): String {
        return CryptoUtils.decrypt(content)
    }
    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            try {
                // Optimistic local delete
                withContext(Dispatchers.IO) {
                    repository.deleteMessageById(messageId)
                    RetrofitClient.comicApiService.deleteMessage(conversationId, messageId)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Message error", "Failed to delete message: $error")
            }
        }
    }
    fun editMessage(
        conversationId: String,
        messageId: String,
        content: String
    ) {
        viewModelScope.launch {
            try {
                val encryptedContent = encryptMessage(content)
                withContext(Dispatchers.IO) {
                    repository.updateMessageContent(messageId, content)
                    val updatedMessage = ChatMessage(id = messageId, content = encryptedContent, isEncrypted = true)
                    RetrofitClient.comicApiService.updateMessage(conversationId, messageId, updatedMessage)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Message error", "Failed to edit message: $error")
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateMessageReadStatus(messageId, true)
            }
        }
    }

    fun getChatRequests() {
        viewModelScope.launch {
            try {
                _chatRequests.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getChatRequests()
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Request error", "Failed to load requests: $error")
            }
        }
    }
    fun acceptFriend(requestId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.acceptChatRequest(requestId)
                }
                _chatRequests.value = _chatRequests.value.filter { it.id != requestId }
                getInbox()
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Request error", "Failed to accept request: $error")
            }
        }
    }
    fun declineFriend(requestId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.declineChatRequest(requestId)
                }
                _chatRequests.value = _chatRequests.value.filter { it.id != requestId }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Request error", "Failed to decline request: $error")
            }
        }
    }
    fun unAddFriend(userId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.unfriendUser(userId)
                    _currentConversationId.value?.let { conversationId ->
                        repository.deleteMessagesForConversation(conversationId)
                        repository.deleteConversationById(conversationId)
                    }
                }
                resetChatState()
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("ChatInbox", "Error unfriending", e)
            }
        }
    }
    fun getInbox() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val conversations = RetrofitClient.comicApiService.getInbox()
                    repository.saveConversations(conversations)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("ChatInbox", "Error loading conversations", e)
            }
        }
    }
    fun searchComics(query: String) {
        viewModelScope.launch {
            try {
                _catalog.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.searchComics(query)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Search failed: ", "$error")
            }
        }
    }
    fun getChaptersForComic(comic: Comic) {
        chaptersJob?.cancel()
        chaptersJob = viewModelScope.launch {
            try {
                repository.getChaptersForComic(comic.id).collect { localChapters ->
                    _chapters.value = localChapters
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Chapter error", "Failed to load chapters: $error")
            }
        }

        if (!comic.isLocalSideload) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.syncRemoteChaptersForComic(comic)
                }
            }
        }
    }
    fun getCommunityPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _communityPosts.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getCommunityPosts()
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Post error", "Failed to load community posts: $error")
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun getPostComments(postId: String) {
        viewModelScope.launch {
            try {
                _postComments.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getPostComments(postId)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Comment error", "Failed to load comments: $error")
            }
        }
    }
    fun getChapterComments(chapterId: String) {
        viewModelScope.launch {
            try {
                _chapterComments.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getChapterComments(chapterId)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Comment error", "Failed to load comic comments: $error")
            }
        }
    }
    fun likePost(postId: String) {
        val currentPosts = _communityPosts.value
        _communityPosts.value = currentPosts.map { post ->
            if (post.id == postId) {
                post.copy(
                    isLiked = !post.isLiked,
                    likesCount = if (post.isLiked) post.likesCount - 1 else post.likesCount + 1
                )
            } else post
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.likePost(postId)
                }
            } catch (e: Exception) {
                _communityPosts.value = currentPosts
                val error = e.message
                _errorMessage.value = error
                Log.e("Post error", "Failed to sync like: $error")
            }
        }
    }
    fun likeComment(commentId: String, commentLocation: CommentLocation) {
        when (commentLocation) {
            CommentLocation.ON_CHAPTER -> {
                val currentComments = _chapterComments.value
                _chapterComments.value = currentComments.map { comment ->
                    if (comment.id == commentId) {
                        comment.copy(
                            isLiked = !comment.isLiked,
                            likesCount = if (comment.isLiked) comment.likesCount - 1 else comment.likesCount + 1
                        )
                    } else comment
                }
                viewModelScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            RetrofitClient.comicApiService.likeChapterComment(commentId)
                        }
                    } catch (e: Exception) {
                        _chapterComments.value = currentComments
                        val error = e.message
                        _errorMessage.value = error
                        Log.e("Comment error", "Failed to like comment on chapter: $error")
                    }
                }
            }
            CommentLocation.ON_POST -> {
                val currentComments = _postComments.value
                _postComments.value = currentComments.map { comment ->
                    if (comment.id == commentId) {
                        comment.copy(
                            isLiked = !comment.isLiked,
                            likesCount = if (comment.isLiked) comment.likesCount - 1 else comment.likesCount + 1
                        )
                    } else comment
                }
                viewModelScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            RetrofitClient.comicApiService.likePostComment(commentId)
                        }
                    } catch (e: Exception) {
                        _postComments.value = currentComments
                        val error = e.message
                        _errorMessage.value = error
                        Log.e("Comment error", "Failed to like comment on post: $error")
                    }
                }
            }
        }
    }
    fun bookmarkPost(postId: String) {
        val currentPosts = _communityPosts.value
        _communityPosts.value = currentPosts.map { post ->
            if (post.id == postId) {
                post.copy(
                    isBookmarked = !post.isBookmarked
                )
            } else post
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.bookmarkPost(postId)
                }
            } catch (e: Exception) {
                _communityPosts.value = currentPosts
                val error = e.message
                _errorMessage.value = error
                Log.e("Post error", "Failed to bookmark post: $error")
            }
        }
    }
    fun makePost(
        title: String?,
        isSpoiler: Boolean,
        postContent: String,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val shared = _sharedContent.value

                val request = PostRequest(
                    title = title,
                    isSpoiler = isSpoiler,
                    content = postContent,
                    tags = tags,
                    sharedId = shared?.id,
                    sharedType = shared?.type,
                    sharedTitle = shared?.title,
                    sharedPreview = shared?.previewText,
                    sharedImageUrl = null
                )

                withContext(Dispatchers.IO) {
                    val response = RetrofitClient.comicApiService.makePost(request)
                    Log.i("Post", "Post created: ${response.message}")
                }

                getCommunityPosts()
                _sharedContent.value = null
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Post error", "Failed to make post: $error")
            }
        }
    }
    fun addPostComment(
        postId: String,
        content: String,
        isSpoiler: Boolean,
        mentionedUserIds: List<String> = emptyList(),
        parentId: String? = null
    ) {
        viewModelScope.launch {
            try {
                val shared = _sharedContent.value

                val newComment = CommentRequest(
                    content = content,
                    isSpoiler = isSpoiler,
                    mentionedUserIds = mentionedUserIds,
                    parentId = parentId,
                    sharedId = shared?.id,
                    sharedType = shared?.type,
                    sharedTitle = shared?.title,
                    sharedPreview = shared?.previewText
                )

                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.addPostComment(postId, newComment)
                }
                getPostComments(postId)
                _sharedContent.value = null
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Comment error", "Failed to post comment: $error")
            }
        }
    }
    fun addChapterComment(
        chapterId: String,
        content: String,
        isSpoiler: Boolean,
        mentionedUserIds: List<String> = emptyList(),
        parentId: String? = null
    ) {
        viewModelScope.launch {
            try {
                val shared = _sharedContent.value

                val newComment = CommentRequest(
                    content = content,
                    mentionedUserIds = mentionedUserIds,
                    parentId = parentId,
                    isSpoiler = isSpoiler,
                    sharedId = shared?.id,
                    sharedType = shared?.type,
                    sharedTitle = shared?.title,
                    sharedPreview = shared?.previewText
                )

                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.addChapterComment(chapterId, newComment)
                }
                getChapterComments(chapterId)
                _sharedContent.value = null
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Comment error", "Failed to post comic comment: $error")
            }
        }
    }
    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                _userSuggestions.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.searchUsers(query)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("SearchUsers", "Failed: $error")
            }
        }
    }
    fun sendChatRequest(receiverId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.sendChatRequest(receiverId)
                }
                Log.i("ChatRequest", "Request sent: ${response.message}")
                onSuccess()
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("ChatRequest", "Failed to send request: $error")
            }
        }
    }
    fun clearUserSuggestions() {
        _userSuggestions.value = emptyList()
    }

    fun getSubscribedAuthors() {
        viewModelScope.launch {
            try {
                _subscribedAuthors.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getSubscribedAuthors()
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("SubscribedAuthors", "Failed: $error")
            }
        }
    }

    fun toggleAuthorFilter(authorId: String) {
        val current = _selectedAuthorIds.value
        _selectedAuthorIds.value = if (current.contains(authorId)) {
            current - authorId
        } else {
            current + authorId
        }
    }

    fun getUserWorks(userId: String) {
        viewModelScope.launch {
            try {
                if (userId == "") {
                    return@launch
                }
                val works = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getUserWorks(userId)
                }
                val currentUserId = _userProfile.value?.id
                if (userId == currentUserId) {
                    _userWorks.value = works
                } else {
                    _targetUserWorks.value = works
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("AppViewModel", "Failed to get user works: $error")
            }
        }
    }

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("ProfileDebug", "Attempting to fetch profile")
                if (userId.isBlank()) {
                    Log.e("Profile error", "Cannot fetch profile: userId is empty!")
                    return@launch
                }
                val profile = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getUserProfile(userId)
                }
                val posts = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getUserPosts(userId)
                }
                getUserWorks(userId)

                val currentUserId = _userProfile.value?.id?.trim() ?: userId.trim()
                if (userId.trim().equals(currentUserId, ignoreCase = true)) {
                    _userProfile.update { profile }
                    _userPosts.value = posts

                    withContext(Dispatchers.IO) {
                        RetrofitClient.preferenceManager.saveUserData(
                            profile.id,
                            profile.username,
                            profile.avatarUrl,
                            profile.bio
                        )
                    }
                } else {
                    _targetUserProfile.value = profile
                    _targetUserPosts.value = posts
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Profile error: ", "$error")
            }
        }
    }

    fun toggleProfilePrivacy(userId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.toggleProfilePrivacy(userId)
                }
                _userProfile.update { currentProfile ->
                    currentProfile?.copy(isPrivate = !currentProfile.isPrivate)
                }
                Log.i("Profile", "Privacy toggled: ${response.message}")
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Profile", "Failed to toggle privacy: $error")
            }
        }
    }

    fun registerFcmToken(token: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.registerFcmToken(FcmTokenRequest(token))
                }
                Log.i("FCM", "Token registered successfully")
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("FCM", "Failed to register token: $error")
            }
        }
    }

    fun fetchAndRegisterFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i("fcm", "fcm fetch success")
                task.result?.let { registerFcmToken(it) }
            }
        }
    }
    fun handleNavigation(route: String) {
        _pendingNavigation.value = route
    }

    fun onNavigationHandled() {
        _pendingNavigation.value = null
    }

    fun updateInboxSearchQuery(query: String) {
        _inboxSearchQuery.value = query
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                RetrofitClient.preferenceManager.setDarkTheme(enabled)
            }
        }
    }

    fun clearLocalDatabase() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearAllData()
            }
        }
    }

    fun setSharedContent(content: SharedContent?) {
        _sharedContent.value = content
    }

    fun loadAndNavigateToSharedComic(comicId: String) {
        viewModelScope.launch {
            var comicToLoad = myLibrary.value.find { it.id == comicId }

            if (comicToLoad == null) {
                comicToLoad = catalog.value.find { it.id == comicId }
            }
            if (comicToLoad != null) {
                setCurrentComic(comicToLoad)
                handleNavigation(Screen.Overview.createRoute(comicId))
            } else {
                try {
                    getComicById(comicId)
                    handleNavigation(Screen.Overview.createRoute(comicId))
                } catch (e: Exception) {
                    val error = e.message
                    _errorMessage.value = error
                    Log.e("Navigation", "Error fetching comic $comicId: $error")
                }
            }
        }
    }

    fun loadComicById(comicId: String) {
        viewModelScope.launch {
            _currentComic.value = null

            var comicToLoad = myLibrary.value.find { it.id == comicId } ?:
                             catalog.value.find { it.id == comicId } ?:
                             trending.value.find { it.id == comicId } ?:
                             userWorks.value.find { it.id == comicId } ?:
                             targetUserWorks.value.find { it.id == comicId }

            if (comicToLoad == null) {
                try {
                    comicToLoad = withContext(Dispatchers.IO) {
                        RetrofitClient.comicApiService.getComicById(comicId)
                    }
                } catch (e: Exception) {
                    Log.e("AppViewModel", "Failed to fetch comic $comicId from API", e)
                }
            }

            if (comicToLoad != null) {
                _currentComic.value = comicToLoad
            } else {
                Log.e("Comic error", "Comic not found or unavailable.")
                _errorMessage.value = "Failed to load comic details."
            }
        }
    }

    fun findUserByUsername(username: String, onFound: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.searchUsers(username)
                }
                results.find { it.username.equals(username, ignoreCase = true) }?.let {
                    onFound(it.id)
                }
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("AppViewModel", "User not found", e)
            }
        }
    }

    fun rateComic(comicId: String, rating: Float) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.rateComic(comicId, rating)
                }
                _currentComic.update { comic ->
                    if (comic?.id == comicId) comic.copy(rating = rating) else comic
                }
                
                _catalog.update { list -> list.map { if (it.id == comicId) it.copy(rating = rating) else it } }
                _trending.update { list -> list.map { if (it.id == comicId) it.copy(rating = rating) else it } }
                _userWorks.update { list -> list.map { if (it.id == comicId) it.copy(rating = rating) else it } }
                _targetUserWorks.update { list -> list.map { if (it.id == comicId) it.copy(rating = rating) else it } }

            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Comic error", "Failed to submit rating: $error")
            }
        }
    }

    fun markChapterAsRead(comicId: String, chapterId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.markChapterAsRead(comicId, chapterId)
                }
                val currentChapters = _chapters.value
                _chapters.value = currentChapters.map { chapter ->
                    if (chapter.id == chapterId) {
                        chapter.copy(
                            isRead = true,
                        )
                    } else chapter
                }
                Log.d("Chapter read", "Success ${response.message}")
            } catch(e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Chapter error", "Failed to mark as read: $error")
            }
        }
    }
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.markNotificationAsRead(notificationId)
                }
                Log.d("Notification read", "Success ${response.message}")
            } catch(e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Notification error", "Failed to mark as read: $error")
            }
        }
    }
    fun clearNotifications() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearNotifications()
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteNotificationById(notificationId)
            }
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String?,
        reason: String,
        details: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = ReportRequest(
                    targetType = targetType,
                    targetId = targetId,
                    reason = reason,
                    details = details
                )
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.submitReport(request)
                }

                Log.i("Report", "Successfully reported $targetType: $reason response: $response")
                onSuccess()
            } catch (e: Exception) {
                val error = e.message
                _errorMessage.value = error
                Log.e("Report error", "Failed to submit report: $error")
            }
        }
    }
    fun connectChatSocket() {
        if (chatWebSocket != null) return

        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val incomingMessage = socketJson.decodeFromString<ChatMessage>(text)

                    viewModelScope.launch(Dispatchers.IO) {
                        repository.saveMessage(incomingMessage)

                        // update the Conversation's lastMessage here

                    }

                } catch (e: Exception) {
                    val error = e.message
                    _errorMessage.value = error
                    Log.e("WebSocket", "Failed to decode message: $error")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val error = t.message
                _errorMessage.value = error
                Log.e("WebSocket", "Connection failed: $error")
            }
        }

        chatWebSocket = RetrofitClient.createChatWebSocket(listener)
    }

    fun disconnectChatSocket() {
        chatWebSocket?.close(1000, "App backgrounded")
        chatWebSocket = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
fun convertTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return date.format(formatter)
}