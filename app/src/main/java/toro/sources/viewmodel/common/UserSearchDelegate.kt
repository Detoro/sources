package toro.sources.viewmodel.common

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toro.sources.models.UserProfile
import toro.sources.network.RetrofitClient

class UserSearchDelegate(private val scope: CoroutineScope) {

    private val _userSuggestions = MutableStateFlow<List<UserProfile>>(emptyList())
    val userSuggestions = _userSuggestions.asStateFlow()

    fun search(query: String) {
        scope.launch {
            try {
                _userSuggestions.value = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.searchUsers(query)
                }
            } catch (e: Exception) {
                Log.e("UserSearchDelegate", "Error searching users: ${e.message}")
            }
        }
    }

    fun clear() {
        _userSuggestions.value = emptyList()
    }
}