package toro.sources.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import toro.sources.AppViewModel
import toro.sources.DataModels.Post

@Composable
fun PostCard(
    viewModel: AppViewModel,
    post: Post,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.authorName.first().toString(), fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "2 hours ago", color = Color.Gray, fontSize = 12.sp) // Mocked time
                }
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.Yellow)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                text = "Discussion Topic",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = post.content,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Tags
            Row(modifier = Modifier.padding(vertical = 12.dp)) {
                TagChip("#graphic novel")
                Spacer(modifier = Modifier.width(8.dp))
                TagChip("#discussion")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LikeButton(
                    likeCount = post.likesCount,
                    isPostLiked = true,
                    onLikePost = {
                        viewModel.likePost(post.id)
                    }
                )
                CommentButton(
                    onCommentOnPost = onCommentClick
                )
                BookmarkButton(
                    isPostBookmarked = true,
                    onBookmarkPost = {
                        viewModel.bookmarkPost(post.id)
                    }
                )
            }
        }
    }
}