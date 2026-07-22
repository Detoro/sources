package toro.sources.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.toro.models.ChatMessage

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE isDelivered = 0 AND senderId = :userId")
    suspend fun getPendingMessages(userId: String): List<ChatMessage>

    @Query("SELECT MAX(timestamp) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getLastMessageTimestamp(conversationId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("UPDATE chat_messages SET isDelivered = :delivered WHERE id = :messageId")
    suspend fun updateMessageDeliveryStatus(messageId: String, delivered: Boolean)

    @Query("UPDATE chat_messages SET isRead = :read WHERE id = :messageId")
    suspend fun updateMessageReadStatus(messageId: String, read: Boolean)

    @Query("UPDATE chat_messages SET content = :content WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, content: String)
}