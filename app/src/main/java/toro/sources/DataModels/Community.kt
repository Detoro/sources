package toro.sources.DataModels

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val timestamp: Long,
    val likesCount: Int = 0
)

@Serializable
data class Comment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val timestamp: Long
)