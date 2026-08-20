package toro.sources.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toro.models.SharedContent
import com.toro.models.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toro.sources.PreferenceManager
import toro.sources.db.ComicRepository
import toro.sources.navigation.DeepLinkRouter
import toro.sources.navigation.NavigationState
import toro.sources.session.SessionManager
import toro.sources.sharing.ShareCoordinator
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    application: Application,
    private val preferenceManager: PreferenceManager,
    sessionManager: SessionManager,
    private val navigationState: NavigationState,
    private val deepLinkRouter: DeepLinkRouter,
    private val shareCoordinator: ShareCoordinator,
    private val repository: ComicRepository
) : AndroidViewModel(application) {

    val userProfile: StateFlow<UserProfile?> = sessionManager.userProfile
    val sharedContent: StateFlow<SharedContent?> = shareCoordinator.sharedContent
    val pendingNavigation: StateFlow<String?> = navigationState.pendingNavigation

    val isDarkTheme: StateFlow<Boolean> = preferenceManager.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _showShareDialog = MutableStateFlow(false)
    val showShareDialog = _showShareDialog.asStateFlow()

    fun setSharedContent(content: SharedContent?) {
        shareCoordinator.setSharedContent(content)
    }

    fun showShareDialog(show: Boolean) {
        _showShareDialog.value = show
    }

    fun handleNavigation(route: String?) {
        navigationState.handleNavigation(route)
    }

    fun onNavigationHandled() {
        navigationState.onNavigationHandled()
    }

    fun handleIntent(intent: Intent?) {
        deepLinkRouter.handleIntent(intent)
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                preferenceManager.setDarkTheme(enabled)
            }
        }
    }

    fun deleteUserLocalData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearAllData()
            }
        }
    }
}