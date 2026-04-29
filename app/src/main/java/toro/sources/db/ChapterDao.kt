package toro.sources.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import toro.sources.DataModels.Chapter

@Dao
interface ChapterDao {

    @Query("SELECT * FROM chapters WHERE comicId = :comicId ORDER BY chapterNumber ASC")
    fun getChaptersForComic(comicId: String): Flow<List<Chapter>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun getChapterById(chapterId: String): Chapter

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertChapters(chapters: List<Chapter>)

    @Query("UPDATE chapters SET lastReadPageIndex = :pageIndex WHERE id = :chapterId")
    suspend fun updateReadingProgress(chapterId: String, pageIndex: Int)

    @Query("UPDATE chapters SET isDownloaded = :isDownloaded WHERE id = :chapterId")
    suspend fun updateDownloadState(chapterId: String, isDownloaded: Boolean)

    @Query("DELETE FROM chapters WHERE comicId = :comicId")
    suspend fun deleteChaptersByComicId(comicId: String)
}