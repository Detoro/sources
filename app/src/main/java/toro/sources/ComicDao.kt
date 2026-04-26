package toro.sources

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import toro.sources.DataModels.Comic

@Dao
interface ComicDao {
    @Query("SELECT * FROM comics")
    fun getAllComics(): Flow<List<Comic>>


    @Query("SELECT * FROM comics WHERE id = :comicId LIMIT 1")
    suspend fun getComicByIdSync(comicId: String): Comic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComics(comics: List<Comic>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComic(comic: Comic)

    @Delete
    suspend fun deleteComic(comic: Comic)
}