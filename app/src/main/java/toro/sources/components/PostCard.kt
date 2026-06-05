package toro.sources.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import toro.sources.AppViewModel
import toro.sources.Screen
import com.toro.models.Post
import com.toro.models.ShareType
import toro.sources.convertTimestamp
import androidx.compose.runtime.getValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

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

    Column(
        modifier = modifier
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp)
        .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            DefaultAvatar(
                avatarUrl = post.authorAvatarUrl,
                size = 32,
                modifier = Modifier.clickable { onAuthorClick(post.authorId) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAuthorClick(post.authorId) }
            ) {
                Text(text = post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = convertTimestamp(post.timestamp), color = Color.Gray, fontSize = 12.sp)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
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
                            onShareClick(post)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Not Interested") },
                        onClick = { showMenu = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Shared Content
        val sharedId = post.sharedId
        val sharedType = post.sharedType
        if (sharedId != null && sharedType != null) {
            SharedContentPlaceholder(
                type = sharedType,
                onClick = {
                    when (sharedType) {
                        ShareType.COMIC -> {
                            viewModel.handleNavigation(Screen.Overview.createRoute(sharedId))
                        }
                        ShareType.POST -> {
                            viewModel.handleNavigation(Screen.PostComments.createRoute(sharedId))
                        }
                        ShareType.COMMENT -> {
                            // In this case, we don't know the parent post ID from the sharedId alone easily
                            // unless we fetch it. For now, route to comments if possible.
                            viewModel.handleNavigation(Screen.PostComments.createRoute(sharedId))
                        }
                        ShareType.USER -> {
                            viewModel.handleNavigation(Screen.Profile.createRoute(sharedId))
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Content
        Column(
            modifier = Modifier.clickable {
                onCommentClick()
            }
        ) {
            Text(
                text = post.title ?: "Discussion Topic",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = post.content,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LikeButton(
                likeCount = post.likesCount,
                isPostLiked = post.isLiked,
                onLikePost = {
                    viewModel.likePost(post.id)
                }
            )
            CommentButton(
                onCommentOnPost = onCommentClick
            )
            BookmarkButton(
                isPostBookmarked = post.isBookmarked,
                onBookmarkPost = {
                    viewModel.bookmarkPost(post.id)
                }
            )
        }
    }
}