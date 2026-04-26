package toro.sources

import android.app.Application
import toro.sources.RetrofitClient.comicApiService


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