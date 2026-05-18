package toro.sources.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import toro.sources.dataModels.Comic

@Dao
interface ComicDao {
    @Query("SELECT * FROM comics")
    fun getAllComics(): Flow<List<Comic>>


    @Query("SELECT * FROM comics WHERE id = :comicId LIMIT 1")
    suspend fun getComicByIdSync(comicId: String): Comic?

    @Query("SELECT * FROM comics WHERE isSubscribed = 1")
    fun getSubscribedComics(): Flow<List<Comic>>

    @Query("SELECT * FROM comics WHERE lastReadTimestamp > 0 ORDER BY lastReadTimestamp DESC")
    fun getRecentlyReadComics(): Flow<List<Comic>>

    @Query("UPDATE comics SET lastReadTimestamp = :timestamp WHERE id = :comicId")
    suspend fun updateLastReadTimestamp(comicId: String, timestamp: Long)

    @Query("UPDATE comics SET isSubscribed = :isSubscribed WHERE id = :comicId")
    suspend fun updateSubscription(comicId: String, isSubscribed: Boolean)

    @Insert
    suspend fun insertComics(comics: List<Comic>)

    @Insert
    suspend fun insertComic(comic: Comic)

    @Delete
    suspend fun deleteComic(comic: Comic)
}