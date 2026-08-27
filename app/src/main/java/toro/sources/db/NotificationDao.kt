package toro.sources.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import toro.sources.models.Notification

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: Notification)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>

    @Query("UPDATE notifications SET isRead = :read WHERE id = :notificationId")
    suspend fun updateReadStatus(notificationId: String, read: Boolean)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
}