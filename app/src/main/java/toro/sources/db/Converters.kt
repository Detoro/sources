package toro.sources.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import com.toro.models.ComicStatus
import com.toro.models.PgRating
import com.toro.models.ShareType
import com.toro.models.Genre

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromGenreList(value: List<Genre>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toGenreList(value: String): List<Genre> {
        return Json.decodeFromString(value)
    }

    @TypeConverter
    fun fromComicStatus(status: ComicStatus): String = status.value

    @TypeConverter
    fun toComicStatus(value: String): ComicStatus =
        ComicStatus.entries.find { it.value == value || it.name == value } ?: ComicStatus.ONGOING

    @TypeConverter
    fun fromPgRating(rating: PgRating): String = rating.name

    @TypeConverter
    fun toPgRating(value: String): PgRating =
        PgRating.entries.find { it.name == value } ?: PgRating.ALL

    @TypeConverter
    fun fromShareType(type: ShareType?): String? = type?.name

    @TypeConverter
    fun toShareType(value: String?): ShareType? =
        ShareType.entries.find { it.name == value }
}
