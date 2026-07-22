package toro.sources.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.Screen
import toro.sources.components.AuthorsRow
import toro.sources.components.PostCard
import toro.sources.components.PostCardShimmer
import com.toro.models.ShareType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import com.toro.models.SharedContent
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.launch

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
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedAuthorIds by viewModel.selectedAuthorIds.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        topBar = {
            LargeTopAppBar(
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
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                ),
                windowInsets = WindowInsets(top = 3.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val tabs = listOf("Trending", "Friends")
            val pagerState = rememberPagerState(pageCount = { tabs.size })
            val coroutineScope = rememberCoroutineScope()

            SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (isLoading) {
                                items(5) {
                                    PostCardShimmer()
                                }
                            } else {
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
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RectangleShape,
                                        containerColor = Color.Transparent
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            item {
                                AuthorsRow(
                                    viewModel = viewModel,
                                    onAddAuthorClick = onAddAuthorClick
                                )
                            }
                            
                            items(filteredPosts.take(10)) { post ->
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
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RectangleShape,
                                    containerColor = Color.Transparent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}