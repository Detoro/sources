package toro.sources

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.UUID
import toro.sources.DataModels.Comic
import toro.sources.DataModels.Chapter

class CbzParser(private val context: Context) {

    suspend fun parseAndSave(fileUri: Uri, givenTitle: String): Pair<Comic, Chapter> {
        return withContext(Dispatchers.IO) {

            val comicId = UUID.randomUUID().toString()
            val chapterId = UUID.randomUUID().toString()

            val outputDir = File(context.filesDir, "sideloaded_comics/$comicId/$chapterId")
            if (!outputDir.exists()) outputDir.mkdirs()

            val pagePaths = mutableListOf<String>()

            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && isImage(entry.name)) {
                            val outputFile = File(outputDir, File(entry.name).name)

                            FileOutputStream(outputFile).use { fos ->
                                zip.copyTo(fos)
                            }
                            pagePaths.add(outputFile.absolutePath)
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            pagePaths.sort()

            val coverPath = pagePaths.firstOrNull() ?: ""

            val comic = Comic(
                id = comicId,
                title = givenTitle,
                author = "Unknown", // maybe use a ComicInfo.xml for this
                description = "Sideloaded from device storage.",
                coverImageUrl = coverPath,
                isLocalSideload = true,
                localFilePath = fileUri.toString()
            )

            val chapter = Chapter(
                id = chapterId,
                comicId = comicId,
                chapterTitle = "One-Shot", // Sideloaded .cbz files are usually single issues hopefully
                chapterNumber = 1f,
                lastReadPageIndex = 0,
                isDownloaded = true
            )

            Pair(comic, chapter)
        }
    }

    private fun isImage(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp")
    }
}