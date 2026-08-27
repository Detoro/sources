package toro.sources.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import toro.sources.models.Notification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toro.sources.db.ComicRepository
import toro.sources.network.RetrofitClient
import toro.sources.session.SessionManager
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    sessionManager: SessionManager,
    private val repository: ComicRepository
) : ViewModel() {

    private val _notificationsUiState = MutableStateFlow(NotificationsUiState())
    val notificationsUiState = _notificationsUiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: StateFlow<List<Notification>> = sessionManager.userProfile
        .flatMapLatest { user ->
            if (user != null) repository.getNotifications() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.markNotificationAsRead(notificationId)
                }
            } catch (e: Exception) {
                _notificationsUiState.update { it.copy(errorMessage = "Failed to mark notification as read ${e.message}") }
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

    fun clearNotificationsError() { _notificationsUiState.update { it.copy(errorMessage = null) } }
}

data class NotificationsUiState(
    val errorMessage: String? = null
)