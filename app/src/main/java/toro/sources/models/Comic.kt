package toro.sources.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import models.Creator
import models.Genre
import models.PgRating
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import models.ComicStatus

@Entity(tableName = "comics")
@Serializable
data class Comic(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    @SerialName("cover_image_url") val coverImageUrl: String,
    @SerialName("is_local_sideload") val isLocalSideload: Boolean = false,
    @SerialName("local_file_path") val localFilePath: String? = null,
    @SerialName("scroll_direction") val scrollDirection: String = "VERTICAL",
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    @SerialName("last_read_timestamp") val lastReadTimestamp: Long = 0L,
    @SerialName("subscribe_timestamp") val subscribeTimestamp: Long = 0L,
    @SerialName("last_update_timestamp") val lastUpdateTimestamp: Long = 0L,
    val status: ComicStatus = ComicStatus.ONGOING,
    @SerialName("subs_count") val subsCount: Int = 0,
    @SerialName("read_chapters_count") val readChapterCount: Int = 0,
    @SerialName("chapter_count") val chapterCount: Int = 0,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("rating") val rating: Float = 0f,
    @SerialName("pg_rating") val pgRating: PgRating = PgRating.ALL,
    val genres: List<Genre> = emptyList(),
    @SerialName("authors") val authors: List<Creator> = emptyList()
)

val Comic.creditsMap: Map<String, List<Creator>>
    get() = authors.groupBy(
        keySelector = { it.role.name.lowercase() }
    )

val Comic.authorName: String
    get() = creditsMap["writer"]?.firstOrNull()?.name ?: authors.firstOrNull()?.name ?: "Unknown"

val Comic.authorId: String
    get() = creditsMap["writer"]?.firstOrNull()?.id ?: authors.firstOrNull()?.id ?: ""

val Comic.writtenBy: String
    get() = creditsMap["writer"]?.joinToString(", ") { it.name } ?: authorName

val Comic.artBy: String
    get() = creditsMap["artist"]?.joinToString(", ") { it.name } ?: "Unknown"