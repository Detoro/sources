package toro.sources.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.CommentItem
import toro.sources.components.SmartInput
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsPage(
    viewModel: AppViewModel,
    postId: String,
    onBackClick: () -> Unit
) {
    val comments by viewModel.comments.collectAsState()

    LaunchedEffect(postId) {
        viewModel.getPostComments(postId)
    }

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
            SmartInput(
                onSend = { text, _, mentions, _ ->
                    viewModel.addComment(postId, text, mentions)
                },
                placeholder = "Add a comment...",
                viewModel = viewModel
            )
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
                items(comments) { comment ->
                    CommentItem(comment)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
        }
    }
}