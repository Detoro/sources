package toro.sources.dataModels

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW,
    FRIEND_REQUEST,
    SYSTEM,
    CHAT;

    companion object {
        fun fromString(value: String): NotificationType? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}

@Serializable
data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val relatedId: String? = null
)