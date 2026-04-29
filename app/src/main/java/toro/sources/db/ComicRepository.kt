package toro.sources.db

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import toro.sources.CbzParser
import toro.sources.network.ComicApiService
import toro.sources.DataModels.Chapter
import toro.sources.DataModels.Comic
import toro.sources.DataModels.Page
import java.io.File

class ComicRepository(
    private val context: Context,
    private val comicDao: ComicDao,
    private val chapterDao: ChapterDao,
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

    suspend fun getChapterById(chapterId: String): Chapter {
        return chapterDao.getChapterById(chapterId)
    }

    suspend fun getPagesForChapter(chapterId: String, comicId: String): List<Page> {
        val chapter = chapterDao.getChapterById(chapterId)
        val comic = comicDao.getComicByIdSync(comicId)

        return if (comic?.isLocalSideload == true || chapter.isDownloaded) {
            getLocalPages(comicId, chapterId)
        } else {
            apiService.getPagesForChapter(chapterId)
        }
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

    suspend fun updateProgress(chapterId: String, pageIndex: Int) {
        chapterDao.updateReadingProgress(chapterId, pageIndex)
    }

    private suspend fun getLocalPages(comicId: String, chapterId: String): List<Page> {
        return withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "sideloaded_comics/$comicId/$chapterId")

            if (!directory.exists()) return@withContext emptyList<Page>()

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

    suspend fun syncRemoteCatalog() {
        try {
            val remoteComics = apiService.getCatalog()
            comicDao.insertComics(remoteComics)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncRemoteChaptersForComic(comic: Comic) {
        val comic = comicDao.getComicByIdSync(comic.id)
        if (comic?.isLocalSideload == false) {
            try {
                val remoteChapters = apiService.getChaptersForComic(comic.id)
                chapterDao.insertChapters(remoteChapters)
            } catch (e: Exception) {
                Log.e("Network Error", "Failed to fetch chapters: ${e.message}")
            }
        }
    }
}