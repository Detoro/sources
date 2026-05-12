package toro.sources.network

import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import toro.sources.DataModels.TokenManager


const val url: String = "http://192.168.1.141:8080/"

object RetrofitClient {
    private val networkJson = Json { ignoreUnknownKeys = true }
    public lateinit var tokenManager: TokenManager

    fun initialize(manager: TokenManager) {
        tokenManager = manager
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        tokenManager.getTokenSync()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val comicApiService: ComicApiService by lazy {
        retrofit.create(ComicApiService::class.java)
    }
}