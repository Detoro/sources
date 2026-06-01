package toro.sources.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.Screen
import toro.sources.components.CommentItem
import com.toro.models.ShareType
import toro.sources.components.SmartInput
import com.toro.models.Comment
import com.toro.models.CommentLocation
import com.toro.models.SharedContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentThreadPage(
    viewModel: AppViewModel,
    commentLocation: CommentLocation,
    targetId: String,
    commentId: String,
    onBackClick: () -> Unit
) {
    val comments by when (commentLocation) {
        CommentLocation.ON_CHAPTER -> viewModel.chapterComments
        CommentLocation.ON_POST -> viewModel.postComments
    }.collectAsState()
    val mainComment = remember(comments, commentId) { comments.find { it.id == commentId } }
    val replies = remember(comments, commentId) { comments.filter { it.parentId == commentId } }
    
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    var initialText by remember { mutableStateOf("") }

    LaunchedEffect(targetId, commentLocation) {
        when (commentLocation) {
            CommentLocation.ON_CHAPTER -> viewModel.getChapterComments(targetId)
            CommentLocation.ON_POST -> viewModel.getPostComments(targetId)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Thread", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                val sharedContent by viewModel.sharedContent.collectAsState()
                SmartInput(
                    onSend = { _, text, mentions, _, _ ->
                        val targetParentId = replyingTo?.id ?: commentId
                        when (commentLocation) {
                            CommentLocation.ON_CHAPTER -> viewModel.addChapterComment(
                                targetId, 
                                text, 
                                mentions, 
                                targetParentId,
                                sharedId = sharedContent?.id,
                                sharedType = sharedContent?.type
                            )
                            CommentLocation.ON_POST -> viewModel.addPostComment(
                                targetId, 
                                text, 
                                mentions, 
                                targetParentId,
                                sharedId = sharedContent?.id,
                                sharedType = sharedContent?.type
                            )
                        }

                        replyingTo = null
                        initialText = ""
                    },
                    initialText = initialText,
                    placeholder = "Write a reply...",
                    viewModel = viewModel
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (mainComment != null) {
                item {
                    CommentItem(
                        comment = mainComment,
                        isReply = true,
                        isThreadHeader = true,
                        onReplyClick = {},
                        onLikeClick = { viewModel.likeComment(it.id, commentLocation) },
                        onAuthorClick = { userId ->
                            viewModel.handleNavigation(Screen.Profile.createRoute(userId))
                        },
                        onShareClick = {
                            viewModel.setSharedContent(
                                SharedContent(
                                    id = it.id,
                                    type = ShareType.COMMENT,
                                    title = "Comment by ${it.authorName}",
                                    previewText = it.content.take(50)
                                )
                            )
                            viewModel.showShareDialog(true)
                        },
                        viewModel = viewModel
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Replies",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            if (replies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No replies yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(replies) { reply ->
                    CommentItem(
                        comment = reply,
                        onReplyClick = {
                            "@${it.authorName} "
                        },
                        isReply = true,
                        onLikeClick = { viewModel.likeComment(it.id, commentLocation) },
                        onCommentClick = { /* Already in thread. Don't want to do anything */ },
                        onAuthorClick = { userId ->
                            viewModel.handleNavigation(Screen.Profile.createRoute(userId))
                        },
                        onShareClick = {
                            viewModel.setSharedContent(
                                SharedContent(
                                    id = it.id,
                                    type = ShareType.COMMENT,
                                    title = "Comment by ${it.authorName}",
                                    previewText = it.content.take(50)
                                )
                            )
                            viewModel.showShareDialog(true)
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}