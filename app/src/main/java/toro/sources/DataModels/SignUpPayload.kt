package toro.sources.DataModels

data class SignUpPayload(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole
)