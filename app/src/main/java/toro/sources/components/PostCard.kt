package toro.sources.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    onShareClick: (Post) -> Unit = {},
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
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
                        DropdownMenuItem(
                            text = { Text("Report", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                viewModel.handleNavigation(Screen.Report.createRoute("POST", post.id))
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.scrim) },
                            onClick = {
                                showMenu = false
                                viewModel.deletePost(post.id)
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
                    imageUrl = post.sharedImageUrl,
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

            post.imageUrls.forEach { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.FillWidth
                )
            }

            post.videoUrls.forEach { videoUrl ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = videoUrl,
                        contentDescription = "Post Video",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
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