package toro.sources

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import models.Creator
import models.Role
import toro.sources.models.Chapter
import toro.sources.models.Comic
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.UUID

class CbzParser(private val context: Context) {

    suspend fun parseAndSave(fileUri: Uri, author: String, description: String, authorId: String = ""): Pair<Comic, Chapter> {
        return withContext(Dispatchers.IO) {

            val comicId = UUID.randomUUID().toString()
            val chapterId = UUID.randomUUID().toString()

            val title = getFileName(fileUri).substringBeforeLast(".")

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
                title = title,
                authors = listOf(Creator(id = authorId, name = author, role = Role.WRITER)),
                description = description,
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
                isDownloaded = true,
                pageCount = pagePaths.size
            )

            Pair(comic, chapter)
        }
    }

    private fun isImage(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp")
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Imported Comic"
    }
}