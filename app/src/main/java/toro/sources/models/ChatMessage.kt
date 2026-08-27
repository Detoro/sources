package toro.sources.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import models.MessageContent
import models.ShareType
import models.SharedAttachment

@Entity(tableName = "chat_messages")
@Serializable
data class ChatMessage(
    @PrimaryKey val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
    val isSpoiler: Boolean = false,
    val replyToMessageId: String? = null,
    val sharedComicId: String? = null,
    val sharedId: String? = null,
    val sharedType: ShareType? = null,
    val sharedTitle: String? = null,
    val sharedPreview: String? = null,
    val sharedImageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val videoUrls: List<String> = emptyList(),
    val mediaType: String? = null // "IMAGE", "VIDEO", "GIF", "SYSTEM_TYPING"
)

fun ChatMessage.toContent(): MessageContent {
    val sharedAttachment = (sharedId ?: sharedComicId)?.let { id ->
        val type = sharedType ?: if (sharedComicId != null) ShareType.COMIC else null
        type?.let { SharedAttachment(id, it, sharedTitle, sharedPreview, sharedImageUrl) }
    }

    if (mediaType == "SYSTEM_TYPING") {
        return MessageContent.System(signal = content, shared = sharedAttachment)
    }

    val hasText = content.isNotEmpty()
    val hasImages = imageUrls.isNotEmpty()
    val hasVideos = videoUrls.isNotEmpty()

    return when {
        hasText && (hasImages || hasVideos) -> MessageContent.TextWithMedia(
            body = content,
            imageUrls = imageUrls,
            videoUrls = videoUrls,
            shared = sharedAttachment
        )
        hasImages -> MessageContent.Image(urls = imageUrls, shared = sharedAttachment)
        hasVideos -> MessageContent.Video(urls = videoUrls, shared = sharedAttachment)
        hasText -> MessageContent.Text(body = content, shared = sharedAttachment)
        sharedAttachment != null -> MessageContent.Shared(
            id = sharedAttachment.id,
            type = sharedAttachment.type,
            shared = sharedAttachment
        )
        else -> MessageContent.Text(body = content)
    }
}