package toro.sources.DataModels

data class Page(
    val pageNumber: Int,                 // To keep them strictly ordered
    val imageUrl: String,                // Remote API URL for streaming
    val localUri: String? = null         // Local device URI for sideloaded/downloaded pages
)