package toro.sources.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.toro.models.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthors(authors: List<UserProfile>)

    @Query("SELECT * FROM authors")
    fun getAllAuthors(): Flow<List<UserProfile>>

    @Query("DELETE FROM authors")
    suspend fun deleteAllAuthors()
}