package toro.sources.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.toro.models.Comic
import com.toro.models.Chapter
import com.toro.models.Conversation
import com.toro.models.ChatMessage
import com.toro.models.Notification
import com.toro.models.Comment
import com.toro.models.Post
import com.toro.models.UserProfile
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
        UserProfile::class
    ],
    version = 29,
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