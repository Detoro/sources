package toro.sources.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import toro.sources.Screen
import toro.sources.components.AuthorsRow
import toro.sources.components.PostCard
import com.toro.models.ShareType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.toro.models.SharedContent

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
    var selectedTab by remember { mutableIntStateOf(0) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val tabs = listOf("Trending", "Friends")

            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (tabs[selectedTab]) {
                    "Trending" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {

                            items(filteredPosts) { post ->
                                PostCard(
                                    viewModel = viewModel,
                                    post = post,
                                    onCommentClick = { onCommentClick(post.id) },
                                    onAuthorClick = { userId ->
                                        viewModel.handleNavigation(Screen.Profile.createRoute(userId))
                                    },
                                    onShareClick = {
                                        viewModel.setSharedContent(
                                            SharedContent(
                                                id = it.id,
                                                type = ShareType.POST,
                                                title = it.title ?: "Post by ${it.authorName}",
                                                previewText = it.content.take(50)
                                            )
                                        )
                                        viewModel.showShareDialog(true)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                    "Friends" -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AuthorsRow(
                                viewModel = viewModel,
                                onAddAuthorClick = onAddAuthorClick
                            )

                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredPosts.take(5)) { post ->
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    PostCard(
                                        viewModel = viewModel,
                                        post = post,
                                        onCommentClick = { onCommentClick(post.id) },
                                        onAuthorClick = { userId ->
                                            viewModel.handleNavigation(Screen.Profile.createRoute(userId))
                                        },
                                        onShareClick = {
                                            viewModel.setSharedContent(
                                                SharedContent(
                                                    id = it.id,
                                                    type = ShareType.POST,
                                                    title = it.title ?: "Post by ${it.authorName}",
                                                    previewText = it.content.take(50)
                                                )
                                            )
                                            viewModel.showShareDialog(true)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}