package toro.sources

import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType


const val url: String = "http://192.168.1.141:8080/"

object RetrofitClient {
    private val networkJson = Json { ignoreUnknownKeys = true }
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(url)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val comicApiService: ComicApiService by lazy {
        retrofit.create(ComicApiService::class.java)
    }
}