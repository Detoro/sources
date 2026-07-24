package toro.sources.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationState @Inject constructor() {
    private val _pendingNavigation = MutableStateFlow<String?>(null)
    val pendingNavigation = _pendingNavigation.asStateFlow()

    fun handleNavigation(route: String?) {
        _pendingNavigation.value = route
    }

    fun onNavigationHandled() {
        _pendingNavigation.value = null
    }
}