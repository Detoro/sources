package toro.sources.network

import android.annotation.SuppressLint
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import toro.sources.PreferenceManager
import toro.sources.BuildConfig
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val networkJson = Json { ignoreUnknownKeys = true }
    @SuppressLint("StaticFieldLeak")
    lateinit var preferenceManager: PreferenceManager

    fun initialize(manager: PreferenceManager) {
        preferenceManager = manager
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        preferenceManager.getTokenSync()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(okHttpClient)
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val comicApiService: ComicApiService by lazy {
        retrofit.create(ComicApiService::class.java)
    }
}