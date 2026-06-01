package toro.sources.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import toro.sources.network.RetrofitClient
import com.toro.models.FcmTokenRequest
import com.toro.models.Notification
import com.toro.models.NotificationType
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
            } catch (e: Exception) {
                Log.e("Bad token", "${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i("FCM", "Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "New Notification"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: ""
        val data = remoteMessage.data
        
        Log.i("FCM", "Title: $title, Message: $message, Data: $data")

        val helper = NotificationHelper(applicationContext)
        helper.showNotification(title, message, data)

        val notification = Notification(
            id = UUID.randomUUID().toString(),
            userId = "",
            type = NotificationType.fromString(title) ?: NotificationType.SYSTEM,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedId = data["conversationId"] ?: data["postId"] ?: data["requestId"]
        )
        scope.launch {
            NotificationEventBus.postNotification(notification)
        }
    }
}