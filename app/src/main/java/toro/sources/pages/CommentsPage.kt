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
import toro.sources.components.CommentItem
import toro.sources.components.SmartInput
import toro.sources.dataModels.Comment
import toro.sources.dataModels.CommentLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsPage(
    viewModel: AppViewModel,
    commentLocation: CommentLocation,
    targetId: String,
    onBackClick: () -> Unit,
    onCommentClick: (Comment) -> Unit = {}
) {
    val comments by when (commentLocation) {
        CommentLocation.ON_CHAPTER -> viewModel.chapterComments
        CommentLocation.ON_POST -> viewModel.postComments
    }.collectAsState()
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    LaunchedEffect(targetId, commentLocation) {
        when (commentLocation) {
            CommentLocation.ON_CHAPTER -> viewModel.getChapterComments(targetId)
            CommentLocation.ON_POST -> viewModel.getPostComments(targetId)
        }
    }

    // Process comments into top-level only (we show replies in thread page now)
    val topLevelComments = remember(comments) {
        comments.filter { it.parentId == "" }
    }

    var initialText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comments", style = MaterialTheme.typography.titleLarge) },
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
                SmartInput(
                    onSend = { _, text, mentions, _, _ ->
                        when (commentLocation) {
                            CommentLocation.ON_CHAPTER -> viewModel.addChapterComment(targetId, text, mentions, replyingTo?.id)
                            CommentLocation.ON_POST -> viewModel.addPostComment(targetId, text, mentions, replyingTo?.id)
                        }
                        replyingTo = null
                        initialText = ""
                    },
                    initialText = initialText,
                    placeholder = if (replyingTo == null) "Add a comment..." else "Write a reply...",
                    viewModel = viewModel
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
                        onLikeClick = { viewModel.likeComment(it.id, commentLocation) },
                        onCommentClick = { onCommentClick(it) }
                    )
                }
            }
        }
    }
}
