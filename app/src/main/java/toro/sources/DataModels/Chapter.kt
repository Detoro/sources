package toro.sources.DataModels

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("comicId")] // Indexing the foreign key speeds up database queries
)
data class Chapter(
    @PrimaryKey val id: String,
    val comicId: String,
    val chapterTitle: String,
    val chapterNumber: Float? = null,            // Float allows for decimal issues (e.g., Chapter 10.5)
    val lastReadPageIndex: Int = 0,      // Crucial for bookmarking where the user left off
    val isDownloaded: Boolean = false    // True if the remote chapter was cached for offline reading
)