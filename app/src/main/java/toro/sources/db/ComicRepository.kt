package toro.sources.db

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import toro.sources.CbzParser
import toro.sources.network.ComicApiService
import com.toro.models.Chapter
import com.toro.models.Comic
import com.toro.models.Conversation
import com.toro.models.ChatMessage
import com.toro.models.Page
import java.io.File

class ComicRepository(
    private val context: Context,
    private val comicDao: ComicDao,
    private val chapterDao: ChapterDao,
    private val conversationDao: ConversationDao,
    private val cbzParser: CbzParser,
    private val apiService: ComicApiService
) {

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

    suspend fun syncRemoteChaptersForComic(comic: Comic) {
        try {
            if (comic.id.isBlank()) {
                Log.e("Sync", "Cannot sync chapters for comic with blank ID")
                return
            }
            comicDao.insertComic(comic)

            val remoteChapters = apiService.getChaptersForComic(comic.id)
            val sanitizedChapters = remoteChapters.map { it.copy(comicId = comic.id) }
            
            chapterDao.insertChapters(sanitizedChapters)
        } catch (e: Exception) {
            Log.e("Network Error", "Failed to fetch chapters for ${comic.title}: ${e.message}")
        }
    }

    suspend fun syncSubscriptions() {
        try {
            val remoteSubs = apiService.getSubscribedComics()
            val syncedComics = remoteSubs.map { it.copy(isSubscribed = true) }
            insertComics(syncedComics)
        } catch (e: Exception) {
            Log.e("Sync", "Failed to sync subscriptions", e)
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
}