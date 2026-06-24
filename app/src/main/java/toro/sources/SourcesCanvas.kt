package toro.sources

import android.app.Application
import com.cloudinary.android.MediaManager
import toro.sources.db.CanvasDatabase
import toro.sources.network.RetrofitClient.comicApiService
import toro.sources.db.ComicRepository


class SourcesCanvas : Application() {

    override fun onCreate() {
        super.onCreate()
        initCloudinary()
    }

    private fun initCloudinary() {
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUD_NAME,
            "secure" to true
        )
        MediaManager.init(this, config)
    }

    val database by lazy { CanvasDatabase.getDatabase(this) }

    val cbzParser by lazy { CbzParser(this) }

    val apiService by lazy { comicApiService }

    val repository by lazy {
        ComicRepository(
            context = this,
            comicDao = database.comicDao(),
            chapterDao = database.chapterDao(),
            conversationDao = database.conversationDao(),
            notificationDao = database.notificationDao(),
            commentDao = database.commentDao(),
            postDao = database.postDao(),
            cbzParser = cbzParser,
            apiService = apiService
        )
    }
}