package toro.sources.db

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import models.ComicStatus
import models.Creator
import models.Genre
import models.PgRating
import models.ShareType

class Converters {
    @TypeConverter
    fun fromGenreList(value: List<Genre>): String {
        return Json.encodeToString<List<Genre>>(value)
    }

    @TypeConverter
    fun toGenreList(value: String): List<Genre> {
        return Json.decodeFromString<List<Genre>>(value)
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

    @TypeConverter
    fun fromCreatorList(value: List<Creator>): String {
        return Json.encodeToString<List<Creator>>(value)
    }

    @TypeConverter
    fun toCreatorList(value: String): List<Creator> {
        return Json.decodeFromString<List<Creator>>(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Json.encodeToString<List<String>>(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return Json.decodeFromString<List<String>>(value)
    }
}