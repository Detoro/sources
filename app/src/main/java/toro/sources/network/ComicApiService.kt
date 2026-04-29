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
import toro.sources.DataModels.Chapter
import toro.sources.DataModels.ChatMessage
import toro.sources.DataModels.ChatRequest
import toro.sources.DataModels.Comic
import toro.sources.DataModels.Comment
import toro.sources.DataModels.Page
import toro.sources.DataModels.Post
import toro.sources.DataModels.ServerResponse
import toro.sources.DataModels.SubscribeResponse
import toro.sources.DataModels.User

interface ComicApiService {
    @GET("api/comics/catalog")
    suspend fun getCatalog(): List<Comic>

    @POST("api/hello")
    suspend fun sayHello(@Body comic: Comic): ServerResponse

    @GET("api/comics/{comicId}/chapters")
    suspend fun getChaptersForComic(
        @Path("comicId") comicId: String
    ): List<Chapter>

    @GET("api/comics/chapters/{chapterId}/pages")
    suspend fun getPagesForChapter(
        @Path("chapterId") chapterId: String
    ): List<Page>

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

    @GET("api/users/friends")
    suspend fun getFriends(): List<User>

    @GET("api/chat/requests")
    suspend fun getChatRequests(): List<ChatRequest>

    @GET("api/chat/{conversationId}/messages")
    suspend fun getChatMessages(@Path("conversationId") conversationId: String): List<ChatMessage>

    @POST("api/chat/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body message: ChatMessage
    ): ServerResponse

    @GET("api/community/posts")
    suspend fun getCommunityPosts(): List<Post>

    @GET("api/community/posts/{postId}/comments")
    suspend fun getPostComments(@Path("postId") postId: String): List<Comment>

    @POST("api/community/posts/{postId}/comments")
    suspend fun addComment(
        @Path("postId") postId: String,
        @Body comment: Comment
    ): ServerResponse
}