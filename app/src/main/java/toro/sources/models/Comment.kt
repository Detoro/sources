package toro.sources.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import models.CommentLocation
import models.FeedContent
import models.ShareType
import models.SharedAttachment

@Entity(tableName = "comments")
@Serializable
data class Comment(
    @PrimaryKey val id: String,
    val commentLocation: CommentLocation,
    val authorId: String,
    val authorName: String,
    val content: String = "",
    val authorAvatarUrl: String? = null,
    val timestamp: Long = 0L,
    val parentId: String? = null,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSpoiler: Boolean = false,
    val repliesCount: Int = 0,
    val sharedId: String? = null,
    val sharedType: ShareType? = null,
    val sharedTitle: String?,
    val sharedPreview: String?,
    val sharedImageUrl: String?
)

fun Comment.toContent(): FeedContent {
    val sharedAttachment = sharedId?.let { id ->
        sharedType?.let { type ->
            SharedAttachment(id, type, sharedTitle, sharedPreview, sharedImageUrl)
        }
    }

    return FeedContent.Text(
        body = content,
        shared = sharedAttachment
    )
}