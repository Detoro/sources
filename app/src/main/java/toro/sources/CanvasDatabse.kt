package toro.sources

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import toro.sources.DataModels.Comic
import toro.sources.DataModels.Chapter

@Database(
    entities = [Comic::class, Chapter::class],
    version = 2,
    exportSchema = false
)
abstract class CanvasDatabase : RoomDatabase() {

    abstract fun comicDao(): ComicDao
    abstract fun chapterDao(): ChapterDao

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
                    .fallbackToDestructiveMigration(false)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}