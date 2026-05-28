package toro.sources.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.Screen
import toro.sources.dataModels.ShareType

@Composable
fun ChatBubble(
    text: String,
    isFromMe: Boolean,
    isDelivered: Boolean = false,
    showStatus: Boolean = false,
    sharedComicId: String? = null,
    sharedId: String? = null,
    sharedType: ShareType? = null,
    viewModel: AppViewModel? = null
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    val catalog by viewModel?.catalog?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    
    val actualSharedId = sharedId ?: sharedComicId
    val actualSharedType = if (sharedId != null) sharedType else if (sharedComicId != null) ShareType.COMIC else null

    val sharedComic = remember(actualSharedId, actualSharedType, catalog) {
        if (actualSharedType == ShareType.COMIC) catalog.find { it.id == actualSharedId } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromMe) 16.dp else 4.dp,
                            bottomEnd = if (isFromMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    if (sharedComic != null) {
                        SharedComicCard(sharedComic)
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (actualSharedType != null && actualSharedId != null) {
                        SharedContentPlaceholder(actualSharedType)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (text.isNotBlank()) {
                        Text(
                            text = text,
                            color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (actualSharedId != null && actualSharedType != null && viewModel != null) {
                                        when (actualSharedType) {
                                            ShareType.COMIC -> {
                                                if (sharedComic != null) {
                                                    viewModel.setCurrentComic(sharedComic)
                                                    viewModel.handleNavigation(Screen.Overview.route)
                                                }
                                            }
                                            ShareType.POST -> {
                                                viewModel.handleNavigation(Screen.PostComments.createRoute(actualSharedId))
                                            }
                                            ShareType.COMMENT -> {
                                                // Since we don't store targetId in message, we can't route perfectly
                                                // but for now we route to a thread if possible or engagement
                                                viewModel.handleNavigation(Screen.Engagement.route)
                                            }
                                        }
                                    }
                                },
                                onLongClick = { showOptionsMenu = true }
                            ),
                        )
                    }
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showOptionsMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showOptionsMenu = false }
                    )
                }
            }
            if (isFromMe && showStatus) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isDelivered) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = if (isDelivered) "Delivered" else "Sent",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
