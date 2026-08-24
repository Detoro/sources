package toro.sources.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.DELETE
import com.toro.models.AuthRequest
import com.toro.models.AuthResponse
import com.toro.models.AuthorRequest
import com.toro.models.Chapter
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
import com.toro.models.RegisterChaptersRequest
import com.toro.models.RegisterComicRequest
import com.toro.models.BoolResponse
import com.toro.models.ForgotPasswordRequest
import com.toro.models.RefreshTokenRequest
import com.toro.models.ReportRequest
import com.toro.models.ResetPasswordRequest

interface ComicApiService {

    // COMICS APIs

    @GET("comics/recommendation")
    suspend fun getRecommendation(): List<Comic>

    @GET("comics/trending")
    suspend fun getTrending(): List<Comic>

    @GET("comics/search")
    suspend fun searchComics(@Query("q") query: String): List<Comic>

    @GET("comics/comic/{comicId}")
    suspend fun getComicById(@Path("comicId") comicId: String): Comic

    @POST("comics/register")
    suspend fun registerNewComic(@Body request: RegisterComicRequest): ServerResponse

    @POST("comics/comic/{comicId}/rate")
    suspend fun rateComic(
        @Path("comicId") comicId: String,
        @Query("rating") rating: Float
    ): ServerResponse

    // CHAPTER APIs

    @GET("comics/comic/{comicId}/chapters")
    suspend fun getChaptersForComic(@Path("comicId") comicId: String): List<Chapter>

    @POST("comics/comic/{comicId}/register-chapters")
    suspend fun registerChapters(
        @Path("comicId") comicId: String,
        @Body request: RegisterChaptersRequest
    ): ServerResponse

    @GET("comics/chapters/{chapterId}/pages")
    suspend fun getPagesForChapter(@Path("chapterId") chapterId: String): List<Page>

    @POST("comics/comic/{comicId}/chapters/{chapterId}/read")
    suspend fun markChapterAsRead(
        @Path("comicId") comicId: String,
        @Path("chapterId") chapterId: String,
    ): BoolResponse

    @POST("comics/comic/{comicId}/chapters/{chapterId}/like")
    suspend fun likeChapter(
        @Path("comicId") comicId: String,
        @Path("chapterId") chapterId: String,
    ): ServerResponse

    // COMMENT APIs

    @GET("comics/chapters/{chapterId}/comments")
    suspend fun getChapterComments(@Path("chapterId") chapterId: String): List<Comment>

    @POST("comics/chapters/{chapterId}/comments")
    suspend fun addChapterComment(
        @Path("chapterId") chapterId: String,
        @Body comment: CommentRequest
    ): ServerResponse

    @DELETE("comics/chapters/{chapterId}/comments/{commentId}")
    suspend fun deleteChapterComment(
        @Path("chapterId") chapterId: String,
        @Path("commentId") commentId: String
    ): ServerResponse

    @POST("comics/chapter/comments/{commentId}/like")
    suspend fun likeChapterComment(@Path("commentId") commentId: String): ServerResponse

    // SUBSCRIPTION APIs

    @POST("comics/comic/{comicId}/subscribe")
    suspend fun toggleComicSubscription(@Path("comicId") comicId: String): BoolResponse

    @GET("comics/subscriptions")
    suspend fun getSubscribedComics(): List<Comic>

    @GET("comics/history")
    suspend fun getRecentlyReadComics(): List<Comic>

    // AUTH APIs

    @POST("auth/register")
    suspend fun signUp(@Body request: AuthRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): ServerResponse

    @POST("auth/sendPasswordReset")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ServerResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ServerResponse

    // CHAT APIs

    @GET("chat/conversations")
    suspend fun getInbox(): List<Conversation>

    @GET("chat/requests")
    suspend fun getChatRequests(): List<ChatRequest>

    @POST("chat/requests")
    suspend fun sendChatRequest(@Query("receiverId") receiverId: String): ServerResponse

