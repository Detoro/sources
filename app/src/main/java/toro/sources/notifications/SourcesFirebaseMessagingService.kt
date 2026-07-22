package toro.sources.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import toro.sources.network.RetrofitClient
import com.toro.models.FcmTokenRequest
import com.toro.models.Notification
import com.toro.models.NotificationType
import toro.sources.db.CanvasDatabase
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SourcesFirebaseMessagingService: FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    @Deprecated("Deprecated in Java")
    override fun onNewToken(token: String) {
        super.onRegistered(token)
        scope.launch {
            try {
                RetrofitClient.comicApiService.registerFcmToken(FcmTokenRequest(token))
                Log.i("FCM", "New token registered successfully")
            } catch (e: Exception) {
                Log.e("FCM", "Failed to register new token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val eventType = data["type"] ?: remoteMessage.notification?.title ?: data["title"] ?: "SYSTEM"

        Log.i("FCM", "Received Event Type: $eventType | Data Payload: $data")

        when (eventType) {
            NotificationType.DELIVERY_RECEIPT.name -> {
                val msgId = data["messageId"] ?: remoteMessage.notification?.body ?: ""
                if (msgId.isNotEmpty()) handleDeliveryReceipt(msgId)
                return
            }
            NotificationType.MESSAGE_DELETED.name -> {
                val msgId = data["messageId"] ?: remoteMessage.notification?.body ?: ""
                if (msgId.isNotEmpty()) handleMessageDelete(msgId)
                return
            }
            NotificationType.MESSAGE_EDITED.name -> {
                val msgId = data["messageId"] ?: data["id"] ?: ""
                val newContent = data["content"] ?: remoteMessage.notification?.body ?: ""
                if (msgId.isNotEmpty() && newContent.isNotEmpty()) handleMessageEdit(msgId, newContent)
                return
            }
            NotificationType.READ_RECEIPT.name -> {
                val msgId = data["messageId"] ?: ""
                if (msgId.isNotEmpty()) handleReadReceipt(msgId)
                return
            }
            NotificationType.NOTIFICATION_READ.name -> {
                val msgId = data["messageId"] ?: ""
                if (msgId.isNotEmpty()) handleNotificationRead(msgId)
                return
            }
            NotificationType.FRIEND_REQUEST.name -> {
                val requestId = data["requestId"] ?: data["id"] ?: ""
                if (requestId.isNotEmpty()) handleFriendRequest(requestId)
                return
            }
        }

        val title = remoteMessage.notification?.title ?: data["title"] ?: "New Notification"
        val message = remoteMessage.notification?.body ?: data["message"] ?: ""
        val targetId = data["id"] ?: data["conversationId"] ?: data["commentId"] ?: data["senderId"] ?: ""

        val helper = NotificationHelper(applicationContext)
        helper.showNotification(title, message, data)

        val notification = Notification(
            id = UUID.randomUUID().toString(),
            userId = "",
            type = NotificationType.fromString(eventType) ?: NotificationType.SYSTEM,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedId = targetId
        )

        val friendRequest = Int.MAX_VALUE

        scope.launch {
            NotificationEventBus.postNotification(notification)
            NotificationEventBus.postFriendRequest(friendRequest)

            if (notification.type == NotificationType.CHAT && targetId.isNotEmpty()) {
                try {
                    RetrofitClient.comicApiService.markMessageAsDelivered(targetId)
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to send delivery receipt for $targetId: ${e.message}")
                }
            }
        }
    }

    private fun handleDeliveryReceipt(messageId: String) {
        scope.launch {
            try {
                val db = CanvasDatabase.getDatabase(applicationContext)
                db.chatMessageDao().updateMessageDeliveryStatus(messageId, true)
                Log.i("FCM", "Successfully updated delivery status for message $messageId")
            } catch (e: Exception) {
                Log.e("FCM", "Error updating delivery status", e)
            }
        }
    }

    private fun handleReadReceipt(messageId: String) {
        scope.launch {
            try {
                val db = CanvasDatabase.getDatabase(applicationContext)
                db.chatMessageDao().updateMessageReadStatus(messageId, true)
                Log.i("FCM", "Successfully updated read status for message $messageId via remote sync")
            } catch (e: Exception) {
                Log.e("FCM", "Error updating read status", e)
            }
        }
    }

    private fun handleNotificationRead(notificationId: String) {
        scope.launch {
            try {
                val db = CanvasDatabase.getDatabase(applicationContext)
                db.notificationDao().updateReadStatus(notificationId, true)
                Log.i("FCM", "Successfully updated read status for notification $notificationId")
            } catch (e: Exception) {
                Log.e("FCM", "Error updating notification read status", e)
            }
        }
    }

    private fun handleMessageDelete(messageId: String) {
        scope.launch {
            try {
                val db = CanvasDatabase.getDatabase(applicationContext)
                db.chatMessageDao().deleteMessageById(messageId)
                Log.i("FCM", "Successfully deleted message $messageId via remote sync")
            } catch (e: Exception) {
                Log.e("FCM", "Error deleting message", e)
            }
        }
    }

    private fun handleMessageEdit(messageId: String, newContent: String) {
        scope.launch {
            try {
                val db = CanvasDatabase.getDatabase(applicationContext)
                db.chatMessageDao().updateMessageContent(messageId, newContent)
                Log.i("FCM", "Successfully updated content for message $messageId via remote sync")
            } catch (e: Exception) {
                Log.e("FCM", "Error editing message content", e)
            }
        }
    }

    private fun handleFriendRequest(requestId: String) {
        scope.launch {
            try {
                Log.i("FCM", "Processed new friend request: $requestId")
            } catch (e: Exception) {
                Log.e("FCM", "Error handling friend request", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}