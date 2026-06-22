package toro.sources.network

import android.annotation.SuppressLint
import retrofit2.Retrofit
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import toro.sources.PreferenceManager
import toro.sources.BuildConfig
import com.toro.models.AuthResponse
import com.toro.models.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import android.util.Log

interface AuthApi {
    @POST("api/auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<AuthResponse>
}

object RetrofitClient {
    private val networkJson = Json { ignoreUnknownKeys = true }

    @SuppressLint("StaticFieldLeak")
    lateinit var preferenceManager: PreferenceManager

    fun initialize(manager: PreferenceManager) {
        preferenceManager = manager
    }

    private val authRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_API_URL)
            .addConverterFactory(networkJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    private val authApi by lazy { authRetrofit.create(AuthApi::class.java) }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        preferenceManager.getAccessTokenSync()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            val refreshToken = preferenceManager.getRefreshTokenSync() ?: return null

            synchronized(this) {
                val currentAccessToken = preferenceManager.getAccessTokenSync()
                val failedRequestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

                // Check if another thread already refreshed it while we were waiting
                if (currentAccessToken != null && currentAccessToken != failedRequestToken) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentAccessToken")
                        .build()
                }

                val refreshResponse = try {
                    authApi.refreshToken(RefreshTokenRequest(refreshToken)).execute()
                } catch (e: Exception) {
                    Log.e("authApi error", "${e.message}")
                    return null
                }

                if (refreshResponse.isSuccessful) {
                    val newTokens = refreshResponse.body()
                    if (newTokens != null) {
                        runBlocking {
                            preferenceManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                        }

                        return response.request.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()
                    }
                } else {
                    runBlocking {
                        preferenceManager.clearTokens()
                    }
                    return null
                }
            }
            return null
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
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