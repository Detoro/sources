package toro.sources.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import models.NotificationType

@Entity(tableName = "notifications")
@Serializable
data class Notification(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val relatedId: String? = null
)