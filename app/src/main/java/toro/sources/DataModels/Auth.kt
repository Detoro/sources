package toro.sources.DataModels

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val username: String? = null
)

@Serializable
data class AuthResponse(
    val token: String = "",
    val userId: String = "",
    val username: String = ""
)

@Serializable
object TokenManager {
    var jwtToken: String? = null
}

@Serializable
data class UserProfile(
    val id: String,
    val username: String
)