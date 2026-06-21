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

    override fun onNewToken(token: String) {
        super.onNewToken(token)
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
            "DELIVERY_RECEIPT" -> {
                val msgId = data["messageId"] ?: remoteMessage.notification?.body ?: ""
                if (msgId.isNotEmpty()) handleDeliveryReceipt(msgId)
                return
            }
            "MESSAGE_DELETED" -> {
                val msgId = data["messageId"] ?: remoteMessage.notification?.body ?: ""
                if (msgId.isNotEmpty()) handleMessageDelete(msgId)
                return
            }
            "MESSAGE_EDITED" -> {
                val msgId = data["messageId"] ?: data["id"] ?: ""
                val newContent = data["content"] ?: remoteMessage.notification?.body ?: ""
                if (msgId.isNotEmpty() && newContent.isNotEmpty()) handleMessageEdit(msgId, newContent)
                return
            }
            "READ_RECEIPT" -> {
                val msgId = data["messageId"] ?: ""
                if (msgId.isNotEmpty()) handleReadReceipt(msgId)
                return
            }
        }

        val title = remoteMessage.notification?.title ?: data["title"] ?: "New Notification"
        val message = remoteMessage.notification?.body ?: data["message"] ?: ""
        val targetId = data["id"] ?: data["conversationId"] ?: data["commentId"] ?: ""

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

        scope.launch {
            NotificationEventBus.postNotification(notification)

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
                db.conversationDao().updateMessageDeliveryStatus(messageId, true)
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
                db.conversationDao().updateMessageReadStatus(messageId, true)
                Log.i("FCM", "Successfully updated read status for message $messageId via remote sync")
            } catch (e: Exception) {
                Log.e("FCM", "Error updating read status", e)
            }
        }
    }

    private fun handleMessageDelete(messageId: String) {
        scope.launch {
            try {
                val db = CanvasDatabase.getDatabase(applicationContext)
                db.conversationDao().deleteMessageById(messageId)
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
                db.conversationDao().updateMessageContent(messageId, newContent)
                Log.i("FCM", "Successfully updated content for message $messageId via remote sync")
            } catch (e: Exception) {
                Log.e("FCM", "Error editing message content", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}