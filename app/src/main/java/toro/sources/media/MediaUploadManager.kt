package toro.sources.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import models.ChapterUploadData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import toro.sources.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MediaUploadManager(private val context: Context) {

    private val MAX_IMAGE_DIMENSION = 2500
    suspend fun processAndUploadChapter(uri: Uri, chapterNumber: Float): ChapterUploadData = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "upload_extract_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        var chapterTitle = "Chapter ${chapterNumber.toInt()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                chapterTitle = cursor.getString(nameIndex).substringBeforeLast(".")
            }
        }

        val pageFiles = mutableListOf<File>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zipInput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && isImageFile(entry.name)) {
                        val file = File(tempDir, entry.name.split("/").last())
                        FileOutputStream(file).use { output -> zipInput.copyTo(output) }

                        val optimizedFile = compressAndResizeLocally(file)
                        pageFiles.add(optimizedFile)
                    }
                    entry = zipInput.nextEntry
                }
            }
        }
        pageFiles.sortBy { it.name }

        val pageUrls = pageFiles.map { file ->
            async { uploadFileToCloudinary(Uri.fromFile(file)) }
        }.awaitAll()

        tempDir.deleteRecursively()

        ChapterUploadData(
            title = chapterTitle,
            chapterNumber = chapterNumber,
            pageCount = pageUrls.size,
            pageUrls = pageUrls
        )
    }

    private fun compressAndResizeLocally(file: File): File {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var width = options.outWidth
        var height = options.outHeight

        if (width <= MAX_IMAGE_DIMENSION && height <= MAX_IMAGE_DIMENSION) {
            if (file.length() < 500 * 1024) return file
        }

        if (width > height) {
            if (width > MAX_IMAGE_DIMENSION) {
                height = (height * MAX_IMAGE_DIMENSION.toFloat() / width).toInt()
                width = MAX_IMAGE_DIMENSION
            }
        } else {
            if (height > MAX_IMAGE_DIMENSION) {
                width = (width * MAX_IMAGE_DIMENSION.toFloat() / height).toInt()
                height = MAX_IMAGE_DIMENSION
            }
        }

        val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true)
        
        val optimizedFile = File(file.parent, "opt_${file.name.substringBeforeLast(".")}.webp")
        FileOutputStream(optimizedFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
        }
        
        originalBitmap.recycle()
        if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
        
        return optimizedFile
    }

    suspend fun uploadFileToCloudinary(uri: Uri): String = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .unsigned(BuildConfig.CLOUDINARY_PRESET)
            .option("resource_type", "auto")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    continuation.resume(resultData["secure_url"] as String)
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resumeWithException(Exception(error.description))
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun isImageFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }
}