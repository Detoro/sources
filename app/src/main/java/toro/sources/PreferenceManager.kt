package toro.sources

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import toro.sources.models.AppTheme

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class PreferenceManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val THEME_SELECTION_KEY = stringPreferencesKey("theme_selection")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")
        private val BIO_KEY = stringPreferencesKey("user_bio")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun saveUserData(userId: String, username: String, avatarUrl: String?, bio: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USERNAME_KEY] = username
            avatarUrl?.let { preferences[AVATAR_URL_KEY] = it }
            bio?.let { preferences[BIO_KEY] = it }
        }
    }

    fun getUserDataSync(): UserData {
        return runBlocking {
            val prefs = context.dataStore.data.first()
            UserData(
                userId = prefs[USER_ID_KEY],
                username = prefs[USERNAME_KEY],
                avatarUrl = prefs[AVATAR_URL_KEY],
                bio = prefs[BIO_KEY]
            )
        }
    }

    data class UserData(
        val userId: String?,
        val username: String?,
        val avatarUrl: String?,
        val bio: String?
    )

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(AVATAR_URL_KEY)
            preferences.remove(BIO_KEY)
        }
    }

    fun getAccessTokenSync(): String? {
        return runBlocking {
            context.dataStore.data.first()[ACCESS_TOKEN_KEY]
        }
    }

    fun getRefreshTokenSync(): String? {
        return runBlocking {
            context.dataStore.data.first()[REFRESH_TOKEN_KEY]
        }
    }

    val theme: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_SELECTION_KEY] ?: ""
        }

    suspend fun setTheme(selection: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_SELECTION_KEY] = selection.name
        }
    }
}