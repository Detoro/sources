package toro.sources.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
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
import models.MessageSummary
import toro.sources.models.Conversation
import toro.sources.utils.formatRelativeTimestamp
import toro.sources.viewmodel.ChatViewModel
import toro.sources.viewmodel.SessionViewModel

enum class ChatPreviewStatus { NONE, SENT, READ }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveChatsList(
    chatViewModel: ChatViewModel,
    sessionViewModel: SessionViewModel,
    onChatClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    lastMessageStatus: (conversationId: String) -> ChatPreviewStatus = { ChatPreviewStatus.NONE }
) {
    val activeChats by chatViewModel.filteredInbox.collectAsState()
    val me by sessionViewModel.userProfile.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(activeChats, key = { it.conversationId }) { chat ->
            key(chat.conversationId) {
                var removed by remember { mutableStateOf(false) }
                if (!removed) {
                    ChatInboxRow(
                        chat = chat,
                        status = lastMessageStatus(chat.conversationId),
                        myUserId = me?.id,
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
    chat: Conversation,
    status: ChatPreviewStatus,
    myUserId: String?,
    onChatClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val isUnread = chat.unreadCount > 0
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
                        text = chat.otherUser.username,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    ChatPreviewLine(
                        lastMessage = chat.lastMessage,
                        isUnread = isUnread,
                        status = status,
                        myUserId = myUserId
                    )
                },
                trailingContent = {
                    ChatRowTrailing(
                        timestamp = chat.timestamp,
                        unreadCount = chat.unreadCount
                    )
                },
                leadingContent = {
                    Box(
                        modifier = Modifier.clickable { onProfileClick(chat.otherUser.userId) }
                    ) {
                        UnreadRail(visible = isUnread)
                        DefaultAvatar(
                            avatarUrl = chat.otherUser.avatarUrl,
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
                    .clickable { onChatClick(chat.conversationId) }
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChatPreviewLine(
    lastMessage: MessageSummary?,
    isUnread: Boolean,
    status: ChatPreviewStatus,
    myUserId: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val isFromMe = lastMessage?.senderId == myUserId
        if (isFromMe && status != ChatPreviewStatus.NONE) {
            ReadReceiptIcon(status = status)
        }
        Text(
            text = buildString {
                if (isFromMe) append("You: ")
                append(lastMessage?.content.orEmpty())
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
private fun ChatRowTrailing(timestamp: Long, unreadCount: Int) {
    val isUnread = unreadCount > 0
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
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
        if (unreadCount > 0) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(text = unreadCount.toString())
            }
        }
    }
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