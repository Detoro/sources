package toro.sources.dataModels

import kotlinx.serialization.Serializable

@Serializable
data class ServerResponse(
    val message: String
)

@Serializable
data class AuthorRequest(val authorId: String)

// The model to catch the toggle status
@Serializable
data class SubscribeResponse(val isSubscribed: Boolean)