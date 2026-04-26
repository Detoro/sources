package toro.sources.DataModels

import kotlinx.serialization.Serializable

@Serializable
data class ServerResponse(
    val message: String
)