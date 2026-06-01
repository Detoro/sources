package toro.sources.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.toro.models.Notification

object NotificationEventBus {
    private val _notifications = MutableSharedFlow<Notification>()
    val notifications = _notifications.asSharedFlow()

    suspend fun postNotification(notification: Notification) {
        _notifications.emit(notification)
    }
}