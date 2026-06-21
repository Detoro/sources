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
import java.util.zip.ZipInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.Boolean
import kotlin.collections.map

enum class SearchSource {
    LOCAL, ONLINE
}

@OptIn(FlowPreview::class)
class AppViewModel(
    private val repository: ComicRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow(AuthResponse())
    val currentUser: StateFlow<AuthResponse> = _currentUser.asStateFlow()

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

    private val _chapterComments = MutableStateFlow<List<Comment>>(emptyList())
    val chapterComments = _chapterComments.asStateFlow()
    private val _pageCount = MutableStateFlow(0)
    val pageCount = _pageCount.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

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

    private val _userRating = MutableStateFlow(0)
    val userRating: StateFlow<Int> = _userRating.asStateFlow()

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
        getCatalog()
        getChatRequests()
        getSubscribedAuthors()

        viewModelScope.launch {
            if (RetrofitClient.preferenceManager.getTokenSync() != null) {
                repository.syncSubscriptions()
            }
        }

        val userData = RetrofitClient.preferenceManager.getUserDataSync()
        if (userData.userId != null && userData.username != null) {
            _currentUser.value = AuthResponse(
                userId = userData.userId,
                username = userData.username,
                avatarUrl = userData.avatarUrl
            )
            _userProfile.value = UserProfile(
                id = userData.userId,
                username = userData.username,
                avatarUrl = userData.avatarUrl,
                bio = userData.bio
            )
            getUserProfile(userData.userId)
        }

        viewModelScope.launch {
            RetrofitClient.preferenceManager.isDarkTheme.collect { enabled ->
                _isDarkTheme.value = enabled
            }
        }

        if (RetrofitClient.preferenceManager.getTokenSync() != null) {
            fetchAndRegisterFcmToken()
        }

        viewModelScope.launch {
            NotificationEventBus.notifications.collect { newNotification ->
                withContext(Dispatchers.IO) {
                    repository.saveNotification(newNotification)
                }
            }
        }

        viewModelScope.launch {
            combine(_searchQuery, _searchSource) { query, source ->
                query to source
            }
                .debounce(500L)
                .filter { it.first.isNotBlank() && it.second == SearchSource.ONLINE }
                .collectLatest { (readyQuery, _) ->
                    searchComics(readyQuery)
                }
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
                _errorMessage.value = "Failed to remove comic: ${e.message}"
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
                Log.e("Subscription", "Failed to toggle: ${e.message}")
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
                Log.e("Subscription", "Failed to subscribe to author: ${e.message}")
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
                    RetrofitClient.preferenceManager.saveToken(res.token)
                    RetrofitClient.preferenceManager.saveUserData(
                        res.userId,
                        res.username,
                        res.avatarUrl
                    )
                    res
                }
                _currentUser.value = response
                getUserProfile(response.userId)
                fetchAndRegisterFcmToken()
                repository.syncSubscriptions()
                _currentComic.value = null
                onSuccess()
                Log.i("Success", "Logged in successfully as ${response.username}!")
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("Failure", "Login failed: ${e.message}")
            }
        }
    }
    fun logoutUser(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            _currentUser.value = AuthResponse()
            withContext(Dispatchers.IO) {
                RetrofitClient.preferenceManager.clearToken()
            }
            FirebaseMessaging.getInstance().deleteToken()
            clearProfileData()
            _currentComic.value = null
            onLogoutComplete()
        }
    }
    fun registerNewUser(newUser: AuthRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                clearProfileData()
                val response = withContext(Dispatchers.IO) {
                    val res = RetrofitClient.comicApiService.signUp(newUser)
                    RetrofitClient.preferenceManager.saveToken(res.token)
                    RetrofitClient.preferenceManager.saveUserData(
                        res.userId,
                        res.username,
                        res.avatarUrl
                    )
                    res
                }
                _currentUser.value = response
                getUserProfile(response.userId)
                fetchAndRegisterFcmToken()
                onSuccess()
                Log.i("Success", "Sign up successfully as ${response.username}!")
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("Failure", "Signup failed: ${e.message}")
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

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateBio(bio: String) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value.userId
                if (userId.isEmpty()) return@launch
                val response = withContext(Dispatchers.IO) {
                    val res = RetrofitClient.comicApiService.updateBio(userId, UpdateBioRequest(bio))
                    RetrofitClient.preferenceManager.saveUserData(
                        _currentUser.value.userId,
                        _currentUser.value.username,
                        _currentUser.value.avatarUrl,
                        res.message
                    )
                    res
                }
                _userProfile.value = _userProfile.value?.copy(bio = response.message)

                Log.i("Success", "Bio updated successfully")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to update bio: ${e.message}")
                _errorMessage.value = "Failed to update bio"
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value.userId
                if (userId.isEmpty()) return@launch

                val response = withContext(Dispatchers.IO) {
                    val res = RetrofitClient.comicApiService.updateUsername(userId, UpdateUsernameRequest(newUsername))
                    RetrofitClient.preferenceManager.saveUserData(
                        _currentUser.value.userId,
                        res.message,
                        _currentUser.value.avatarUrl
                    )
                    res
                }
                _userProfile.value?.username = response.message
                _currentUser.value = _currentUser.value.copy(username = response.message)
                Log.i("Success", "Username updated successfully")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to update username: ${e.message}")
                _errorMessage.value = "Failed to update username"
            }
        }
    }

    fun updateInterests(interests: List<String>) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value.userId
                if (userId.isEmpty()) return@launch

                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.updateInterests(userId, UpdateInterestsRequest(interests))
                }
                Log.i("AppViewModel", "Interests updated successfully")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to update interests: ${e.message}")
                _errorMessage.value = "Failed to update interests"
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
                                _currentUser.value = _currentUser.value.copy(avatarUrl = publicUrl)

                                try {
                                    withContext(Dispatchers.IO) {
                                        RetrofitClient.comicApiService.updateAvatar(publicUrl)
                                        RetrofitClient.preferenceManager.saveUserData(
                                            _currentUser.value.userId,
                                            _currentUser.value.username,
                                            publicUrl,
                                            _userProfile.value?.bio
                                        )
                                    }

                                    Log.i("Cloudinary", "Avatar updated to $publicUrl")
                                } catch (e: Exception) {
                                    Log.e("Cloudinary", "Failed to sync avatar with server", e)
                                }
                            }
                        }
                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e("Cloudinary", "Upload error: ${error.description}")
                        }
                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                        }
                    }).dispatch()

            } catch (e: Exception) {
                Log.e("AvatarUpload", "Failed to upload avatar: ${e.message}")
            }
        }
    }
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    fun getCatalog() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _catalog.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getCatalog()
                }
                Log.i("comics", "${_catalog.value}")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load catalog: ${e.message}"
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
                _errorMessage.value = "Failed to get Comic: ${e.message}"
                Log.e("Failed to get Comic", "${e.message}")
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
            _errorMessage.value = null
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
                _errorMessage.value = "Upload failed: ${e.message}"
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
            .unsigned("dxrcey4p")
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
                Log.e("Chapter like", "Failed to like Chapter ${e.message}")
            }
        }
    }

    fun resetUploadState() {
        _uploadSuccess.value = false
        _errorMessage.value = null
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
                Log.e("Chat", "Failed to fetch messages: ${e.message}")
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
        targetUserId: String,
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
                val newMessage = ChatMessage(
                    id = "",
                    conversationId = conversationId,
                    senderId = _currentUser.value.userId,
                    content = encryptedContent,
                    timestamp = System.currentTimeMillis(),
                    isEncrypted = true,
                    isSpoiler = isSpoiler,
                    sharedId = sharedId,
                    sharedType = sharedType,
                    imageUrl = if (mediaType == "IMAGE") mediaUrl else null,
                    videoUrl = if (mediaType == "VIDEO") mediaUrl else null,
                    mediaType = mediaType
                )
                withContext(Dispatchers.IO) {
                    repository.saveMessage(newMessage)
                    val serverResponse = RetrofitClient.comicApiService.sendMessage(conversationId, targetUserId, newMessage)
                    Log.i("Message status", serverResponse.message)
                    repository.deleteMessageById(newMessage.id)
                }
                getChatMessages(conversationId)
                _sharedContent.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send message: ${e.message}"
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
                _errorMessage.value = e.message
                Log.e("Message error", "Failed to delete message: ${e.message}")
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
                // Update local DB immediately for better UX
                withContext(Dispatchers.IO) {
                    repository.updateMessageContent(messageId, content)
                    val updatedMessage = ChatMessage(id = messageId, content = encryptedContent, isEncrypted = true)
                    RetrofitClient.comicApiService.updateMessage(conversationId, messageId, updatedMessage)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("Message error", "Failed to edit message: ${e.message}")
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
                _errorMessage.value = "Failed to load requests: ${e.message}"
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
                _errorMessage.value = "Failed to accept request: ${e.message}"
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
                _errorMessage.value = "Failed to decline request: ${e.message}"
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
                _errorMessage.value = "Failed to unfriend: ${e.message}"
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
                _errorMessage.value = "Search failed: ${e.message}"
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
                _errorMessage.value = "Failed to load chapters: ${e.message}"
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
                _errorMessage.value = "Failed to load community: ${e.message}"
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
                _errorMessage.value = "Failed to load comments: ${e.message}"
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
                _errorMessage.value = "Failed to load comic comments: ${e.message}"
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
                _errorMessage.value = "Failed to sync like: ${e.message}"
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
                        _errorMessage.value = "Failed to like comment on chapter: ${e.message}"
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
                        _errorMessage.value = "Failed to like comment on post: ${e.message}"
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
                _errorMessage.value = "Failed to bookmark post: ${e.message}"
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
                _errorMessage.value = "Failed to make post: ${e.message}"
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
                _errorMessage.value = "Failed to post comment: ${e.message}"
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
                _errorMessage.value = "Failed to post comic comment: ${e.message}"
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
                Log.e("SearchUsers", "Failed: ${e.message}")
            }
        }
    }
    fun sendFriendRequest(receiverId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.sendChatRequest(receiverId)
                }
                Log.i("FriendRequest", "Request sent: ${response.message}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("FriendRequest", "Failed to send: ${e.message}")
                _errorMessage.value = "Failed to send request"
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
                Log.e("SubscribedAuthors", "Failed: ${e.message}")
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
        Log.i("AuthorFilter", "Selected authors: ${_selectedAuthorIds.value}")
    }

    fun getUserWorks(userId: String) {
        viewModelScope.launch {
            try {
                val works = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getUserWorks(userId)
                }
                val currentUserId = _currentUser.value.userId
                if (userId == currentUserId) {
                    _userWorks.value = works
                } else {
                    _targetUserWorks.value = works
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to get user works: ${e.message}")
                _errorMessage.value = "Failed to load works"
            }
        }
    }

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val profile = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getUserProfile(userId)
                }
                val posts = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getUserPosts(userId)
                }
                getUserWorks(userId)

                val currentUserId = _currentUser.value.userId.trim()
                if (userId.trim().equals(currentUserId, ignoreCase = true)) {
                    _userProfile.value = profile
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
                _errorMessage.value = "Failed to load profile: ${e.message}"
            }
        }
    }

    fun toggleProfilePrivacy(userId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.toggleProfilePrivacy(userId)
                }
                _userProfile.value = _userProfile.value?.copy(isPrivate = !_userProfile.value!!.isPrivate)
                Log.i("Profile", "Privacy toggled: ${response.message}")
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle privacy: ${e.message}"
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
                Log.e("FCM", "Failed to register token: ${e.message}")
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
        Log.i("Shared content", "$content")
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
                    _errorMessage.value = "Comic not found or unavailable."
                    Log.e("Navigation", "Error fetching comic $comicId: ${e.message}")
                }
            }
        }
    }

    fun loadComicById(comicId: String) {
        viewModelScope.launch {
            var comicToLoad = myLibrary.value.find { it.id == comicId }

            if (comicToLoad == null) {
                comicToLoad = catalog.value.find { it.id == comicId }
            }

            if (comicToLoad != null) {
                _currentComic.value = comicToLoad
            } else {
                _errorMessage.value = "Comic not found or unavailable."
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
                Log.e("AppViewModel", "User not found", e)
            }
        }
    }

    fun rateComic(comicId: String, rating: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.rateComic(comicId, rating)
                }
                _userRating.value = rating
            } catch (e: Exception) {
                _errorMessage.value = "Failed to submit rating: ${e.message}"
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
                _errorMessage.value = "Failed to mark as read: ${e.message}"
                Log.e("Chapter error", "Failed to mark as read: ${e.message}")
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
                _errorMessage.value = "Failed to submit report: ${e.message}"
                Log.e("Report error", "Failed to submit report: ${e.message}")
            }
        }
    }
}
fun convertTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return date.format(formatter)
}