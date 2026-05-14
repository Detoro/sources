package toro.sources.dataModels

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
data class Tag(
    val id: String,
    val postId: String,
    val content: String
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
data class PostRequest(
    val content: String,
    val tags: List<String> = emptyList()
)

@Serializable
data class Bookmark(
    val id: String = "",
    val userId: String,
    val postId: String,
    val timestamp: Long = System.currentTimeMillis()
)