package toro.sources.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.Screen
import com.toro.models.Post
import com.toro.models.ShareType
import toro.sources.convertTimestamp

@Composable
fun PostCard(
    viewModel: AppViewModel,
    post: Post,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (String) -> Unit = {},
    onShareClick: (Post) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                DefaultAvatar(
                    avatarUrl = post.authorAvatarUrl,
                    size = 40,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onAuthorClick(post.authorId) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAuthorClick(post.authorId) }
                ) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = convertTimestamp(post.timestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                onShareClick(post)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        DropdownMenuItem(
                            text = { Text("Report", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                viewModel.handleNavigation(Screen.Report.createRoute("POST", post.id))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val sharedId = post.sharedId
            val sharedType = post.sharedType
            if (sharedId != null && sharedType != null) {
                SharedContentPlaceholder(
                    type = sharedType,
                    title = post.title ?: "Shared ${sharedType.name.lowercase()}",
                    previewText = "Tap to view details",
                    imageUrl = post.authorAvatarUrl,
                    modifier = Modifier.padding(bottom = 16.dp),
                    onClick = {
                        when (sharedType) {
                            ShareType.COMIC -> viewModel.handleNavigation(Screen.Overview.createRoute(sharedId))
                            ShareType.POST -> viewModel.handleNavigation(Screen.PostComments.createRoute(sharedId))
                            ShareType.COMMENT -> viewModel.handleNavigation(Screen.PostComments.createRoute(sharedId))
                            ShareType.USER -> viewModel.handleNavigation(Screen.Profile.createRoute(sharedId))
                        }
                    }
                )
            }

            var isExpanded by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                if (!post.title.isNullOrBlank()) {
                    SpoilerText(
                        text = post.title!!,
                        isSpoiler = post.isSpoiler,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(bottom = 8.dp),
                        onClick = { onCommentClick() }
                    )
                }

                SpoilerText(
                    text = post.content,
                    isSpoiler = post.isSpoiler,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    onClick = {
                        if (post.content.length > 200) isExpanded = !isExpanded
                        else onCommentClick()
                    }
                )
                if (post.content.length > 200) {
                    Text(
                        text = if (isExpanded) "Show Less" else "Read More",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { isExpanded = !isExpanded }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(bottom = 8.dp))

            // Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LikeButton(
                    likeCount = post.likesCount,
                    isPostLiked = post.isLiked,
                    onLikePost = { viewModel.likePost(post.id) }
                )
                CommentButton(
                    onCommentOnPost = onCommentClick
                )
                BookmarkButton(
                    isPostBookmarked = post.isBookmarked,
                    onBookmarkPost = { viewModel.bookmarkPost(post.id) }
                )
            }
        }
    }
}