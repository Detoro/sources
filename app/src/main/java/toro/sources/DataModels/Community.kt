package toro.sources.DataModels

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val timestamp: Long,
    val likesCount: Int = 0,
    var isLiked: Boolean = false,
    var isBookmarked: Boolean = false
)

@Serializable
data class Comment(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val timestamp: Long
)

@Serializable
data class CommentRequest(
    val content: String
)

@Serializable
data class Bookmark(
    val id: String = "",
    val userId: String,
    val postId: String,
    val timestamp: Long = System.currentTimeMillis()
)