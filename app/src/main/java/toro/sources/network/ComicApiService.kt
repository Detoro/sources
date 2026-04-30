package toro.sources.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import toro.sources.DataModels.AuthRequest
import toro.sources.DataModels.AuthResponse
import toro.sources.DataModels.AuthorRequest
import toro.sources.DataModels.Bookmark
import toro.sources.DataModels.Chapter
import toro.sources.DataModels.ChatMessage
import toro.sources.DataModels.ChatRequest
import toro.sources.DataModels.Comic
import toro.sources.DataModels.Comment
import toro.sources.DataModels.Conversation
import toro.sources.DataModels.Page
import toro.sources.DataModels.Post
import toro.sources.DataModels.ServerResponse
import toro.sources.DataModels.SubscribeResponse

interface ComicApiService {
    @GET("api/comics/catalog")
    suspend fun getCatalog(): List<Comic>

    @GET("api/comics/{comicId}/chapters")
    suspend fun getChaptersForComic(
        @Path("comicId") comicId: String
    ): List<Chapter>

    @GET("api/comics/chapters/{chapterId}/pages")
    suspend fun getPagesForChapter(
        @Path("chapterId") chapterId: String
    ): List<Page>

    @GET("api/chat/conversations")
    suspend fun getInbox(): List<Conversation>

    @GET("api/comics/search")
    suspend fun searchComics(@Query("q") query: String): List<Comic>

    @POST("api/subscribe/author")
    suspend fun subscribeToAuthor(@Body request: AuthorRequest)

    @POST("api/subscribe/comic/{comicId}")
    suspend fun toggleComicSubscription(@Path("comicId") comicId: String): SubscribeResponse

    @Multipart
    @POST("api/comics/upload")
    suspend fun uploadComic(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("author") author: RequestBody,
        @Part("description") description: RequestBody,
        @Part cover: MultipartBody.Part? = null
    ): ServerResponse

    @POST("api/auth/register")
    suspend fun signUp(@Body request: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @GET("api/chat/requests")
    suspend fun getChatRequests(): List<ChatRequest>

    @POST("api/chat/requests/{requestId}/accept")
    suspend fun acceptChatRequest(@Path("requestId") requestId: String): ServerResponse

    @POST("api/chat/requests/{requestId}/decline")
    suspend fun declineChatRequest(@Path("requestId") requestId: String): ServerResponse

    @GET("api/chat/{conversationId}/messages")
    suspend fun getChatMessages(@Path("conversationId") conversationId: String): List<ChatMessage>

    @POST("api/chat/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body message: ChatMessage
    ): ServerResponse

    @GET("api/community/posts")
    suspend fun getCommunityPosts(): List<Post>

    @POST("api/community/posts")
    suspend fun makePost(@Body post: Post): ServerResponse

    @POST("api/community/posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: String): ServerResponse

    @POST("api/community/posts/{postId}/bookmark")
    suspend fun bookmarkPost(
        @Path("postId") postId: String,
        @Body bookmark: Bookmark): ServerResponse

    @GET("api/community/posts/{postId}/comments")
    suspend fun getPostComments(@Path("postId") postId: String): List<Comment>

    @POST("api/community/posts/{postId}/comments")
    suspend fun addComment(
        @Path("postId") postId: String,
        @Body comment: Comment
    ): ServerResponse
}