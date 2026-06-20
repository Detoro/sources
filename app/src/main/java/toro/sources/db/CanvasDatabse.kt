package toro.sources.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.toro.models.Comic
import Chapter
import com.toro.models.Conversation
import com.toro.models.ChatMessage
import com.toro.models.Notification

@Database(
    entities = [Comic::class, Chapter::class, Conversation::class, ChatMessage::class, Notification::class],
    version = 19,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CanvasDatabase : RoomDatabase() {

    abstract fun comicDao(): ComicDao
    abstract fun chapterDao(): ChapterDao
    abstract fun conversationDao(): ConversationDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: CanvasDatabase? = null

        fun getDatabase(context: Context): CanvasDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CanvasDatabase::class.java,
                    "graphic_novel_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}