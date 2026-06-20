package toro.sources.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import toro.sources.Screen
import toro.sources.AppViewModel
import toro.sources.convertTimestamp
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
    var isExpanded by remember { mutableStateOf(false) }

    val backgroundColor = if (isThreadHeader) MaterialTheme.colorScheme.surfaceContainerLow else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .then(if (!isThreadHeader) Modifier.clickable { onCommentClick(comment) } else Modifier)
            .padding(vertical = 12.dp, horizontal = if (isThreadHeader) 16.dp else 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DefaultAvatar(
                avatarUrl = comment.authorAvatarUrl,
                size = if (isThreadHeader) 44 else 36,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAuthorClick(comment.authorId) }
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
                                color = MaterialTheme.colorScheme.onSurface,
                                style = if (isThreadHeader) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                                modifier = Modifier.clickable { onAuthorClick(comment.authorId) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = convertTimestamp(comment.timestamp),
                                style = MaterialTheme.typography.labelMedium,
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
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showMenu = false
                                    onShareClick(comment)
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            DropdownMenuItem(
                                text = { Text("Report", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false
                                    viewModel?.handleNavigation(Screen.Report.createRoute("COMMENT", comment.id))
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val sharedId = comment.sharedId
                val sharedType = comment.sharedType

                if (sharedId != null && sharedType != null) {
                    SharedContentPlaceholder(
                        type = sharedType,
                        title = "Shared ${sharedType.name.lowercase()}",
                        previewText = "Tap to view details",
                        imageUrl = comment.authorAvatarUrl,
                        modifier = Modifier.padding(bottom = 8.dp),
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
                }

                AnimatedContent(
                    targetState = showComment,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "SpoilerToggle"
                ) { isVisible ->
                    if (!isVisible) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showComment = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Spoiler Content • Tap to reveal",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .animateContentSize()
                        ) {
                            SpoilerText(
                                text = comment.content,
                                isSpoiler = comment.isSpoiler,
                                style = if (isThreadHeader) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis,
                                onClick = {
                                    if (comment.content.length > 200) isExpanded = !isExpanded
                                }
                            )
                            if (comment.content.length > 200) {
                                Text(
                                    text = if (isExpanded) "Show Less" else "Read More",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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