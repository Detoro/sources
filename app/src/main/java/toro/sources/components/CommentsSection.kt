package toro.sources.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.dataModels.Comment
import toro.sources.dataModels.CommentLocation

@Composable
fun CommentsSection(
    viewModel: AppViewModel,
    chapterId: String,
    onViewAllClick: () -> Unit = {},
    onCommentClick: (Comment) -> Unit = {}
) {
    val comments by viewModel.chapterComments.collectAsState()
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            topThree.forEach { comment ->
                CommentItem(
                    comment = comment,
                    onCommentClick = { onCommentClick(it) },
                    onReplyClick = { onCommentClick(it) },
                    onLikeClick = { viewModel.likeComment(it.id, CommentLocation.ON_CHAPTER) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        SmartInput(
            onSend = { _, text, mentions, _, _ ->
                viewModel.addChapterComment(
                    chapterId = chapterId,
                    text,
                    mentions,
                )
            },
            placeholder = "Add a comment...",
            viewModel = viewModel
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}
