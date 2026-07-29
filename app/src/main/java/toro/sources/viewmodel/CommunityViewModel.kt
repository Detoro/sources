package toro.sources.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toro.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toro.sources.db.ComicRepository
import toro.sources.media.MediaUploadManager
import toro.sources.network.RetrofitClient
import toro.sources.sharing.ShareCoordinator
import toro.sources.viewmodel.common.UserSearchDelegate
import toro.sources.viewmodel.common.optimisticToggle
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val shareCoordinator: ShareCoordinator,
    private val repository: ComicRepository,
    private val mediaUploadManager: MediaUploadManager
) : ViewModel() {

    private val _communityState = MutableStateFlow(CommunityUiState())
    val communityState = _communityState.asStateFlow()

    private val _postComments = MutableStateFlow<List<Comment>>(emptyList())
    val postComments = _postComments.asStateFlow()

    private val _chapterComments = MutableStateFlow<List<Comment>>(emptyList())
    val chapterComments = _chapterComments.asStateFlow()

    private val userSearch = UserSearchDelegate(viewModelScope)
    val userSuggestions = userSearch.userSuggestions

    fun getCommunityPosts() {
        viewModelScope.launch {
            _communityState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val posts = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getCommunityPosts()
                }
                _communityState.update { it.copy(posts = posts, isLoading = false) }
                posts.forEach { repository.savePost(it) }
            } catch (e: Exception) {
                _communityState.update { it.copy(isLoading = false, errorMessage = "Failed to fetch posts: ${e.message}") }
            }
        }
    }

    fun makePost(
        title: String?,
        isSpoiler: Boolean,
        content: String,
        tags: List<String> = emptyList(),
        attachment: Uri? = null
    ) {
        viewModelScope.launch {
            try {
                var mediaUrl: String? = null
                var mediaType: String? = null
                if (attachment != null) {
                    mediaUrl = mediaUploadManager.uploadFileToCloudinary(attachment)
                    mediaType = if (mediaUrl.endsWith(".mp4") || mediaUrl.endsWith(".mov")) "VIDEO" else "IMAGE"
                }

                val shared = shareCoordinator.sharedContent.value
                val request = PostRequest(
                    title = title,
                    isSpoiler = isSpoiler,
                    content = content,
                    tags = tags,
                    sharedId = shared?.id,
                    sharedType = shared?.type,
                    sharedTitle = shared?.title,
                    sharedPreview = shared?.previewText,
                    imageUrls = if (mediaType == "IMAGE") listOfNotNull(mediaUrl) else emptyList(),
                    videoUrls = if (mediaType == "VIDEO") listOfNotNull(mediaUrl) else emptyList()
                )

                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.makePost(request)
                }
                getCommunityPosts()
                shareCoordinator.setSharedContent(null)
            } catch (e: Exception) {
                _communityState.update { it.copy(errorMessage = "Failed to make post: ${e.message}") }
            }
        }
    }

    fun getPostComments(postId: String) {
        viewModelScope.launch {
            try {
                val comments = withContext(Dispatchers.IO) { RetrofitClient.comicApiService.getPostComments(postId) }
                _postComments.value = comments
                comments.forEach { repository.saveComment(it) }
            } catch (e: Exception) {
                _communityState.update { it.copy(errorMessage = "Failed to fetch comments: ${e.message}") }
            }
        }
    }

    fun getChapterComments(chapterId: String) {
        viewModelScope.launch {
            try {
                val comments = withContext(Dispatchers.IO) { RetrofitClient.comicApiService.getChapterComments(chapterId) }
                _chapterComments.value = comments
                comments.forEach { repository.saveComment(it) }
            } catch (e: Exception) {
                _communityState.update { it.copy(errorMessage = "Failed to fetch comments: ${e.message}") }
            }
        }
    }

    fun addPostComment(postId: String, content: String, isSpoiler: Boolean, muids: List<String> = emptyList(), paid: String? = null) {
        viewModelScope.launch {
            try {
                val shared = shareCoordinator.sharedContent.value
                val request = CommentRequest(content, isSpoiler, muids, paid, shared?.id, shared?.type, shared?.title, shared?.previewText)
                RetrofitClient.comicApiService.addPostComment(postId, request)
                getPostComments(postId)
            } catch (e: Exception) {
                _communityState.update { it.copy(errorMessage = "Failed to add comment: ${e.message}") }
            }
        }
    }

    fun addChapterComment(chapterId: String, content: String, isSpoiler: Boolean, muids: List<String> = emptyList(), paid: String? = null) {
        viewModelScope.launch {
            try {
                val shared = shareCoordinator.sharedContent.value
                val request = CommentRequest(content, isSpoiler, muids, paid, shared?.id, shared?.type, shared?.title, shared?.previewText)
                RetrofitClient.comicApiService.addChapterComment(chapterId, request)
                getChapterComments(chapterId)
            } catch (e: Exception) {
                _communityState.update { it.copy(errorMessage = "Failed to add comment: ${e.message}") }
            }
        }
    }

    fun likePost(postId: String) {
        optimisticToggle(
            scope = viewModelScope,
            applyOptimistically = {
                val previous = _communityState.value.posts
                _communityState.update { state ->
                    state.copy(posts = previous.map {
                        if (it.id == postId) it.copy(isLiked = !it.isLiked, likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1) else it
                    })
                }
                previous
            },
            networkCall = { RetrofitClient.comicApiService.likePost(postId) },
            rollback = { previousPosts -> _communityState.update { it.copy(posts = previousPosts) } }
        )
    }

    fun likeComment(commentId: String, location: CommentLocation) {
        val stateFlow = if (location == CommentLocation.ON_CHAPTER) _chapterComments else _postComments
        optimisticToggle(
            scope = viewModelScope,
            applyOptimistically = {
                val previous = stateFlow.value
                stateFlow.update { list ->
                    list.map { if (it.id == commentId) it.copy(isLiked = !it.isLiked, likesCount = if (it.isLiked) it.likesCount - 1 else it.likesCount + 1) else it }
                }
                previous
            },
            networkCall = {
                if (location == CommentLocation.ON_CHAPTER) RetrofitClient.comicApiService.likeChapterComment(commentId)
                else RetrofitClient.comicApiService.likePostComment(commentId)
            },
            rollback = { previousComments -> stateFlow.value = previousComments }
        )
    }

    fun bookmarkPost(postId: String) {
        optimisticToggle(
            scope = viewModelScope,
            applyOptimistically = {
                val previous = _communityState.value.posts
                _communityState.update { state ->
                    state.copy(posts = previous.map { if (it.id == postId) it.copy(isBookmarked = !it.isBookmarked) else it })
                }
                previous
            },
            networkCall = { RetrofitClient.comicApiService.bookmarkPost(postId) },
            rollback = { previousPosts -> _communityState.update { it.copy(posts = previousPosts) } }
        )
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deletePostById(postId)
                    RetrofitClient.comicApiService.deletePost(postId)
                }
                getCommunityPosts()
            } catch (e: Exception) {
                _communityState.update { it.copy(errorMessage = "Failed to delete post: ${e.message}") }
            }
        }
    }

    fun deleteComment(location: CommentLocation, typeId: String, commentId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (location == CommentLocation.ON_CHAPTER) RetrofitClient.comicApiService.deleteChapterComment(typeId, commentId)
                    else RetrofitClient.comicApiService.deletePostComment(typeId, commentId)
                    repository.deleteCommentById(commentId)
                }
                if (location == CommentLocation.ON_CHAPTER) getChapterComments(typeId) else getPostComments(typeId)
            } catch (e: Exception) {
                _communityState.update { it.copy(errorMessage = "Failed to delete comment: ${e.message}") }
            }
        }
    }

    fun searchUsers(query: String) = userSearch.search(query)

    fun clearUserSuggestions() = userSearch.clear()

    fun submitReport(targetType: String, targetId: String?, reason: String, details: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val request = ReportRequest(targetType, targetId, reason, details)
                withContext(Dispatchers.IO) { RetrofitClient.comicApiService.submitReport(request) }
                onSuccess()
            } catch (e: Exception) {
                _communityState.update { it.copy(errorMessage = "Report failed: ${e.message}") }
            }
        }
    }
}

data class CommunityUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)