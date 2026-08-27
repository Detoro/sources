package toro.sources.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import models.FeedContent
import models.ShareType
import models.SharedAttachment

@Entity(tableName = "posts")
@Serializable
data class Post(
    @PrimaryKey val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val title: String? = null,
    val content: String = "",
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    var isLiked: Boolean = false,
    val isSpoiler: Boolean = false,
    var isBookmarked: Boolean = false,
    val sharedId: String? = null,
    val sharedType: ShareType? = null,
    val sharedTitle: String?,
    val sharedPreview: String?,
    val sharedImageUrl: String?,
    val imageUrls: List<String> = emptyList(),
    val videoUrls: List<String> = emptyList()
)

fun Post.toContent(): FeedContent {
    val sharedAttachment = sharedId?.let { id ->
        sharedType?.let { type ->
            SharedAttachment(id, type, sharedTitle, sharedPreview, sharedImageUrl)
        }
    }

    return if (imageUrls.isEmpty() && videoUrls.isEmpty()) {
        FeedContent.Text(
            title = title,
            body = content,
            shared = sharedAttachment
        )
    } else {
        FeedContent.Media(
            title = title,
            body = content,
            imageUrls = imageUrls,
            videoUrls = videoUrls,
            shared = sharedAttachment
        )
    }
}