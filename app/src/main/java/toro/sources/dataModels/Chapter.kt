package toro.sources.dataModels

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
    val lastReadPageIndex: Int = 0,
    val isDownloaded: Boolean = false,
    val isLiked: Boolean = false,
    val pageCount: Int,
)

@Serializable
data class RegisterComicRequest(
    val title: String,
    val author: String,
    val description: String,
    val coverUrl: String?,
    val chapters: List<ChapterUploadData>
)

@Serializable
data class RegisterChaptersRequest(
    val chapters: List<ChapterUploadData>
)

@Serializable
data class ChapterUploadData(
    val title: String,
    val chapterNumber: Float,
    val pageCount: Int,
    val pageUrls: List<String>
)