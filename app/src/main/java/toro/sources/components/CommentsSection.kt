package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import models.CommentLocation
import models.ShareType
import models.SharedContent
import toro.sources.models.Comment
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.viewmodel.SessionViewModel

@Composable
fun CommentsSection(
    communityViewModel: CommunityViewModel,
    sessionViewModel: SessionViewModel,
    onViewAllClick: () -> Unit = {},
    onMakeFirstComment: () -> Unit = {},
    onCommentClick: (Comment) -> Unit = {},
) {
    val comments by communityViewModel.chapterComments.collectAsState()
    val topThree = comments.sortedByDescending { it.likesCount }.take(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (comments.isNotEmpty()) {
                TextButton(onClick = onViewAllClick) {
                    Text("View all ${comments.size}")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (topThree.isEmpty()) {
            Text(
                text = "No comments yet. Be the first to comment!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable {
                    onMakeFirstComment()
                }
            )
        } else {
            topThree.forEach { comment ->
                CommentItem(
                    comment = comment,
                    onCommentClick = { onCommentClick(it) },
                    onReplyClick = { onCommentClick(it) },
                    onLikeClick = { communityViewModel.likeComment(it.id, CommentLocation.ON_CHAPTER) },
                    onShareClick = { 
                        sessionViewModel.setSharedContent(
                            SharedContent(
                                id = it.id,
                                type = ShareType.COMMENT,
                                title = "Comment by ${it.authorName}",
                                previewText = it.content.take(50)
                            )
                        )
                        sessionViewModel.showShareDialog(true)
                    },
                    sessionViewModel = sessionViewModel
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}