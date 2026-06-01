package toro.sources

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class PreferenceManager(private val context: Context) {

    companion object {
        private val JWT_KEY = stringPreferencesKey("jwt_token")
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val AVATAR_URL_KEY = stringPreferencesKey("avatar_url")
        private val BIO_KEY = stringPreferencesKey("user_bio")
    }

    // Auth Token
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[JWT_KEY] = token
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

    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(JWT_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USERNAME_KEY)
            preferences.remove(AVATAR_URL_KEY)
            preferences.remove(BIO_KEY)
        }
    }

    fun getTokenSync(): String? {
        return runBlocking {
            context.dataStore.data.first()[JWT_KEY]
        }
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_THEME_KEY] ?: true
        }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = enabled
        }
    }
}