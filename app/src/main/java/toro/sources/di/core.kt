package toro.sources.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import toro.sources.CbzParser
import toro.sources.session.SessionManager
import toro.sources.network.ChatConnectionManager
import toro.sources.db.ComicRepository
import toro.sources.media.MediaUploadManager
import toro.sources.PreferenceManager
import toro.sources.network.AuthApi
import toro.sources.network.ComicApiService
import toro.sources.network.RetrofitClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager {
        val manager = PreferenceManager(context)
        RetrofitClient.initialize(manager)
        return manager
    }

    @Provides
    @Singleton
    fun provideSessionManager(
        preferenceManager: PreferenceManager,
        @ApplicationContext context: Context
    ): SessionManager {
        return SessionManager(preferenceManager, context)
    }

    @Provides
    @Singleton
    fun provideMediaUploadManager(@ApplicationContext context: Context): MediaUploadManager {
        return MediaUploadManager(context)
    }

    @Provides
    @Singleton
    fun provideChatConnectionManager(
        @ApplicationScope applicationScope: CoroutineScope
    ): ChatConnectionManager {
        return ChatConnectionManager(
            socketFactory = RetrofitClient::createChatWebSocket,
            coroutineScope = applicationScope
        )
    }

    @Provides
    @Singleton
    fun provideCbzParser(@ApplicationContext context: Context): CbzParser {
        return CbzParser(context)
    }

    @Provides
    @Singleton
    fun provideComicRepository(
        @ApplicationContext context: Context,
        cbzParser: CbzParser,
        apiService: ComicApiService
    ): ComicRepository {
        return ComicRepository(
            context = context,
            cbzParser = cbzParser,
            apiService = apiService
        )
    }

    @Provides
    @Singleton
    fun provideAuthApiService(): AuthApi {
        return RetrofitClient.authApi
    }

    @Provides
    @Singleton
    fun provideComicApiService(): ComicApiService {
        return RetrofitClient.comicApiService
    }
}