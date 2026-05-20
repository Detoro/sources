package toro.sources.dataModels

import kotlinx.serialization.Serializable

@Serializable
enum class ChatStatus { PENDING, ACCEPTED, REJECTED }

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
    val timestamp: Long,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
    val isEncrypted: Boolean = false,
    val sharedComicId: String? = null
)

@Serializable
data class Conversation(
    val conversationId: String,
    val otherUserId: String,
    val otherUserName: String,
    val lastMessage: String?,
    val timestamp: Long
)