package toro.sources.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.toro.models.Comic
import com.toro.models.Post
import com.toro.models.UpdateBioRequest
import com.toro.models.UpdateInterestsRequest
import com.toro.models.UpdateUsernameRequest
import com.toro.models.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toro.sources.BuildConfig
import toro.sources.db.ComicRepository
import toro.sources.network.RetrofitClient
import toro.sources.session.SessionManager
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    application: Application,
    private val sessionManager: SessionManager,
    private val repository: ComicRepository
) : AndroidViewModel(application) {

    private val _targetUserProfile = MutableStateFlow<UserProfile?>(null)
    val targetUserProfile = _targetUserProfile.asStateFlow()

    private val _targetUserPosts = MutableStateFlow<List<Post>>(emptyList())
    val targetUserPosts = _targetUserPosts.asStateFlow()

    private val _userWorks = MutableStateFlow<List<Comic>>(emptyList())
    val userWorks = _userWorks.asStateFlow()

    private val _targetUserWorks = MutableStateFlow<List<Comic>>(emptyList())
    val targetUserWorks = _targetUserWorks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun getUserProfile(userId: String) {
        viewModelScope.launch {
            fetchAndApplyUserProfile(userId)
        }
    }

    private suspend fun fetchAndApplyUserProfile(userId: String) {
        val currentUserId = sessionManager.userProfile.value?.id ?: ""
        val isMe = userId.trim().equals(currentUserId.trim(), ignoreCase = true)

        _isLoading.value = true
        try {
            if (userId.isBlank()) {
                _errorMessage.value = "Empty User ID"
                return
            }
            withContext(Dispatchers.IO) {
                val profileAsync = async { RetrofitClient.comicApiService.getUserProfile(userId) }
                val postsAsync = async { RetrofitClient.comicApiService.getUserPosts(userId) }
                val worksAsync = async { RetrofitClient.comicApiService.getUserWorks(userId) }

                val profile = profileAsync.await()
                val posts = postsAsync.await()
                val works = worksAsync.await()

                if (isMe) {
                    sessionManager.updateUserProfile(profile)
                    posts.forEach { repository.savePost(it) }
                    _userWorks.value = works
                } else {
                    _targetUserProfile.value = profile
                    _targetUserPosts.value = posts
                    _targetUserWorks.value = works
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message
            Log.e("ProfileViewModel", "Failed to fetch profile: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun updateBio(bio: String) {
        viewModelScope.launch {
            try {
                val userId = sessionManager.userProfile.value?.id ?: return@launch
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.updateBio(userId, UpdateBioRequest(bio))
                }
                val currentProfile = sessionManager.userProfile.value
                sessionManager.updateUserProfile(currentProfile?.copy(bio = response.message))
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update bio"
            }
        }
    }

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            try {
                val userId = sessionManager.userProfile.value?.id ?: return@launch
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.updateUsername(
                        userId,
                        UpdateUsernameRequest(newUsername)
                    )
                }
                val currentProfile = sessionManager.userProfile.value
                sessionManager.updateUserProfile(currentProfile?.copy(username = response.message))
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update username"
            }
        }
    }

    fun uploadAvatar(selectedUri: Uri) {
        viewModelScope.launch {
            try {
                MediaManager.get().upload(selectedUri)
                    .unsigned(BuildConfig.CLOUDINARY_PRESET)
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val publicUrl = resultData["secure_url"] as String
                            viewModelScope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        RetrofitClient.comicApiService.updateAvatar(publicUrl)
                                    }
                                    val currentProfile = sessionManager.userProfile.value
                                    sessionManager.updateUserProfile(currentProfile?.copy(avatarUrl = publicUrl))
                                } catch (e: Exception) {
                                    _errorMessage.value = "Failed to sync avatar with server"
                                }
                            }
                        }
                        override fun onError(requestId: String, error: ErrorInfo) {
                            _errorMessage.value = error.description
                        }
                        override fun onReschedule(requestId: String, error: ErrorInfo) {}
                    }).dispatch()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upload avatar"
            }
        }
    }

    fun getUserWorks(userId: String) {
        viewModelScope.launch {
            try {
                if (userId.isEmpty()) return@launch
                val works = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getUserWorks(userId)
                }
                val currentUserId = sessionManager.userProfile.value?.id ?: ""
                if (userId == currentUserId) {
                    _userWorks.value = works
                } else {
                    _targetUserWorks.value = works
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to get works: ${e.message}")
            }
        }
    }

    fun toggleProfilePrivacy(userId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.toggleProfilePrivacy(userId)
                }
                val currentProfile = sessionManager.userProfile.value
                sessionManager.updateUserProfile(currentProfile?.copy(isPrivate = !currentProfile.isPrivate))
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle privacy"
            }
        }
    }

    fun updateInterests(interests: List<String>) {
        viewModelScope.launch {
            try {
                val userId = sessionManager.userProfile.value?.id ?: return@launch
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.updateInterests(
                        userId,
                        UpdateInterestsRequest(interests)
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update interests: ${e.message}")
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
                _errorMessage.value = e.message
                Log.e("ProfileViewModel", "User not found", e)
            }
        }
    }

    fun clearProfileError() { _errorMessage.value = null }
}