package toro.sources

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp
import toro.sources.db.CanvasDatabase
import toro.sources.network.RetrofitClient.comicApiService
import toro.sources.db.ComicRepository


@HiltAndroidApp
class SourcesCanvas : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        initCloudinary()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }

    private fun initCloudinary() {
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUD_NAME,
            "secure" to true
        )
        MediaManager.init(this, config)
    }

    val database: CanvasDatabase
        get() = CanvasDatabase.getDatabase(this)

    val cbzParser by lazy { CbzParser(this) }

    val apiService by lazy { comicApiService }

    val repository: ComicRepository by lazy {
        ComicRepository(
            context = this,
            cbzParser = cbzParser,
            apiService = apiService
        )
    }
}