package toro.sources.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.*
import com.toro.models.Comic
import toro.sources.Screen
import com.toro.models.ShareType
import com.toro.models.SharedContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    viewModel: AppViewModel,
    onComicClick: (Comic) -> Unit,
    onAccountClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val libraryList by viewModel.myLibrary.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val recentlyRead by viewModel.recentlyReadComics.collectAsState()
    val communityPosts by viewModel.communityPosts.collectAsState()
    val followedAuthors by viewModel.subscribedAuthors.collectAsState()
    val followedAuthorIds = followedAuthors.map { it.id }.toSet()

    val isLoading by viewModel.isLoading.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importLocalComic(
                title = "Imported Comic",
                author = "Unknown",
                description = "Imported from device",
                comicUri = it
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("") },
                actions = {
                    IconButton(onClick = { onNotificationsClick() }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = { filePickerLauncher.launch("application/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Import Comic")
                    }
                    IconButton(onClick = { onAccountClick() }) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
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
        if (libraryList.isEmpty() && catalog.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your library is empty.\nTap + to import a .cbz file!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (isLoading) {
                    item { BillboardCarouselShimmer() }
                    item { ComicCarouselShimmer(title = "Continue Reading") }
                    item { ComicCarouselShimmer(title = "New Releases") }
                    item { ComicCarouselShimmer(title = "Top Stories") }
                } else {
                    item {
                        BillboardCarousel(
                            comics = catalog.take(5),
                            onComicClick = onComicClick,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (recentlyRead.isNotEmpty()) {
                        item {
                            ContinueReadingCarousel(
                                comics = recentlyRead,
                                onComicClick = onComicClick
                            )
                        }
                    }

                    item {
                        ComicCarousel(
                            title = "New Releases",
                            comics = catalog.shuffled().take(6),
                            viewModel = viewModel,
                            onComicClick = onComicClick
                        )
                    }

                    item {
                        ComicCarousel(
                            title = "Top Stories",
                            comics = catalog.shuffled().take(8),
                            viewModel = viewModel,
                            onComicClick = onComicClick
                        )
                    }

                    val followedPosts = communityPosts.filter { followedAuthorIds.contains(it.authorId) }
                    if (followedPosts.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Creator Feed",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        item {
                            val pagerState = rememberPagerState(
                                initialPage = 0,
                                pageCount = { followedPosts.size }
                            )
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                pageSpacing = 16.dp
                            ) { postIndex ->
                                val post = followedPosts[postIndex]
                                PostCard(
                                    viewModel = viewModel,
                                    post = post,
                                    onCommentClick = { onNotificationsClick() },
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
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }

                    val announcements = communityPosts.filter { it.authorId == "toro_creator" }
                    if (announcements.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Announcements", modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        items(announcements.take(2)) { post ->
                            PostCard(
                                viewModel = viewModel,
                                post = post,
                                onCommentClick = { onNotificationsClick() },
                                onAuthorClick = { userId ->
                                    viewModel.handleNavigation(Screen.Profile.createRoute(userId))
                                },
                                onShareClick = {
                                    viewModel.setSharedContent(
                                        SharedContent(
                                            id = it.id,
                                            type = ShareType.POST,
                                            title = it.title ?: "Announcement: ${null}",
                                            previewText = it.content.take(50)
                                        )
                                    )
                                    viewModel.showShareDialog(true)
                                },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                            )
                        }
                    } else {
                        item {
                            ComicCarousel(
                                title = "Announcements",
                                comics = catalog.shuffled().take(4),
                                viewModel = viewModel,
                                onComicClick = onComicClick
                            )
                        }
                    }

                    item {
                        ComicCarousel(
                            title = "For You",
                            comics = catalog.shuffled().take(8),
                            viewModel = viewModel,
                            onComicClick = onComicClick
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}