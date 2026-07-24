package toro.sources.session

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.toro.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import toro.sources.PreferenceManager
import toro.sources.db.CanvasDatabase

class SessionManager(
    private val preferenceManager: PreferenceManager,
    private val context: Context
) {
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    init {
        val userData = preferenceManager.getUserDataSync()
        if (userData.userId != null && preferenceManager.getAccessTokenSync() != null) {
            _userProfile.value = UserProfile(
                id = userData.userId,
                username = userData.username ?: "User",
                avatarUrl = userData.avatarUrl,
                bio = userData.bio
            )
        }
    }

    suspend fun updateUserProfile(profile: UserProfile?) = withContext(Dispatchers.IO) {
        if (profile == null) clearSession(null)
        _userProfile.value = profile
        preferenceManager.saveUserData(
            profile?.id ?: "",
            profile?.username ?: "",
            profile?.avatarUrl,
            profile?.bio
        )
    }

    suspend fun saveSession(accessToken: String, refreshToken: String) =
        withContext(Dispatchers.IO) {
            preferenceManager.saveTokens(accessToken, refreshToken)
            CanvasDatabase.resetDatabase()
        }

    suspend fun clearSession(userId: String?) = withContext(Dispatchers.IO) {
        preferenceManager.clearTokens()
        FirebaseMessaging.getInstance().unregister()
        CanvasDatabase.deleteDatabase(context, userId)
        CanvasDatabase.resetDatabase()
    }

    fun registerFcmToken() {
        FirebaseMessaging.getInstance().register()
    }
}