package toro.sources.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toro.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import toro.sources.network.RetrofitClient
import toro.sources.session.SessionManager
import toro.sources.network.ChatConnectionManager
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val chatConnectionManager: ChatConnectionManager
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState = _authState.asStateFlow()

    fun loginUser(credentials: LoginCredentials, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val authRequest = AuthRequest(email = credentials.email, password = credentials.password)
                val res = RetrofitClient.comicApiService.login(authRequest)
                sessionManager.saveSession(res.accessToken, res.refreshToken)

                val profile = RetrofitClient.comicApiService.getUserProfile(res.userId)
                sessionManager.updateUserProfile(profile)

                _authState.update { it.copy(isLoading = false, isAuthenticated = true) }
                onSuccess()
            } catch (e: Exception) {
                _authState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Login failed") }
            }
        }
    }

    fun registerNewUser(newUser: AuthRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val res = RetrofitClient.comicApiService.signUp(newUser)
                sessionManager.saveSession(res.accessToken, res.refreshToken)

                val profile = RetrofitClient.comicApiService.getUserProfile(res.userId)
                sessionManager.updateUserProfile(profile)

                _authState.update { it.copy(isLoading = false, isAuthenticated = true) }
                onSuccess()
            } catch (e: Exception) {
                _authState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Signup failed") }
            }
        }
    }

    fun logoutUser(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val refreshToken = RetrofitClient.preferenceManager.getRefreshTokenSync()
                if (refreshToken != null) {
                    RetrofitClient.comicApiService.logout(RefreshTokenRequest(refreshToken))
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Logout failed", e)
            } finally {
                sessionManager.clearSession()
                sessionManager.updateUserProfile(null)
                chatConnectionManager.disconnect()
                _authState.update { AuthUiState() }
                onLogoutComplete()
            }
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.comicApiService.deleteAccount()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Delete account failed", e)
            }
            logoutUser(onComplete)
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
)