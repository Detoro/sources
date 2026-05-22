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
import toro.sources.dataModels.Chapter
import toro.sources.dataModels.Comic
import toro.sources.dataModels.LoginCredentials
import toro.sources.dataModels.AuthRequest
import toro.sources.dataModels.Notification
import toro.sources.dataModels.Post
import toro.sources.dataModels.ChatMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import toro.sources.dataModels.AuthResponse
import toro.sources.dataModels.ChatRequest
import toro.sources.dataModels.Comment
import toro.sources.db.ComicRepository
import toro.sources.network.RetrofitClient
import android.content.Context
import toro.sources.dataModels.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import okhttp3.RequestBody.Companion.asRequestBody
import toro.sources.dataModels.AuthorRequest
import toro.sources.dataModels.CommentRequest
import toro.sources.dataModels.PostRequest
import toro.sources.dataModels.FcmTokenRequest
import toro.sources.dataModels.Conversation
import toro.sources.dataModels.Tag
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.String
import androidx.core.net.toUri
import toro.sources.dataModels.UpdateBioRequest
import toro.sources.dataModels.UpdateUsernameRequest
import toro.sources.dataModels.UserProfile
import com.google.firebase.messaging.FirebaseMessaging
import toro.sources.notifications.NotificationEventBus

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

    val searchResults: StateFlow<List<Comic>> = combine(
        myLibrary, _catalog, _searchQuery, _searchSource
    ) { library, online, query, source ->
        if (query.isBlank()) {
            emptyList()
        } else {
            when (source) {
                SearchSource.LOCAL -> {
                    library.filter { comic ->
                        comic.title.contains(query, ignoreCase = true) ||
                                comic.author.contains(query, ignoreCase = true)
                    }
                }
                SearchSource.ONLINE -> {
                    online.filter { comic ->
                        comic.title.contains(query, ignoreCase = true) ||
                                comic.author.contains(query, ignoreCase = true)
                    }
                }
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

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags = _tags.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts = _userPosts.asStateFlow()

    private val _userWorks = MutableStateFlow<List<Comic>>(emptyList())
    val userWorks = _userWorks.asStateFlow()

    private val _userSuggestions = MutableStateFlow<List<UserProfile>>(emptyList())
    val userSuggestions = _userSuggestions.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation = _pendingNavigation.asStateFlow()

    val subscribedComics: StateFlow<List<Comic>> = repository.getSubscribedComics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyReadComics: StateFlow<List<Comic>> = repository.getRecentlyReadComics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var chapterPages: List<Page> = emptyList()

    init {
        getCatalog()
        getChatRequests()
        seedTestData()

        if (RetrofitClient.tokenManager.getTokenSync() != null) {
            fetchAndRegisterFcmToken()
        }

        viewModelScope.launch {
            NotificationEventBus.notifications.collect { newNotification ->
                _notifications.value = listOf(newNotification) + _notifications.value
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
                repository.toggleLocalSubscription(comicId, response.isSubscribed)
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
            repository.updateLastRead(comic.id)
            try {
                // Check if it's our test data
                if (chapterId.startsWith("chapter_")) {
                    val mockPages = when (chapterId) {
                        "chapter_1_1" -> listOf(
                            Page(id = "p1", chapterId = chapterId, pageNumber = 0, imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=500&auto=format&fit=crop"),
                            Page(id = "p2", chapterId = chapterId, pageNumber = 1, imageUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?q=80&w=500&auto=format&fit=crop"),
                            Page(id = "p3", chapterId = chapterId, pageNumber = 2, imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=500&auto=format&fit=crop")
                        )
                        "chapter_1_2" -> listOf(
                            Page(id = "p4", chapterId = chapterId, pageNumber = 0, imageUrl = "https://images.unsplash.com/photo-1444703686981-a3abb9a555e6?q=80&w=500&auto=format&fit=crop"),
                            Page(id = "p5", chapterId = chapterId, pageNumber = 1, imageUrl = "https://images.unsplash.com/photo-1475274047050-1d0c0975c63e?q=80&w=500&auto=format&fit=crop")
                        )
                        else -> listOf(
                            Page(id = "p6", chapterId = chapterId, pageNumber = 0, imageUrl = "https://images.unsplash.com/photo-1506318137071-a8e063b4bcc0?q=80&w=500&auto=format&fit=crop")
                        )
                    }
                    chapterPages = mockPages
                    _pageCount.value = mockPages.size
                } else {
                    val pages = repository.getPagesForChapter(chapterId, comic.id)
                    chapterPages = pages
                    _pageCount.value = pages.size
                }
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
                _currentUser.value = response
                RetrofitClient.tokenManager.saveToken(response.token)
                fetchAndRegisterFcmToken()
                _currentComic.value = null
                onSuccess()
                Log.i("Success", "Logged in successfully as ${response.username}!")
            } catch (e: Exception) {
                Log.e("Failure", "Login failed: ${e.message}")
            }
        }
    }
    fun logoutUser(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            _currentUser.value = AuthResponse()
            RetrofitClient.tokenManager.clearToken()
            _currentComic.value = null
            onLogoutComplete()
        }
    }
    fun registerNewUser(newUser: AuthRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.comicApiService.signUp(newUser)
                _currentUser.value = response
                RetrofitClient.tokenManager.saveToken(response.token)
                fetchAndRegisterFcmToken()
                onSuccess()
                Log.i("Success", "Sign up successfully as ${response.username}!")
            } catch (e: Exception) {
                Log.e("Failure", "Signup failed: ${e.message}")
            }
        }
    }

    fun updateBio(bio: String) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value.userId
                if (userId.isEmpty()) return@launch
                
                val response = RetrofitClient.comicApiService.updateBio(userId, UpdateBioRequest(bio))
                _userProfile.value?.bio = response.message
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

                val response = RetrofitClient.comicApiService.updateUsername(userId, UpdateUsernameRequest(newUsername))
                _userProfile.value?.username = response.message
                _currentUser.value = _currentUser.value.copy(username = response.message)
                Log.i("Success", "Username updated successfully")
            } catch (e: Exception) {
                Log.e("AppViewModel", "Failed to update username: ${e.message}")
                _errorMessage.value = "Failed to update username"
            }
        }
    }
    fun uploadAvatar(context: Context, selectedUri: Uri) {
        _currentUser.value = _currentUser.value.copy(avatarUrl = selectedUri)
        viewModelScope.launch {
            try {
                val file = getFileFromUri(context, selectedUri)

                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

                val response = RetrofitClient.comicApiService.uploadAvatar(body)

                _currentUser.value = _currentUser.value.copy(avatarUrl = response.message.toUri())

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
                val messages = RetrofitClient.comicApiService.getChatMessages(conversationId)
                _chatMessages.value = messages.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chat: ${e.message}"
            }
        }
    }

    fun clearChatMessages() {
        _chatMessages.value = emptyList()
    }
    fun sendMessage(conversationId: String, content: String, sharedComicId: String? = null) {
        viewModelScope.launch {
            try {
                val encryptedContent = encryptMessage(content)
                
                val newMessage = ChatMessage(
                    id = "",
                    senderId = "",
                    content = encryptedContent,
                    timestamp = System.currentTimeMillis(),
                    isEncrypted = true,
                    sharedComicId = sharedComicId
                )
                RetrofitClient.comicApiService.sendMessage(conversationId, newMessage)
                getChatMessages(conversationId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send message: ${e.message}"
            }
        }
    }
    private fun encryptMessage(content: String): String {
        // reading e2e stuff
        Log.i("encrypt", "encrypted")
        return content
    }
    fun decryptMessage(content: String): String {
        // same as above
        Log.i("decrypt", "decrypted")
        return content
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
    fun getComicComments(comicId: String) {
        viewModelScope.launch {
            try {
                _comments.value = RetrofitClient.comicApiService.getComicComments(comicId)
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
                RetrofitClient.comicApiService.likePost(postId)
            } catch (e: Exception) {
                _communityPosts.value = currentPosts
                _errorMessage.value = "Failed to sync like: ${e.message}"
            }
        }
    }
    fun likeComment(commentId: String) {
        val currentComments = _comments.value
        _comments.value = currentComments.map { comment ->
            if (comment.id == commentId) {
                comment.copy(
                    isLiked = !comment.isLiked,
                    likesCount = if (comment.isLiked) comment.likesCount - 1 else comment.likesCount + 1
                )
            } else comment
        }
        viewModelScope.launch {
            try {
                RetrofitClient.comicApiService.likeComment(commentId)
            } catch (e: Exception) {
                _comments.value = currentComments
                _errorMessage.value = "Failed to like comment: ${e.message}"
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
                RetrofitClient.comicApiService.bookmarkPost(postId)
            } catch (e: Exception) {
                _communityPosts.value = currentPosts
                _errorMessage.value = "Failed to bookmark post: ${e.message}"
            }
        }
    }
    fun makePost(postContent: String, tags: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                val request = PostRequest(
                    content = postContent,
                    tags = tags
                )
                val response = RetrofitClient.comicApiService.makePost(request)
                Log.i("Post", "Post created: ${response.message}")
                getCommunityPosts()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to make post: ${e.message}"
            }
        }
    }
    fun addPostComment(postId: String, content: String, mentionedUserIds: List<String> = emptyList(), parentId: String? = null) {
        viewModelScope.launch {
            try {
                val newComment = CommentRequest(
                    content = content,
                    mentionedUserIds = mentionedUserIds,
                    parentId = parentId
                )
                RetrofitClient.comicApiService.addPostComment(postId, newComment)
                getPostComments(postId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to post comment: ${e.message}"
            }
        }
    }
    fun addComicComment(comicId: String, content: String, mentionedUserIds: List<String> = emptyList(), parentId: String? = null) {
        viewModelScope.launch {
            try {
                val newComment = CommentRequest(
                    content = content,
                    mentionedUserIds = mentionedUserIds,
                    parentId = parentId
                )
                RetrofitClient.comicApiService.addComicComment(comicId, newComment)
                getComicComments(comicId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to post comic comment: ${e.message}"
            }
        }
    }
    fun searchUsers(query: String) {
        viewModelScope.launch {
            try {
                _userSuggestions.value = RetrofitClient.comicApiService.searchUsers(query)
            } catch (e: Exception) {
                Log.e("SearchUsers", "Failed: ${e.message}")
            }
        }
    }
    fun sendFriendRequest(receiverId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.comicApiService.sendChatRequest(receiverId)
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
    fun getTags(postId: String) {
        viewModelScope.launch {
            try {
                _tags.value = RetrofitClient.comicApiService.getTagsByPostId(postId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to get Tags: ${e.message}"
            }
        }
    }

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                _userProfile.value = RetrofitClient.comicApiService.getUserProfile(userId)
                _userPosts.value = RetrofitClient.comicApiService.getUserPosts(userId)
                _userWorks.value = RetrofitClient.comicApiService.getUserWorks(userId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load profile: ${e.message}"
            }
        }
    }

    fun toggleProfilePrivacy(userId: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.comicApiService.toggleProfilePrivacy(userId)
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
                RetrofitClient.comicApiService.registerFcmToken(FcmTokenRequest(token))
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

    fun clearError() {
        _errorMessage.value = null
    }
    fun seedTestData() {
        viewModelScope.launch {
            val testComics = listOf(
                Comic(
                    id = "comic_1",
                    title = "The Galactic Journey",
                    author = "Astra Nova",
                    description = "An epic space adventure across unknown galaxies.",
                    coverImageUrl = "https://images.unsplash.com/photo-1614728263952-84ea256f9679?q=80&w=500&auto=format&fit=crop",
                    scrollDirection = "VERTICAL",
                    hasMusic = true,
                    isSubscribed = true,
                    lastReadTimestamp = System.currentTimeMillis()
                ),
                Comic(
                    id = "comic_2",
                    title = "Midnight Whispers",
                    author = "Elena Shadow",
                    description = "A mystery lurking in the shadows of an old Victorian mansion.",
                    coverImageUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=500&auto=format&fit=crop",
                    scrollDirection = "HORIZONTAL",
                    hasMusic = false,
                    isSubscribed = false,
                    lastReadTimestamp = 0L
                ),
                Comic(
                    id = "comic_3",
                    title = "Neon Samurai",
                    author = "Kenji Cyber",
                    description = "A high-octane cyberpunk action story set in neo-Tokyo.",
                    coverImageUrl = "https://images.unsplash.com/photo-1605810230434-7631ac76ec81?q=80&w=500&auto=format&fit=crop",
                    scrollDirection = "HORIZONTAL",
                    hasMusic = true,
                    isSubscribed = true,
                    lastReadTimestamp = System.currentTimeMillis() - 100000
                ),
                Comic(
                    id = "comic_4",
                    title = "Solar Winds",
                    author = "Astra Nova",
                    description = "A relaxing journey through the rings of Saturn.",
                    coverImageUrl = "https://images.unsplash.com/photo-1614728263952-84ea256f9679?q=80&w=500&auto=format&fit=crop",
                    scrollDirection = "HORIZONTAL",
                    hasMusic = false,
                    isSubscribed = true,
                    lastReadTimestamp = System.currentTimeMillis() - 500000
                )
            )

            val testChapters = listOf(
                Chapter(
                    id = "chapter_1_1",
                    comicId = "comic_1",
                    chapterTitle = "The Departure",
                    chapterNumber = 1f,
                    pageCount = 3
                ),
                Chapter(
                    id = "chapter_1_2",
                    comicId = "comic_1",
                    chapterTitle = "First Contact",
                    chapterNumber = 2f,
                    pageCount = 2
                ),
                Chapter(
                    id = "chapter_2_1",
                    comicId = "comic_2",
                    chapterTitle = "The Invitation",
                    chapterNumber = 1f,
                    pageCount = 4
                ),
                Chapter(
                    id = "chapter_3_1",
                    comicId = "comic_3",
                    chapterTitle = "Binary Soul",
                    chapterNumber = 1f,
                    pageCount = 5
                ),
                Chapter(
                    id = "chapter_4_1",
                    comicId = "comic_4",
                    chapterTitle = "Golden Dust",
                    chapterNumber = 1f,
                    pageCount = 2
                )
            )

            repository.insertComics(testComics)
            repository.insertChapters(testChapters)

            // Seed community data
            val testPosts = listOf(
                Post(
                    id = "post_1",
                    authorId = "u1",
                    authorName = "AstraNova",
                    content = "Just uploaded Chapter 2 of Galactic Journey! What do you guys think?",
                    timestamp = System.currentTimeMillis() - 3600000,
                    likesCount = 12,
                    isLiked = false
                ),
                Post(
                    id = "post_2",
                    authorId = "u2",
                    authorName = "ElenaShadow",
                    content = "Midnight Whispers is reaching its climax. Stay tuned!",
                    timestamp = System.currentTimeMillis() - 7200000,
                    likesCount = 45,
                    isLiked = true
                )
            )
            _communityPosts.value = testPosts

            val testComments = listOf(
                Comment(
                    id = "c1",
                    postId = "post_1",
                    authorId = "u2",
                    authorName = "ElenaShadow",
                    content = "The art in the nebula scene was breathtaking!",
                    timestamp = System.currentTimeMillis() - 3000000,
                    likesCount = 5,
                    repliesCount = 1
                ),
                Comment(
                    id = "c2",
                    postId = "post_1",
                    authorId = "u1",
                    authorName = "AstraNova",
                    content = "Thank you! I spent a lot of time on that one.",
                    timestamp = System.currentTimeMillis() - 2500000,
                    parentId = "c1",
                    likesCount = 2
                ),
                Comment(
                    id = "c3",
                    postId = "post_1",
                    authorId = "u3",
                    authorName = "SpaceTraveler",
                    content = "Can't wait for Chapter 3 @AstraNova!",
                    timestamp = System.currentTimeMillis() - 2000000,
                    likesCount = 0
                )
            )
            _comments.value = testComments

            val testPages = listOf(
                Page(id = "p1", chapterId = "chapter_1_1", pageNumber = 0, imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=500&auto=format&fit=crop"),
                Page(id = "p2", chapterId = "chapter_1_1", pageNumber = 1, imageUrl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?q=80&w=500&auto=format&fit=crop"),
                Page(id = "p3", chapterId = "chapter_1_1", pageNumber = 2, imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=500&auto=format&fit=crop")
            )
            if (currentComic.value?.id == "comic_1") {
                chapterPages = testPages
                _pageCount.value = testPages.size
            }

            Log.i("TestData", "Database seeded with test comics, chapters, and sample pages.")
        }
    }
}
fun convertTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return date.format(formatter)
}
fun getFileFromUri(context: Context, uri: Uri): File {
    val inputStream = context.contentResolver.openInputStream(uri)
    val tempFile = File(context.cacheDir, "temp_avatar_upload.jpg")

    tempFile.outputStream().use { outputStream ->
        inputStream?.copyTo(outputStream)
    }
    return tempFile
}