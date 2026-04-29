package toro.sources.DataModels

enum class UserRole { CREATOR, READER, ADMIN }

data class LoginCredentials(
    val email: String,
    val password: String
)