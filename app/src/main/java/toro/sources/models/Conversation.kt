package toro.sources.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import models.ChatUser
import models.MessageSummary

@Entity(tableName = "conversations")
@Serializable
data class Conversation(
    @PrimaryKey val conversationId: String = "",
    @Embedded val otherUser: ChatUser = ChatUser(),
    @Embedded(prefix = "last_") val lastMessage: MessageSummary? = null,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val timestamp: Long = 0L,
    val backgroundImageUri: String? = null
)

@Entity(tableName = "conversation_ui_state")
@Serializable
data class ConversationUiState(
    @PrimaryKey val conversationId: String,
    val draftText: String = "",
    val replyToMessageId: String? = null
)