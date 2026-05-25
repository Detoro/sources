package toro.sources.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.AuthorsRow
import toro.sources.components.SectionTitle
import toro.sources.components.PostCard
import androidx.compose.material3.HorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementPage(
    viewModel: AppViewModel,
    onCommentClick: (String) -> Unit,
    onMakePost: () -> Unit,
    onAddAuthorClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val posts by viewModel.communityPosts.collectAsState()
    val selectedAuthorIds by viewModel.selectedAuthorIds.collectAsState()

    val filteredPosts = remember(posts, selectedAuthorIds) {
        if (selectedAuthorIds.isEmpty()) {
            posts
        } else {
            posts.filter { selectedAuthorIds.contains(it.authorId) }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getCommunityPosts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onMakePost() }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Post")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            item {
                AuthorsRow(
                    viewModel = viewModel,
                    onAddAuthorClick = onAddAuthorClick
                )
            }

            item {
                SectionTitle(title = "Featured Discussions", onExploreClick = {})
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(filteredPosts.take(5)) { post ->
                        PostCard(
                            viewModel = viewModel,
                            post = post,
                            onCommentClick = { onCommentClick(post.id) },
                            modifier = Modifier.width(300.dp)
                        )
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SectionTitle(title = "Community Feed", onExploreClick = {})
            }

            items(filteredPosts) { post ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PostCard(
                        viewModel = viewModel,
                        post = post,
                        onCommentClick = { onCommentClick(post.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
