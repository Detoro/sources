package toro.sources.dataModels

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "comics")
@Serializable
data class Comic(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    @SerialName("cover_image_url") val coverImageUrl: String,
    @SerialName("is_local_sideload") val isLocalSideload: Boolean = false,
    @SerialName("local_file_path") val localFilePath: String? = null,
    @SerialName("scroll_direction") val scrollDirection: String = "VERTICAL",
    @SerialName("has_music") val hasMusic: Boolean = false,
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
    @SerialName("last_read_timestamp") val lastReadTimestamp: Long = 0L
)