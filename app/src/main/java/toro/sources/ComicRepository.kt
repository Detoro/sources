package toro.sources

import android.net.Uri
import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import toro.sources.DataModels.Comic
import toro.sources.DataModels.Chapter
import toro.sources.DataModels.Page

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

        // If the user sideloaded this comic, OR they downloaded a remote chapter
        return if (comic?.isLocalSideload == true || chapter.isDownloaded) {
            getLocalPages(comicId, chapterId)
        } else {
            // Otherwise, fetch the image URLs from your backend server
            apiService.getPagesForChapter(chapterId)
        }
    }

    // for local files
    suspend fun importLocalComic(fileUri: Uri, title: String) {
        try {
            val (comic, chapter) = cbzParser.parseAndSave(fileUri, title)

            comicDao.insertComic(comic)

            chapterDao.insertChapters(listOf(chapter))

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Parse error", "Damn error")
        }
    }

    suspend fun updateProgress(chapterId: String, pageIndex: Int) {
        chapterDao.updateReadingProgress(chapterId, pageIndex)
    }

    private suspend fun getLocalPages(comicId: String, chapterId: String): List<Page> {
        return withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "sideloaded_comics/$comicId/$chapterId")

            if (!directory.exists()) return@withContext emptyList<Page>()

            // Grab all images, sort them alphabetically (001.jpg, 002.jpg), and map to Page objects
            directory.listFiles()
                ?.filter { it.isFile && isImage(it.name) }
                ?.sortedBy { it.name }
                ?.mapIndexed { index, file ->
                    Page(
                        pageNumber = index,
                        imageUrl = "", // Blank because it's local
                        localUri = file.toURI().toString() // Coil reads this URI perfectly
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
            // Here you could throw a custom error to let the ViewModel know the network failed
        }
    }

    // I want the author to be able to pick whether they want vertical or horizontal scroll
}