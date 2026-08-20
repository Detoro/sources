package toro.sources.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import toro.sources.Screen
import toro.sources.utils.convertTimestamp
import toro.sources.viewmodel.NotificationsViewModel
import toro.sources.viewmodel.SessionViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsPage(
    notificationsViewModel: NotificationsViewModel,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val notifications by notificationsViewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { notificationsViewModel.clearNotifications() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
                        }
                    }
                },
                windowInsets = WindowInsets(top = 3.dp)
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No notifications yet", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    val dismissState = rememberSwipeToDismissBoxState()

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        },
                        onDismiss = { notificationsViewModel.deleteNotification(notification.id) }
                    ) {
                        ListItem(
                            headlineContent = { Text(notification.message) },
                            supportingContent = {
                                Text(text = convertTimestamp(notification.timestamp))
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .graphicsLayer(alpha = if (notification.isRead) 0.5f else 1.0f)
                                .clickable {
                                    notificationsViewModel.markNotificationAsRead(notification.id)
                                    sessionViewModel.handleNavigation(Screen.Chat.createRoute(notification.relatedId ?: ""))
                            }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}