package toro.sources.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class Page(
    val id: String = "",
    val chapterId: String = "",
    val pageNumber: Int,
    val imageUrl: String,
    val localUri: String? = null
)