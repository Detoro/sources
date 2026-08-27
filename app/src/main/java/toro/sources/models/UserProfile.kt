package toro.sources.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "authors")
@Serializable
data class UserProfile(
    @PrimaryKey val id: String,
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