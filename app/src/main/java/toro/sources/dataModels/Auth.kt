package toro.sources.dataModels

import android.net.Uri
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val username: String? = null,
    @Contextual
    var avatarUrl: Uri? = null
)

@Serializable
data class AuthResponse(
    val token: String = "",
    val userId: String = "",
    val username: String = "",
    @Contextual
    var avatarUrl: Uri? = null
) {
}

@Serializable
data class UserProfile(
    val id: String,
    var username: String,
    val avatarUrl: String? = null,
    val isAuthor: Boolean = false,
    var bio: String? = null,
    val worksCount: Int = 0,
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val friendsCount: Int = 0,
    val isPrivate: Boolean = false,
    val isFollowing: Boolean = false
)

@Serializable
data class UpdateBioRequest(val bio: String)

@Serializable
data class UpdateUsernameRequest(val username: String)
