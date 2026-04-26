package toro.sources.DataModels

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "comics")
@Serializable
data class Comic(
    @PrimaryKey val id: String,          // UUID from your API, or auto-generated for sideloaded files
    val title: String,
    val author: String,
    val description: String,
    val coverImageUrl: String,           // URL for the remote thumbnail
    val isLocalSideload: Boolean = false,// True if the user imported a .cbz/.cbr file
    val localFilePath: String? = null    // The absolute path on the device if it's sideloaded
)