package toro.sources.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import toro.sources.models.ChatMessage
import toro.sources.models.Comic
import toro.sources.models.Chapter
import toro.sources.models.Comment
import toro.sources.models.Conversation
import toro.sources.models.ConversationUiState
import toro.sources.models.Post
import toro.sources.models.Notification
import toro.sources.models.UserProfile
import toro.sources.PreferenceManager

@Database(
    entities = [
        Comic::class,
        Chapter::class,
        Conversation::class,
        ChatMessage::class,
        Notification::class,
        Comment::class,
        Post::class,
        UserProfile::class,
        ConversationUiState::class
    ],
    version = 31,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CanvasDatabase : RoomDatabase() {

    abstract fun comicDao(): ComicDao
    abstract fun chapterDao(): ChapterDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun commentDao(): CommentDao
    abstract fun postDao(): PostDao
    abstract fun authorDao(): AuthorDao
    abstract fun conversationUiStateDao(): ConversationUiStateDao

    companion object {
        @Volatile
        private var INSTANCE: CanvasDatabase? = null

        fun getDatabase(context: Context): CanvasDatabase {
            INSTANCE?.let { return it }

            synchronized(this) {
                INSTANCE?.let { return it }

                val prefs = PreferenceManager(context)
                val userId = prefs.getUserDataSync().userId

                val dbName = if (userId != null) {
                    "graphic_novel_database_$userId"
                } else {
                    "graphic_novel_database_guest"
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CanvasDatabase::class.java,
                    dbName
                )
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                return instance
            }
        }

        fun resetDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        fun deleteDatabase(context: Context, userId: String?) {
            synchronized(this) {
                resetDatabase()
                val dbName = if (userId != null) {
                    "graphic_novel_database_$userId"
                } else {
                    "graphic_novel_database_guest"
                }
                context.deleteDatabase(dbName)
            }
        }
    }
}