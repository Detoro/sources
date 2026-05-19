package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import toro.sources.convertTimestamp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import toro.sources.dataModels.Comment

@Composable
fun CommentItem(
    comment: Comment,
    isThreadHeader: Boolean = false,
    onReplyClick: (Comment) -> Unit = {},
    onLikeClick: (Comment) -> Unit = {},
    onCommentClick: (Comment) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isThreadHeader) Modifier.clickable { onCommentClick(comment) } else Modifier)
            .padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            DefaultAvatar(modifier = Modifier.size(if (isThreadHeader) 44.dp else 36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.authorName,
                        fontWeight = FontWeight.Bold,
                        style = if (isThreadHeader) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = convertTimestamp(comment.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = comment.content,
                    style = if (isThreadHeader) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )

                CommentActions(
                    comment = comment,
                    onReplyClick = { onReplyClick(comment) },
                    onLikeClick = { onLikeClick(comment) }
                )
            }
        }
    }
}