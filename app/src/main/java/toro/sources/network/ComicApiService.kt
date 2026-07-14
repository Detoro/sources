package toro.sources.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import com.toro.models.AuthRequest
import com.toro.models.AuthResponse
import com.toro.models.AuthorRequest
import Chapter
import com.toro.models.ChatMessage
import com.toro.models.ChatRequest
import com.toro.models.Comic
import com.toro.models.Comment
import com.toro.models.CommentRequest
import com.toro.models.Conversation
import com.toro.models.Page
import com.toro.models.Post
import com.toro.models.PostRequest
import com.toro.models.ServerResponse
import com.toro.models.UserProfile
import com.toro.models.UpdateBioRequest
import com.toro.models.UpdateUsernameRequest
import com.toro.models.UpdateInterestsRequest
import com.toro.models.FcmTokenRequest
import RegisterChaptersRequest
import RegisterComicRequest
import com.toro.models.BoolResponse
import com.toro.models.RefreshTokenRequest
import com.toro.models.ReportRequest
import retrofit2.http.DELETE

interface ComicApiService {

    // comics apis
    @GET("api/comics/recommendation")
    suspend fun getRecommendation(): List<Comic>

    @GET("api/comics/trending")
    suspend fun getTrending(): List<Comic>

    @GET("api/comics/{comicId}")
    suspend fun getComicById(@Path("comicId") comicId: String): Comic

    @GET("api/comics/{comicId}/chapters")
    suspend fun getChaptersForComic(
        @Path("comicId") comicId: String
    ): List<Chapter>

    @GET("api/comics/chapters/{chapterId}/pages")
    suspend fun getPagesForChapter(
        @Path("chapterId") chapterId: String
    ): List<Page>

    @POST("api/comics/{comicId}/rate")
    suspend fun rateComic(
        @Path("comicId") comicId: String,
        @Query("rating") rating: Float
    ): ServerResponse

    @POST("api/comics/chapter/comments/{commentId}/like")
    suspend fun likeChapterComment(
        @Path("commentId") commentId: String
    ): ServerResponse

    @GET("api/comics/{chapterId}/comments")
    suspend fun getChapterComments(
        @Path("chapterId") chapterId: String
    ): List<Comment>

    @POST("api/comics/{chapterId}/comments")
    suspend fun addChapterComment(
        @Path("chapterId") chapterId: String,
        @Body comment: CommentRequest
    ): ServerResponse

    @DELETE("api/comics/{chapterId}/comments/{commentId}")
    suspend fun deleteChapterComment(
        @Path("chapterId") chapterId: String,
        @Path("commentId") commentId: String
    ): ServerResponse

    @POST("api/comics/register")
    suspend fun registerNewComic(
        @Body request: RegisterComicRequest
    ): ServerResponse

    @POST("api/comics/{comicId}/register-chapters")
    suspend fun registerChapters(
        @Path("comicId") comicId: String,
        @Body request: RegisterChaptersRequest
    ): ServerResponse

    @POST("api/comics/{comicId}/chapters/{chapterId}/like")
    suspend fun likeChapter(
        @Path("comicId") comicId: String,
        @Path("chapterId") chapterId: String,
    ): ServerResponse

    @GET("api/comics/search")
    suspend fun searchComics(@Query("q") query: String): List<Comic>

    @POST("api/comics/subscribe/comic/{comicId}")
    suspend fun toggleComicSubscription(@Path("comicId") comicId: String): BoolResponse

    @GET("api/comics/subscriptions")
    suspend fun getSubscribedComics(): List<Comic>

    // Auth apis
    @POST("api/auth/register")
    suspend fun signUp(@Body request: AuthRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): ServerResponse

    @POST("api/comics/{comicId}/chapters/{chapterId}/read")
    suspend fun markChapterAsRead(
        @Path("comicId") comicId: String,
        @Path("chapterId") chapterId: String,
    ): BoolResponse

    // Chat apis
    @GET("api/chat/conversations")
    suspend fun getInbox(): List<Conversation>

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

    @DELETE("api/chat/{conversationId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String
    ): ServerResponse

    @POST("api/chat/{conversationId}/messages/{messageId}/edit")
    suspend fun updateMessage(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String,
        @Body message: ChatMessage
    ): ServerResponse

    @POST("api/chat/messages/{messageId}/delivered")
    suspend fun markMessageAsDelivered(@Path("messageId") messageId: String): ServerResponse

    @POST("api/chat/messages/{messageId}/read")
    suspend fun markMessageAsRead(@Path("messageId") messageId: String): ServerResponse

    @POST("api/chat/sync")
    suspend fun syncPendingMessages(@Body pendingMessage: List<ChatMessage>): BoolResponse

    // community apis
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

    @POST("api/community/posts/comments/{commentId}/like")
    suspend fun likePostComment(@Path("commentId") commentId: String): ServerResponse

    @DELETE("api/community/posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: String): ServerResponse

    @DELETE("api/community/posts/{postId}/comments/{commentId}")
    suspend fun deletePostComment(
        @Path("postId") postId: String,
        @Path("commentId") commentId: String
    ): ServerResponse

    // users apis
    @GET("api/users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: String): UserProfile

    @POST("api/users/{userId}/unfriend")
    suspend fun unfriendUser(@Path("userId") userId: String): ServerResponse

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

    @POST("api/users/interests")
    suspend fun updateInterests(@Path("userId") userId: String, @Body request: UpdateInterestsRequest): ServerResponse

    @POST("api/users/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): ServerResponse

    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserProfile>

    @GET("api/users/author/subscribe")
    suspend fun getSubscribedAuthors(): List<UserProfile>

    @POST("api/users/author/subscribe")
    suspend fun subscribeToAuthor(@Body request: AuthorRequest)

    @POST("api/users/avatar")
    suspend fun updateAvatar(@Body avatar: String): ServerResponse

    @DELETE("api/users")
    suspend fun deleteAccount(): ServerResponse

    // miscellaneous
    @POST("api/reports/submit")
    suspend fun submitReport(@Body request: ReportRequest): ServerResponse

    @POST("api/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(
        @Path("notificationId") notificationId: String
    ): ServerResponse
}