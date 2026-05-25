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
import toro.sources.dataModels.AuthRequest
import toro.sources.dataModels.AuthResponse
import toro.sources.dataModels.AuthorRequest
import toro.sources.dataModels.Chapter
import toro.sources.dataModels.ChatMessage
import toro.sources.dataModels.ChatRequest
import toro.sources.dataModels.Comic
import toro.sources.dataModels.Comment
import toro.sources.dataModels.CommentRequest
import toro.sources.dataModels.Conversation
import toro.sources.dataModels.Page
import toro.sources.dataModels.Post
import toro.sources.dataModels.PostRequest
import toro.sources.dataModels.ServerResponse
import toro.sources.dataModels.SubscribeResponse
import toro.sources.dataModels.UserProfile
import toro.sources.dataModels.UpdateBioRequest
import toro.sources.dataModels.UpdateUsernameRequest
import toro.sources.dataModels.FcmTokenRequest

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

    @GET("api/subscriptions")
    suspend fun getSubscribedComics(): List<Comic>

    @Multipart
    @POST("api/comics/upload")
    suspend fun uploadComic(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("author") author: RequestBody,
        @Part("description") description: RequestBody,
        @Part("coverUrl") coverUrl: RequestBody? = null
    ): ServerResponse

    @POST("api/auth/register")
    suspend fun signUp(@Body request: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("api/users/avatar")
    suspend fun updateAvatar(@Body avatar: String): ServerResponse

    @GET("api/chat/requests")
    suspend fun getChatRequests(): List<ChatRequest>

    @POST("api/chat/requests")
    suspend fun sendChatRequest(@Query("receiverId") receiverId: String): ServerResponse

    @POST("api/chat/requests/{requestId}/accept")
    suspend fun acceptChatRequest(@Path("requestId") requestId: String): ServerResponse

    @POST("api/chat/requests/{requestId}/decline")
    suspend fun declineChatRequest(@Path("requestId") requestId: String): ServerResponse

    @GET("api/chat/{conversationId}/messages")
    suspend fun getChatMessages(@Path("conversationId") conversationId: String): List<ChatMessage>

    @POST("api/chat/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Query("targetUserId") targetUserId: String,
        @Body message: ChatMessage
    ): ServerResponse

    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserProfile>

    @GET("api/users/subscribed-authors")
    suspend fun getSubscribedAuthors(): List<UserProfile>

    @GET("api/community/posts")
    suspend fun getCommunityPosts(): List<Post>

    @POST("api/community/posts")
    suspend fun makePost(
        @Body request: PostRequest
    ): ServerResponse

    @POST("api/community/posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: String): ServerResponse

    @POST("api/community/posts/{postId}/bookmark")
    suspend fun bookmarkPost(
        @Path("postId") postId: String): ServerResponse

    @GET("api/community/posts/{postId}/comments")
    suspend fun getPostComments(@Path("postId") postId: String): List<Comment>

    @POST("api/community/posts/{postId}/comments")
    suspend fun addPostComment(
        @Path("postId") postId: String,
        @Body comment: CommentRequest
    ): ServerResponse

    @POST("api/community/comments/{commentId}/like")
    suspend fun likeComment(@Path("commentId") commentId: String): ServerResponse

    @GET("api/comics/{comicId}/comments")
    suspend fun getComicComments(@Path("comicId") comicId: String): List<Comment>

    @POST("api/comics/{comicId}/comments")
    suspend fun addComicComment(
        @Path("comicId") comicId: String,
        @Body comment: CommentRequest
    ): ServerResponse

    @GET("api/users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: String): UserProfile

    @POST("api/users/{userId}/profile/bio")
    suspend fun updateBio(@Path("userId") userId: String, @Body request: UpdateBioRequest): ServerResponse

    @POST("api/users/{userId}/profile/username")
    suspend fun updateUsername(@Path("userId") userId: String, @Body request: UpdateUsernameRequest): ServerResponse

    @GET("api/users/{userId}/posts")
    suspend fun getUserPosts(@Path("userId") userId: String): List<Post>

    @GET("api/users/{userId}/works")
    suspend fun getUserWorks(@Path("userId") userId: String): List<Comic>

    @POST("api/users/{userId}/profile/privacy")
    suspend fun toggleProfilePrivacy(@Path("userId") userId: String): ServerResponse

    @POST("api/users/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): ServerResponse
}