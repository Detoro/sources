package toro.sources.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.toro.models.ConversationUiState

@Dao
interface ConversationUiStateDao {
    @Query("SELECT * FROM conversation_ui_state WHERE conversationId = :conversationId LIMIT 1")
    fun getUiState(conversationId: String): Flow<ConversationUiState?>

    @Query("SELECT * FROM conversation_ui_state WHERE conversationId = :conversationId LIMIT 1")
    suspend fun getUiStateSync(conversationId: String): ConversationUiState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUiState(state: ConversationUiState)

    @Query("DELETE FROM conversation_ui_state WHERE conversationId = :conversationId")
    suspend fun clearUiState(conversationId: String)
}
