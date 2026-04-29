package toro.sources

import android.app.Application
import toro.sources.db.CanvasDatabase
import toro.sources.network.RetrofitClient.comicApiService
import toro.sources.db.ComicRepository


class SourcesCanvas : Application() {

    val database by lazy { CanvasDatabase.getDatabase(this) }

    val cbzParser by lazy { CbzParser(this) }

    val apiService by lazy { comicApiService }

    val repository by lazy {
        ComicRepository(
            context = this,
            comicDao = database.comicDao(),
            chapterDao = database.chapterDao(),
            cbzParser = cbzParser,
            apiService = apiService
        )
    }
}