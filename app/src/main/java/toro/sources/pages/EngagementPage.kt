package toro.sources.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import toro.sources.Screen
import toro.sources.components.AuthorsRow
import toro.sources.components.PostCard
import toro.sources.components.PostCardShimmer
import toro.sources.components.DefaultAvatar
import com.toro.models.ShareType
import com.toro.models.SharedContent
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.ComicsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngagementPage(
    communityViewModel: CommunityViewModel,
    sessionViewModel: SessionViewModel,
    comicsViewModel: ComicsViewModel,
    onCommentClick: (String) -> Unit,
    onMakePost: () -> Unit,
    onAddAuthorClick: () -> Unit,
) {
    val communityState by communityViewModel.communityState.collectAsState()
    val posts = communityState.posts
    val selectedAuthorIds by comicsViewModel.selectedAuthorIds.collectAsState()
    val filteredPosts = remember(posts, selectedAuthorIds) {
        if (selectedAuthorIds.isEmpty()) {
            posts
        } else {
            posts.filter { it.authorId in selectedAuthorIds }
        }
    }
    val isLoading = communityState.isLoading
    val me by sessionViewModel.userProfile.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(Unit) {
        communityViewModel.getCommunityPosts()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Community",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onMakePost) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Make Post", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            DefaultAvatar(
                                avatarUrl = me?.avatarUrl,
                                size = 32,
                                modifier = Modifier
                                    .background(Color.Gray.copy(alpha = 0.2f), CircleShape)
                                    .clickable { sessionViewModel.handleNavigation(Screen.Profile.createRoute(me?.id ?: "fallback-123")) }
                            )
                        }
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
                windowInsets = WindowInsets(top = 8.dp)
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
                    comicsViewModel = comicsViewModel,
                    onAddAuthorClick = onAddAuthorClick
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    thickness = 0.5.dp
                )
            }

            if (isLoading) {
                items(5) {
                    PostCardShimmer()
                }
            } else {
                itemsIndexed(filteredPosts) { index, post ->
                    PostCard(
                        communityViewModel = communityViewModel,
                        sessionViewModel = sessionViewModel,
                        comicsViewModel = comicsViewModel,
                        post = post,
                        onCommentClick = { onCommentClick(post.id) },
                        onAuthorClick = { userId ->
                            sessionViewModel.handleNavigation(Screen.Profile.createRoute(userId))
                        },
                        onShareClick = {
                            sessionViewModel.setSharedContent(
                                SharedContent(
                                    id = it.id,
                                    type = ShareType.POST,
                                    title = it.title ?: "Post by ${it.authorName}",
                                    previewText = it.content.take(50)
                                )
                            )
                            sessionViewModel.showShareDialog(true)
                        },
                        showAccentLine = index % 2 != 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        thickness = 0.5.dp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}