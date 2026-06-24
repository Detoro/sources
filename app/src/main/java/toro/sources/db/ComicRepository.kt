package toro.sources.db

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import toro.sources.CbzParser
import toro.sources.network.ComicApiService
import Chapter
import com.toro.models.Comic
import com.toro.models.Conversation
import com.toro.models.ChatMessage
import com.toro.models.Comment
import com.toro.models.Notification
import com.toro.models.Page
import com.toro.models.Post
import java.io.File

class ComicRepository(
    private val context: Context,
    private var comicDao: ComicDao,
    private var chapterDao: ChapterDao,
    private var conversationDao: ConversationDao,
    private var notificationDao: NotificationDao,
    private var commentDao: CommentDao,
    private var postDao: PostDao,
    private val cbzParser: CbzParser,
    private val apiService: ComicApiService
) {
    fun refreshDAOs() {
        val db = CanvasDatabase.getDatabase(context)
        comicDao = db.comicDao()
        chapterDao = db.chapterDao()
        conversationDao = db.conversationDao()
        notificationDao = db.notificationDao()
        commentDao = db.commentDao()
        postDao = db.postDao()
    }
    // new novels
    fun getMyLibrary(): Flow<List<Comic>> {
        return comicDao.getAllComics()
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

    suspend fun getPagesForChapter(chapterId: String, comicId: String): List<Page> {
        val chapter = chapterDao.getChapterById(chapterId)
        val comic = comicDao.getComicByIdSync(comicId)

        if (chapter == null) {
            Log.e("Repository", "Chapter not found in local DB: $chapterId")
            return emptyList()
        }

        Log.i("Repository", "Fetching pages for Chapter: ${chapter.chapterTitle}, ID: $chapterId")

        val pages = if (comic?.isLocalSideload == true || chapter.isDownloaded) {
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
        title: String,
        author: String,
        description: String,
    ) {
        try {
            val (comic, chapter) = cbzParser.parseAndSave(fileUri, title, author, description)

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
            withContext(Dispatchers.IO) {
                val directory = File(context.filesDir, "sideloaded_comics")
                if (directory.exists()) {
                    directory.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            Log.e("Repository", "Failed to clear all data", e)
        }
    }

    suspend fun updateProgress(chapterId: String, pageIndex: Int) {
        chapterDao.updateReadingProgress(chapterId, pageIndex)
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
        comicDao.insertComic(comic)

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
                    delay(2000L * currentRetry)
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
                    delay(2000L * currentRetry)
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

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> {
        return conversationDao.getMessagesForConversation(conversationId)
    }

    suspend fun saveConversations(conversations: List<Conversation>) {
        conversationDao.insertConversations(conversations)
    }

    suspend fun saveMessages(messages: List<ChatMessage>) {
        conversationDao.insertMessages(messages)
    }

    suspend fun saveMessage(message: ChatMessage) {
        conversationDao.insertMessage(message)
    }

    suspend fun deleteMessageById(messageId: String) {
        conversationDao.deleteMessageById(messageId)
    }

    suspend fun deleteMessagesForConversation(conversationId: String) {
        conversationDao.deleteMessagesForConversation(conversationId)
    }

    suspend fun deleteConversationById(conversationId: String) {
        conversationDao.deleteConversationById(conversationId)
    }

    suspend fun updateMessageReadStatus(messageId: String, read: Boolean) {
        conversationDao.updateMessageReadStatus(messageId, read)
    }

    suspend fun updateMessageContent(messageId: String, content: String) {
        conversationDao.updateMessageContent(messageId, content)
    }

    fun getNotifications(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications()
    }

    suspend fun saveNotification(notification: Notification) {
        notificationDao.insert(notification)
    }

    suspend fun deleteNotificationById(notificationId: String) {
        notificationDao.deleteById(notificationId)
    }

    suspend fun clearNotifications() {
        notificationDao.deleteAll()
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

    suspend fun clearComments() {
        commentDao.deleteAll()
    }

    fun getPosts(): Flow<List<Post>> {
        return postDao.getUserPosts()
    }

    suspend fun savePost(post: Post) {
        postDao.insert(post)
    }

    suspend fun deletePostById(commentId: String) {
        postDao.deleteById(commentId)
    }

    suspend fun clearPosts() {
        postDao.deleteAll()
    }
}