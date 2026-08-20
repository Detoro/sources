package toro.sources.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.Screen
import toro.sources.components.CommentItem
import com.toro.models.ShareType
import toro.sources.components.SmartInput
import com.toro.models.Comment
import com.toro.models.CommentLocation
import com.toro.models.SharedContent
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsPage(
    communityViewModel: CommunityViewModel,
    sessionViewModel: SessionViewModel,
    comicsViewModel: ComicsViewModel,
    commentLocation: CommentLocation,
    targetId: String,
    onCommentClick: (Comment) -> Unit = {}
) {
    val comments by when (commentLocation) {
        CommentLocation.ON_CHAPTER -> communityViewModel.chapterComments
        CommentLocation.ON_POST -> communityViewModel.postComments
    }.collectAsState()
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    LaunchedEffect(targetId, commentLocation) {
        when (commentLocation) {
            CommentLocation.ON_CHAPTER -> communityViewModel.getChapterComments(targetId)
            CommentLocation.ON_POST -> communityViewModel.getPostComments(targetId)
        }
    }

    val topLevelComments = remember(comments) {
        comments.filter { it.parentId.isNullOrEmpty() }
    }

    var initialText by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Comments", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets(top = 3.dp)
            )
        },
        bottomBar = {
            Column {
                if (replyingTo != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Replying to ${replyingTo?.authorName}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { 
                                replyingTo = null 
                                initialText = ""
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        }
                    }
                }
                SmartInput(
                    onSend = { _, text, mentions, _, _, isSpoiler ->
                        when (commentLocation) {
                            CommentLocation.ON_CHAPTER -> communityViewModel.addChapterComment(
                                targetId, 
                                text, 
                                isSpoiler,
                                mentions, 
                                replyingTo?.id,
                            )
                            CommentLocation.ON_POST -> communityViewModel.addPostComment(
                                targetId, 
                                text, 
                                isSpoiler,
                                mentions, 
                                replyingTo?.id,
                            )
                        }
                        replyingTo = null
                        initialText = ""
                    },
                    initialText = initialText,
                    placeholder = if (replyingTo == null) "Add a comment..." else "Write a reply...",
                    sessionViewModel = sessionViewModel
                )
            }
        }
    ) { paddingValues ->
        if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No comments yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(topLevelComments) { comment ->
                    CommentItem(
                        comment = comment,
                        onReplyClick = {onCommentClick(it)},
                        onLikeClick = { communityViewModel.likeComment(it.id, commentLocation) },
                        onCommentClick = { onCommentClick(it) },
                        onAuthorClick = { userId ->
                            sessionViewModel.handleNavigation(Screen.Profile.createRoute(userId))
                        },
                        onShareClick = { c ->
                            sessionViewModel.setSharedContent(
                                SharedContent(
                                    id = c.id,
                                    type = ShareType.COMMENT,
                                    title = "Comment by ${c.authorName}",
                                    previewText = c.content.take(50),
                                    targetId = targetId
                                )
                            )
                            sessionViewModel.showShareDialog(true)
                        },
                        onDeleteClick = {
                            communityViewModel.deleteComment(
                                comment.commentLocation,
                                targetId,
                                comment.id
                            )
                        },
                        sessionViewModel = sessionViewModel,
                        comicsViewModel = comicsViewModel
                    )
                }
            }
        }
    }
}