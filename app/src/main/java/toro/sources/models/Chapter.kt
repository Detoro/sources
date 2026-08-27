package toro.sources.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = Comic::class,
            parentColumns = ["id"],
            childColumns = ["comicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("comicId")]
)
@Serializable
data class Chapter(
    @PrimaryKey val id: String,
    @SerialName("comic_id") val comicId: String,
    @SerialName("chapter_title") val chapterTitle: String,
    @SerialName("chapter_number") val chapterNumber: Float? = null,
    @SerialName("has_music") val hasMusic: Boolean = false,
    var lastReadPageIndex: Int = 0,
    val isDownloaded: Boolean = false,
    val isLiked: Boolean = false,
    @SerialName("is_read") var isRead: Boolean = false,
    @SerialName("page_count") val pageCount: Int,
    @SerialName("audio_url") val audioUrl: String? = null
)