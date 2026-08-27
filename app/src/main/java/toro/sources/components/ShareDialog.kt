package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import models.ShareType
import models.SharedContent
import toro.sources.viewmodel.SessionViewModel
import toro.sources.Screen
import toro.sources.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDialog(
    sessionViewModel: SessionViewModel,
    sharedId: String,
    sharedType: ShareType,
    sharedTitle: String,
    sharedPreview: String,
    sharedTargetId: String? = null,
    chatViewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    var showChatPicker by remember { mutableStateOf(false) }
    val inbox by chatViewModel.filteredInbox.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (showChatPicker) "Send to..." else "Share to...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (showChatPicker) {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(inbox) { conversation ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    sessionViewModel.setSharedContent(
                                        SharedContent(
                                            sharedId,
                                            sharedType,
                                            sharedTitle,
                                            sharedPreview,
                                            sharedTargetId
                                        )
                                    )
                                    sessionViewModel.handleNavigation(Screen.Chat.createRoute(conversation.conversationId))
                                    onDismiss()
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DefaultAvatar(
                                avatarUrl = conversation.otherUser.avatarUrl,
                                size = 48
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = conversation.otherUser.username,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    ShareOption(
                        icon = Icons.Default.ChatBubble,
                        label = "Direct Message",
                        description = "Send directly to a friend",
                        onClick = {
                            chatViewModel.getInbox()
                            showChatPicker = true
                        }
                    )
                    ShareOption(
                        icon = Icons.Default.PostAdd,
                        label = "Community Post",
                        description = "Share to the main feed",
                        onClick = {
                            sessionViewModel.setSharedContent(
                                SharedContent(sharedId, sharedType, sharedTitle, sharedPreview, sharedTargetId)
                            )
                            sessionViewModel.handleNavigation(Screen.Post.route)
                            onDismiss()
                        }
                    )
                    ShareOption(
                        icon = Icons.AutoMirrored.Filled.Comment,
                        label = "Engagement Thread",
                        description = "Drop into a comment section",
                        onClick = {
                            sessionViewModel.setSharedContent(
                                SharedContent(sharedId, sharedType, sharedTitle, sharedPreview, sharedTargetId)
                            )
                            sessionViewModel.handleNavigation(Screen.Engagement.route)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareOption(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}