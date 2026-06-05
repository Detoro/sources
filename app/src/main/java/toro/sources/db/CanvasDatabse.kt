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

@Database(
    entities = [Comic::class, Chapter::class, Conversation::class, ChatMessage::class],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CanvasDatabase : RoomDatabase() {

    abstract fun comicDao(): ComicDao
    abstract fun chapterDao(): ChapterDao
    abstract fun conversationDao(): ConversationDao

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