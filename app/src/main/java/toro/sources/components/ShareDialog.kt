package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.Screen
import toro.sources.dataModels.ShareType
import toro.sources.dataModels.SharedContent

@Composable
fun ShareDialog(
    viewModel: AppViewModel,
    sharedId: String,
    sharedType: ShareType,
    sharedTitle: String,
    sharedPreview: String,
    onDismiss: () -> Unit
) {
    var showChatPicker by remember { mutableStateOf(false) }
    val inbox by viewModel.inbox.collectAsState()

    if (showChatPicker) {
        AlertDialog(
            onDismissRequest = { showChatPicker = false },
            title = { Text("Select Chat") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(inbox) { conversation ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSharedContent(
                                        SharedContent(
                                            sharedId,
                                            sharedType,
                                            sharedTitle,
                                            sharedPreview
                                        )
                                    )
                                    viewModel.handleNavigation(Screen.Chat.createRoute(conversation.conversationId))
                                    onDismiss()
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DefaultAvatar(modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(conversation.otherUserName)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Share to...") },
            text = {
                Column {
                    ShareOption(
                        icon = Icons.Default.Chat,
                        label = "Chat",
                        onClick = {
                            viewModel.getInbox()
                            showChatPicker = true
                        }
                    )
                    ShareOption(
                        icon = Icons.Default.PostAdd,
                        label = "Post",
                        onClick = {
                            viewModel.setSharedContent(
                                SharedContent(
                                    sharedId,
                                    sharedType,
                                    sharedTitle,
                                    sharedPreview
                                )
                            )
                            viewModel.handleNavigation(Screen.Post.route)
                            onDismiss()
                        }
                    )
                    ShareOption(
                        icon = Icons.Default.Comment,
                        label = "Comment",
                        onClick = {
                            viewModel.setSharedContent(
                                SharedContent(
                                    sharedId,
                                    sharedType,
                                    sharedTitle,
                                    sharedPreview
                                )
                            )
                            viewModel.handleNavigation(Screen.Engagement.route)
                            onDismiss()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ShareOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