    @POST("chat/requests/{requestId}/accept")
    suspend fun acceptChatRequest(@Path("requestId") requestId: String): ServerResponse

    @POST("chat/requests/{requestId}/decline")
    suspend fun declineChatRequest(@Path("requestId") requestId: String): ServerResponse

    @GET("chat/{conversationId}/messages")
    suspend fun getChatMessages(
        @Path("conversationId") conversationId: String,
        @Query("since") since: Long? = null
    ): List<ChatMessage>

    @DELETE("chat/{conversationId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String
    ): ServerResponse

    @POST("chat/{conversationId}/messages/{messageId}/edit")
    suspend fun updateMessage(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String,
        @Body message: ChatMessage
    ): ServerResponse

    @POST("chat/messages/{messageId}/delivered")
    suspend fun markMessageAsDelivered(@Path("messageId") messageId: String): ServerResponse

    @POST("chat/messages/{messageId}/read")
    suspend fun markMessageAsRead(@Path("messageId") messageId: String): ServerResponse

    @POST("chat/sync")
    suspend fun syncPendingMessages(@Body pendingMessage: List<ChatMessage>): BoolResponse

    // COMMUNITY APIs

    @GET("community/posts")
    suspend fun getCommunityPosts(): List<Post>

    @POST("community/posts")
    suspend fun makePost(@Body request: PostRequest): ServerResponse

    @POST("community/posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: String): ServerResponse

    @POST("community/posts/{postId}/bookmark")
    suspend fun bookmarkPost(@Path("postId") postId: String): ServerResponse

    @GET("community/posts/{postId}/comments")
    suspend fun getPostComments(@Path("postId") postId: String): List<Comment>

    @POST("community/posts/{postId}/comments")
    suspend fun addPostComment(
        @Path("postId") postId: String,
        @Body comment: CommentRequest
    ): ServerResponse

    @POST("community/posts/comments/{commentId}/like")
    suspend fun likePostComment(@Path("commentId") commentId: String): ServerResponse

    @DELETE("community/posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: String): ServerResponse

    @DELETE("community/posts/{postId}/comments/{commentId}")
    suspend fun deletePostComment(
        @Path("postId") postId: String,
        @Path("commentId") commentId: String
    ): ServerResponse

    // USER APIs

    @GET("users/{userId}/profile")
    suspend fun getUserProfile(@Path("userId") userId: String): UserProfile

    @POST("users/{userId}/unfriend")
    suspend fun unfriendUser(@Path("userId") userId: String): ServerResponse

    @POST("users/{userId}/profile/bio")
    suspend fun updateBio(@Path("userId") userId: String, @Body request: UpdateBioRequest): ServerResponse

    @POST("users/{userId}/profile/username")
    suspend fun updateUsername(@Path("userId") userId: String, @Body request: UpdateUsernameRequest): ServerResponse

    @GET("users/{userId}/posts")
    suspend fun getUserPosts(@Path("userId") userId: String): List<Post>

    @GET("users/{userId}/works")
    suspend fun getUserWorks(@Path("userId") userId: String): List<Comic>

    @POST("users/{userId}/profile/privacy")
    suspend fun toggleProfilePrivacy(@Path("userId") userId: String): ServerResponse

    @POST("users/interests")
    suspend fun updateInterests(@Path("userId") userId: String, @Body request: UpdateInterestsRequest): ServerResponse

    @POST("users/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): ServerResponse

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserProfile>

    @GET("users/author/subscribe")
    suspend fun getSubscribedAuthors(): List<UserProfile>

    @POST("users/author/subscribe")
    suspend fun subscribeToAuthor(@Body request: AuthorRequest)

    @POST("users/avatar")
    suspend fun updateAvatar(@Body avatar: String): ServerResponse

    @DELETE("users")
    suspend fun deleteAccount(): ServerResponse

    // MISCELLANEOUS APIs

    @POST("reports/submit")
    suspend fun submitReport(@Body request: ReportRequest): ServerResponse

    @POST("notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(
        @Path("notificationId") notificationId: String
    ): ServerResponse
}