package toro.sources.DataModels

enum class UserRole { CREATOR, READER, ADMIN }

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val role: UserRole = UserRole.READER
)

data class LoginCredentials(
    val email: String,
    val password: String
)