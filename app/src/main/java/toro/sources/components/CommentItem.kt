package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import toro.sources.Screen
import toro.sources.AppViewModel
import toro.sources.convertTimestamp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toro.models.Comment
import com.toro.models.ShareType

@Composable
fun CommentItem(
    comment: Comment,
    isThreadHeader: Boolean = false,
    isReply: Boolean = false,
    onReplyClick: (Comment) -> Unit = {},
    onLikeClick: (Comment) -> Unit = {},
    onCommentClick: (Comment) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onShareClick: (Comment) -> Unit = {},
    viewModel: AppViewModel? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showComment by remember { mutableStateOf(!comment.isSpoiler) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isThreadHeader) Modifier.clickable { onCommentClick(comment) } else Modifier)
            .padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DefaultAvatar(
                avatarUrl = comment.authorAvatarUrl,
                size = if (isThreadHeader) 44 else 36,
                modifier = Modifier.clickable { onAuthorClick(comment.authorId) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = comment.authorName,
                                fontWeight = FontWeight.Bold,
                                style = if (isThreadHeader) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.clickable { onAuthorClick(comment.authorId) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = convertTimestamp(comment.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report") },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = { 
                                    showMenu = false
                                    onShareClick(comment)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel") },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
                val sharedId = comment.sharedId
                val sharedType = comment.sharedType

                if (sharedId != null && sharedType != null) {
                    SharedContentPlaceholder(
                        type = sharedType,
                        onClick = {
                            if (viewModel != null) {
                                when (sharedType) {
                                    ShareType.COMIC -> viewModel.handleNavigation(Screen.Overview.createRoute(sharedId))
                                    ShareType.POST -> viewModel.handleNavigation(Screen.PostComments.createRoute(sharedId))
                                    ShareType.COMMENT -> viewModel.handleNavigation(Screen.PostComments.createRoute(sharedId))
                                    ShareType.USER -> viewModel.handleNavigation(Screen.Profile.createRoute(sharedId))
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                var isExpanded by remember { mutableStateOf(false) }
                
                Column(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable {
                            if (comment.isSpoiler) {
                                showComment = !showComment
                            } else {
                                isExpanded = !isExpanded
                            }
                        }
                ) {
                    Text(
                        text = if (showComment) comment.content else "This is a spoiler",
                        style = if (isThreadHeader) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (showComment && comment.content.length > 200) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Read More",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                CommentActions(
                    comment = comment,
                    hideReply = isReply,
                    onReplyClick = { onReplyClick(comment) },
                    onLikeClick = { onLikeClick(comment) }
                )
            }
        }
    }
}