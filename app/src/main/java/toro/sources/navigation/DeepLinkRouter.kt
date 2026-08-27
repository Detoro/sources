package toro.sources.navigation

import android.content.Intent
import models.NotificationType
import toro.sources.Screen
import javax.inject.Inject

class DeepLinkRouter @Inject constructor(
    private val navigationState: NavigationState
) {
    fun handleIntent(intent: Intent?) {
        intent?.let {
            val routingType = it.getStringExtra("type")
            val id = it.getStringExtra("id")

            when (routingType) {
                NotificationType.CHAT.name -> {
                    if (id != null) {
                        navigationState.handleNavigation(Screen.Chat.createRoute(id))
                    }
                }
                NotificationType.LIKE.name -> {
                    navigationState.handleNavigation(Screen.Engagement.route)
                }
                NotificationType.COMMENT.name, NotificationType.FOLLOW.name -> {
                    if (id != null) {
                        navigationState.handleNavigation(Screen.PostComments.createRoute(id))
                    } else {
                        navigationState.handleNavigation(Screen.Notifications.route)
                    }
                }
                NotificationType.FRIEND_REQUEST.name -> {
                    navigationState.handleNavigation(Screen.FriendRequest.route)
                }
            }
            it.removeExtra("type")
            it.removeExtra("id")
        }
    }
}