package toro.sources.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import toro.sources.viewmodel.ChatViewModel
import java.util.concurrent.TimeUnit

enum class ChatPreviewStatus { NONE, SENT, READ }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveChatsList(
    chatViewModel: ChatViewModel,
    onChatClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    isUnread: (conversationId: String) -> Boolean = { false },
    lastMessageStatus: (conversationId: String) -> ChatPreviewStatus = { ChatPreviewStatus.NONE },
    isLastMessageFromMe: (conversationId: String) -> Boolean = { false }
) {
    val activeChats by chatViewModel.filteredInbox.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(activeChats, key = { it.conversationId }) { chat ->
            key(chat.conversationId) {
                var removed by remember { mutableStateOf(false) }
                if (!removed) {
                    ChatInboxRow(
                        conversationId = chat.conversationId,
                        username = chat.otherUserName,
                        otherUserId = chat.otherUserId,
                        avatarUrl = chat.otherUserAvatarUrl,
                        lastMessage = chat.lastMessage,
                        timestamp = chat.timestamp,
                        isUnread = isUnread(chat.conversationId),
                        status = lastMessageStatus(chat.conversationId),
                        isFromMe = isLastMessageFromMe(chat.conversationId),
                        onChatClick = onChatClick,
                        onProfileClick = onProfileClick,
                        onArchive = {
                            removed = true
                            onArchive(chat.conversationId)
                        },
                        onDelete = {
                            removed = true
                            onDelete(chat.conversationId)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInboxRow(
    conversationId: String,
    username: String,
    otherUserId: String,
    avatarUrl: String?,
    lastMessage: String?,
    timestamp: Long,
    isUnread: Boolean,
    status: ChatPreviewStatus,
    isFromMe: Boolean,
    onChatClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> onArchive()
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            SwipeToDismissBoxValue.Settled -> {}
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection) },
        content = {
            ListItem(
                headlineContent = {
                    Text(
                        text = username,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    ChatPreviewLine(
                        lastMessage = lastMessage,
                        isUnread = isUnread,
                        status = status,
                        isFromMe = isFromMe
                    )
                },
                trailingContent = {
                    ChatRowTrailing(timestamp = timestamp, isUnread = isUnread)
                },
                leadingContent = {
                    Box(
                        modifier = Modifier.clickable { onProfileClick(otherUserId) }
                    ) {
                        UnreadRail(visible = isUnread)
                        DefaultAvatar(
                            avatarUrl = avatarUrl,
                            modifier = Modifier
                                .padding(start = if (isUnread) 6.dp else 0.dp)
                                .clip(CircleShape)
                        )
                    }
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChatClick(conversationId) }
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChatPreviewLine(
    lastMessage: String?,
    isUnread: Boolean,
    status: ChatPreviewStatus,
    isFromMe: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isFromMe && status != ChatPreviewStatus.NONE) {
            ReadReceiptIcon(status = status)
        }
        Text(
            text = buildString {
                if (isFromMe) append("You: ")
                append(lastMessage.orEmpty())
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
            color = if (isUnread) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun ReadReceiptIcon(status: ChatPreviewStatus) {
    when (status) {
        ChatPreviewStatus.SENT -> Icon(
            imageVector = Icons.Filled.Done,
            contentDescription = "Sent",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        ChatPreviewStatus.READ -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Read",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
        )
        ChatPreviewStatus.NONE -> Unit
    }
}

@Composable
private fun ChatRowTrailing(timestamp: Long, isUnread: Boolean) {
    Text(
        text = formatRelativeTimestamp(timestamp),
        style = MaterialTheme.typography.labelSmall,
        color = if (isUnread) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal
    )
}

@Composable
private fun UnreadRail(visible: Boolean) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .size(width = 3.dp, height = 40.dp)
            .background(MaterialTheme.colorScheme.primary)
    )
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue?) {
    val (color, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Filled.Archive,
            Alignment.CenterStart
        )
        SwipeToDismissBoxValue.EndToStart -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            Icons.Filled.Delete,
            Alignment.CenterEnd
        )
        else -> Triple(Color.Transparent, null, Alignment.Center)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> "Archive conversation"
                    SwipeToDismissBoxValue.EndToStart -> "Delete conversation"
                    else -> null
                },
                tint = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}

private fun formatRelativeTimestamp(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis
    if (diff < 0) return ""

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days == 1L -> "Yesterday"
        days < 7 -> java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
            .format(java.util.Date(timestampMillis))
        else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            .format(java.util.Date(timestampMillis))
    }
}