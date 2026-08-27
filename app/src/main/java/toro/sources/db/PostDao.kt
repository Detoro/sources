package toro.sources.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import toro.sources.models.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post)

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getUserPosts(): Flow<List<Post>>

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}