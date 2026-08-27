package toro.sources.db

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import models.Page
import toro.sources.CbzParser
import toro.sources.network.ComicApiService
import toro.sources.SourcesCanvas
import toro.sources.models.Chapter
import toro.sources.models.ChatMessage
import toro.sources.models.Comic
import toro.sources.models.Comment
import toro.sources.models.Conversation
import toro.sources.models.ConversationUiState
import toro.sources.models.Notification
import toro.sources.models.Post
import toro.sources.models.UserProfile
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

class ComicRepository(
    private val context: Context,
    private val cbzParser: CbzParser,
    private val apiService: ComicApiService
) {
    private val database: CanvasDatabase
        get() = (context.applicationContext as SourcesCanvas).database

    private val comicDao get() = database.comicDao()
    private val chapterDao get() = database.chapterDao()
    private val conversationDao get() = database.conversationDao()
    private val chatMessageDao get() = database.chatMessageDao()
    private val notificationDao get() = database.notificationDao()
    private val commentDao get() = database.commentDao()
    private val postDao get() = database.postDao()
    private val authorDao get() = database.authorDao()
    private val uiStateDao get() = database.conversationUiStateDao()

    // new novels
    fun getMyLibrary(): Flow<List<Comic>> {
        return comicDao.getAllComics()
    }

    suspend fun getComicByIdSync(comicId: String): Comic? {
        return comicDao.getComicByIdSync(comicId)
    }

    // issues
    fun getChaptersForComic(comicId: String): Flow<List<Chapter>> {
        return chapterDao.getChaptersForComic(comicId)
    }

    fun getSubscribedComics(): Flow<List<Comic>> {
        return comicDao.getSubscribedComics()
    }

    fun getRecentlyReadComics(): Flow<List<Comic>> {
        return comicDao.getRecentlyReadComics()
    }

    suspend fun updateLastRead(comicId: String) {
        comicDao.updateLastReadTimestamp(comicId, System.currentTimeMillis())
    }

    suspend fun toggleLocalSubscription(comicId: String, isSubscribed: Boolean) {
        comicDao.updateSubscription(comicId, isSubscribed)
    }

    suspend fun getPagesForChapter(chapterId: String, comicId: String, loadLocally: Boolean): List<Page> {
        Log.i("Repository", "Fetching pages for Chapter ID: $chapterId (Local: $loadLocally)")

        val pages = if (loadLocally) {
            getLocalPages(comicId, chapterId)
        } else {
            apiService.getPagesForChapter(chapterId)
        }

        Log.i("Repository", "Found ${pages.size} pages")
        return pages
    }

    // for local files
    suspend fun importLocalComic(
        fileUri: Uri,
        author: String,
        description: String,
    ) {
        try {
            val (comic, chapter) = cbzParser.parseAndSave(fileUri, author, description)

            comicDao.insertComic(comic)

            chapterDao.insertChapters(listOf(chapter))

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Parse error", "Damn error")
        }
    }

    suspend fun removeComicFromLibrary(comicId: String) {
        try {
            val comic = comicDao.getComicByIdSync(comicId) ?: return

            chapterDao.deleteChaptersByComicId(comicId)
            comicDao.deleteComic(comic)

            if (comic.isLocalSideload) {
                withContext(Dispatchers.IO) {
                    val directory = File(context.filesDir, "sideloaded_comics/$comicId")
                    if (directory.exists()) {
                        directory.deleteRecursively()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Failed to remove comic", e)
        }
    }

    suspend fun clearAllData() {
        try {
            chapterDao.deleteAllChapters()
            comicDao.deleteAllComics()
            conversationDao.deleteAllConversations()
            notificationDao.deleteAllNotifications()
            commentDao.deleteAllComments()
            postDao.deleteAllPosts()
            authorDao.deleteAllAuthors()

            withContext(Dispatchers.IO) {
                val directory = File(context.filesDir, "sideloaded_comics")
                if (directory.exists()) {
                    directory.deleteRecursively()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Repository", "Failed to clear all data", e)
        }
    }

    suspend fun updateProgress(chapterId: String, pageIndex: Int) {
        chapterDao.updateReadingProgress(chapterId, pageIndex)
    }

    suspend fun updateChapterLikeState(chapterId: String, isLiked: Boolean) {
        chapterDao.updateLikeState(chapterId, isLiked)
    }

    private suspend fun getLocalPages(comicId: String, chapterId: String): List<Page> {
        return withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "sideloaded_comics/$comicId/$chapterId")

            if (!directory.exists()) return@withContext emptyList()

            directory.listFiles()
                ?.filter { it.isFile && isImage(it.name) }
                ?.sortedBy { it.name }
                ?.mapIndexed { index, file ->
                    Page(
                        pageNumber = index,
                        imageUrl = "",
                        localUri = file.toURI().toString()
                    )
                } ?: emptyList()
        }
    }

    private fun isImage(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp")
    }

    suspend fun syncRemoteChaptersForComic(comic: Comic, maxRetries: Int = 3) {
        if (comic.id.isBlank()) {
            Log.e("Sync", "Cannot sync chapters for comic with blank ID")
            return
        }

        val existing = comicDao.getComicByIdSync(comic.id)
        val comicToInsert = if (existing != null) {
            comic.copy(isSubscribed = existing.isSubscribed)
        } else {
            comic
        }
        comicDao.insertComic(comicToInsert)

        var currentRetry = 0
        while (currentRetry < maxRetries) {
            try {
                val remoteChapters = apiService.getChaptersForComic(comic.id)
                val sanitizedChapters = remoteChapters.map { it.copy(comicId = comic.id) }
                chapterDao.insertChapters(sanitizedChapters)
                return
            } catch (e: Exception) {
                currentRetry++
                if (currentRetry >= maxRetries) {
                    Log.e("Network Error", "Failed to fetch chapters for ${comic.title} after $maxRetries retries: ${e.message}")
                } else {
                    Log.w("Sync", "Fetch chapters failed for ${comic.title}, retrying ($currentRetry/$maxRetries)...")
                    delay((2000L * currentRetry).milliseconds)
                }
            }
        }
    }

    suspend fun syncSubscriptions(maxRetries: Int = 3) {
        var currentRetry = 0
        while (currentRetry < maxRetries) {
            try {
                val remoteSubs = apiService.getSubscribedComics()
                val syncedComics = remoteSubs.map { it.copy(isSubscribed = true) }
                insertComics(syncedComics)
                return
            } catch (e: Exception) {
                currentRetry++
                if (currentRetry >= maxRetries) {
                    Log.e("Sync", "Failed to sync subscriptions after $maxRetries retries", e)
                } else {
                    Log.w("Sync", "Sync failed, retrying ($currentRetry/$maxRetries)...", e)
                    delay((2000L * currentRetry).milliseconds)
                }
            }
        }
    }

    suspend fun insertComics(comics: List<Comic>) {
        comicDao.insertComics(comics)
    }

    fun getConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations()
    }

    suspend fun getConversationById(id: String): Conversation? {
        return conversationDao.getConversationById(id)
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesForConversation(conversationId)
    }

    suspend fun saveConversations(conversations: List<Conversation>) {
        conversationDao.insertConversations(conversations)
    }

    suspend fun updateChatBackground(conversationId: String, uri: String?) {
        conversationDao.updateChatBackground(conversationId, uri)
    }

    suspend fun saveMessages(messages: List<ChatMessage>) {
        chatMessageDao.insertMessages(messages)
    }

    suspend fun saveMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(message)
    }

    suspend fun getPendingMessages(): List<ChatMessage> {
        return chatMessageDao.getPendingMessages()
    }

    suspend fun getLastMessageTimestamp(conversationId: String): Long? {
        return chatMessageDao.getLastMessageTimestamp(conversationId)
    }

    suspend fun getMessageById(clientMessageId: String): ChatMessage? {
        return chatMessageDao.getMessageById(clientMessageId)
    }

    suspend fun confirmPendingMessage(clientMessageId: String, serverContent: String) {
        chatMessageDao.confirmPendingMessage(clientMessageId, serverContent)
    }

    suspend fun deleteMessageById(messageId: String) {
        chatMessageDao.deleteMessageById(messageId)
    }

    suspend fun deleteMessagesForConversation(conversationId: String) {
        chatMessageDao.deleteMessagesForConversation(conversationId)
    }

    suspend fun deleteConversationById(conversationId: String) {
        conversationDao.deleteConversationById(conversationId)
    }

    suspend fun updateMessageReadStatus(messageId: String, read: Boolean) {
        chatMessageDao.updateMessageReadStatus(messageId, read)
    }

    suspend fun updateMessageDeliveryStatus(messageId: String, isDelivered: Boolean) {
        chatMessageDao.updateMessageDeliveryStatus(messageId, isDelivered)
    }

    suspend fun updateMessageContent(messageId: String, content: String) {
        chatMessageDao.updateMessageContent(messageId, content)
    }

    fun getNotifications(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications()
    }

    suspend fun deleteNotificationById(notificationId: String) {
        notificationDao.deleteById(notificationId)
    }

    suspend fun clearNotifications() {
        notificationDao.deleteAllNotifications()
    }

    fun getComments(): Flow<List<Comment>> {
        return commentDao.getUserComments()
    }

    suspend fun saveComment(comment: Comment) {
        commentDao.insert(comment)
    }

    suspend fun deleteCommentById(commentId: String) {
        commentDao.deleteById(commentId)
    }

    fun getPosts(): Flow<List<Post>> {
        return postDao.getUserPosts()
    }

    suspend fun savePost(post: Post) {
        postDao.insert(post)
    }

    suspend fun deletePostById(postId: String) {
        postDao.deleteById(postId)
        apiService.deletePost(postId)
    }

    // authors
    fun getSubscribedAuthors(): Flow<List<UserProfile>> {
        return authorDao.getAllAuthors()
    }

    suspend fun saveAuthors(authors: List<UserProfile>) {
        authorDao.insertAuthors(authors)
    }

    fun getConversationUiState(conversationId: String) = uiStateDao.getUiState(conversationId)

    suspend fun saveConversationUiState(state: ConversationUiState) {
        uiStateDao.insertUiState(state)
    }

    suspend fun clearConversationUiState(conversationId: String) {
        uiStateDao.clearUiState(conversationId)
    }
}