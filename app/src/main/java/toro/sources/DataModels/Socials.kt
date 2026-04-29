package toro.sources.DataModels

import kotlinx.serialization.Serializable

@Serializable
enum class ChatStatus { PENDING, ACCEPTED, REJECTED }

@Serializable
data class User(
    val id: String,
    val username: String,
    val profileImageUrl: String? = null
)

@Serializable
data class ChatRequest(
    val id: String,
    val senderId: String,
    val senderName: String,
    val status: ChatStatus = ChatStatus.PENDING
)

@Serializable
data class ChatMessage(
    val id: String,
    val senderId: String,
    val content: String,
    val timestamp: Long
)