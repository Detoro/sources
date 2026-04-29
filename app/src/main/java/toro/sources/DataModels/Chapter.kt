package toro.sources.DataModels

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
    val comicId: String,
    val chapterTitle: String,
    val chapterNumber: Float? = null,
    val lastReadPageIndex: Int = 0,
    val isDownloaded: Boolean = false,
    val storageBucketPath: String? = null,
)