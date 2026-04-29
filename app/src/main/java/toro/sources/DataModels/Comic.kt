package toro.sources.DataModels

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "comics")
@Serializable
data class Comic(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val coverImageUrl: String,
    val isLocalSideload: Boolean = false,
    val localFilePath: String? = null,
    val scrollDirection: String = "VERTICAL"
)