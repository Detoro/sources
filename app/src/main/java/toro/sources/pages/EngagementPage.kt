package toro.sources.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.AuthorsRow
import toro.sources.components.SectionTitle
import toro.sources.components.PostCard
import toro.sources.components.MockEngagementData.mockCommunityPosts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementPage(
    viewModel: AppViewModel,
    onCommentClick: (String) -> Unit,
    onMakePost: () -> Unit
) {
    val posts by viewModel.communityPosts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                actions = {
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { onMakePost() }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "DMs")
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
                AuthorsRow(viewModel)
            }

            item {
                SectionTitle(title = "Featured Discussions", onExploreClick = {})
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(posts) { post ->
                        PostCard(
                            viewModel = viewModel,
                            post = post,
                            onCommentClick = { onCommentClick(post.id) },
                            modifier = Modifier.width(300.dp)
                        )
                    }
                }
            }
        }
    }
}