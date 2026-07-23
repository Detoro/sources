package toro.sources.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import toro.sources.AppViewModel
import toro.sources.Screen
import com.toro.models.Post

@Composable
fun PostCard(
    viewModel: AppViewModel,
    post: Post,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAuthorClick: (String) -> Unit = {},
    onShareClick: (Post) -> Unit = {},
    showAccentLine: Boolean = false,
    accentColor: Color = Color.LightGray,
    shape: Shape = RectangleShape,
    containerColor: Color = Color.Transparent
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (showAccentLine) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DefaultAvatar(
                        avatarUrl = post.authorAvatarUrl,
                        size = 40,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onAuthorClick(post.authorId) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "2h ago", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    
                    // Follow Button
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Blue
                        ),
                        border = BorderStroke(1.dp, Color.Blue.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false; onShareClick(post) })
                            DropdownMenuItem(text = { Text("Report", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; viewModel.handleNavigation(Screen.Report.createRoute("POST", post.id)) })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; viewModel.deletePost(post.id) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Media
                if (post.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = post.imageUrls.first(),
                        contentDescription = "Post Content",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (post.videoUrls.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Content
                Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    val annotatedContent = buildAnnotatedString {
                        val keywords = listOf("toro", "deto")
                        val parts = post.content.split(" ")
                        parts.forEach { word ->
                            if (keywords.any { word.contains(it, ignoreCase = true) }) {
                                withStyle(SpanStyle(color = Color.Cyan, fontWeight = FontWeight.Bold)) {
                                    append("$word ")
                                }
                            } else {
                                append("$word ")
                            }
                        }
                    }
                    
                    Text(
                        text = annotatedContent,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LikeButton(
                        likeCount = post.likesCount,
                        isLiked = post.isLiked,
                        onLike = { viewModel.likePost(post.id) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    CommentButton(
                        onCommentOnPost = onCommentClick
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    BookmarkButton(
                        isBookmarked = post.isBookmarked,
                        onBookmark = { viewModel.bookmarkPost(post.id) },
                    )
                }
            }
        }
    }
}