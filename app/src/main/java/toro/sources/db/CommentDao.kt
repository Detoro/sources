package toro.sources.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import toro.sources.models.Comment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    @Query("SELECT * FROM comments ORDER BY timestamp DESC")
    fun getUserComments(): Flow<List<Comment>>

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM comments")
    suspend fun deleteAllComments()
}