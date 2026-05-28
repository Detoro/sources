package toro.sources.dataModels

import kotlinx.serialization.Serializable

enum class CommentLocation { ON_POST, ON_CHAPTER }
@Serializable
enum class ShareType { COMIC, POST, COMMENT }

@Serializable
data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String? = null,
    val content: String = "",
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    var isLiked: Boolean = false,
    var isBookmarked: Boolean = false,
    val sharedId: String? = null,
    val sharedType: ShareType? = null
)

@Serializable
data class Comment(
    val id: String = "",
    val commentLocationId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val parentId: String? = null,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val repliesCount: Int = 0,
    val sharedId: String? = null,
    val sharedType: ShareType? = null
)

@Serializable
data class CommentRequest(
    val content: String,
    val mentionedUserIds: List<String> = emptyList(),
    val parentId: String? = null,
    val sharedId: String? = null,
    val sharedType: ShareType? = null
)

@Serializable
data class PostRequest(
    val title: String? = null,
    val content: String,
    val tags: List<String> = emptyList(),
    val sharedId: String? = null,
    val sharedType: ShareType? = null
)

@Serializable
data class FcmTokenRequest(
    val token: String
)