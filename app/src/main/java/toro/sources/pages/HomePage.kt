package toro.sources.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.toro.models.*
import toro.sources.viewmodel.NotificationsViewModel
import toro.sources.Screen
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.components.*
import toro.sources.viewmodel.ComicsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel = hiltViewModel(),
    onComicClick: (Comic) -> Unit,
    onAccountClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val localCatalog by comicsViewModel.localLibrary.collectAsState()
    val onlineCatalog by comicsViewModel.onlineLibrary.collectAsState()
    val trending by comicsViewModel.trending.collectAsState()
    val recentlyRead by comicsViewModel.recentlyReadComics.collectAsState()
    
    val communityViewModel: CommunityViewModel = hiltViewModel()
    val communityState by communityViewModel.communityState.collectAsState()
    val communityPosts = communityState.posts
    
    val followedAuthors by comicsViewModel.subscribedAuthors.collectAsState()
    val followedAuthorIds = followedAuthors.map { it.id }.toSet()
    val me by sessionViewModel.userProfile.collectAsState()

    val isLoading = communityState.isLoading
    
    val notificationsViewModel: NotificationsViewModel = hiltViewModel()
    val notifications by notificationsViewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            comicsViewModel.importLocalComic(
                title = "Imported Comic",
                author = "Unknown",
                description = "Imported from device",
                uri = it
            )
        }
    }

    LaunchedEffect(Unit) {
        comicsViewModel.fetchTrendingAndRecommendations()
        communityViewModel.getCommunityPosts()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
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
                    DefaultAvatar(
                        avatarUrl = me?.avatarUrl,
                        size = 32,
                        modifier = Modifier
                            .background(Color.Gray.copy(alpha = 0.2f), CircleShape)
                            .clickable { onAccountClick() }
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                ),
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { paddingValues ->
        if (localCatalog.isEmpty() && onlineCatalog.isEmpty()) {
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (isLoading) {
                    item { BillboardCarouselShimmer() }
                    item { ComicCarouselShimmer(title = "Continue Reading") }
                } else {
                    item {
                        BillboardCarousel(
                            comics = trending,
                            onComicClick = onComicClick
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

                    if (localCatalog.isNotEmpty()) {
                        item {
                            ComicCarousel(
                                title = "From Your Device",
                                comics = localCatalog,
                                comicsViewModel = comicsViewModel,
                                sessionViewModel = sessionViewModel,
                                onComicClick = onComicClick
                            )
                        }
                    }

                    val followedPosts =
                        communityPosts.filter { followedAuthorIds.contains(it.authorId) }
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
                                    communityViewModel = communityViewModel,
                                    sessionViewModel = sessionViewModel,
                                    post = post,
                                    onCommentClick = { onNotificationsClick() },
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
                            SectionHeader(
                                title = "Announcements",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        items(announcements.take(2)) { post ->
                            PostCard(
                                communityViewModel = communityViewModel,
                                sessionViewModel = sessionViewModel,
                                post = post,
                                onCommentClick = { onNotificationsClick() },
                                onAuthorClick = { userId ->
                                    sessionViewModel.handleNavigation(Screen.Profile.createRoute(userId))
                                },
                                onShareClick = {
                                    sessionViewModel.setSharedContent(
                                        SharedContent(
                                            id = it.id,
                                            type = ShareType.POST,
                                            title = it.title ?: "Announcement",
                                            previewText = it.content.take(50)
                                        )
                                    )
                                    sessionViewModel.showShareDialog(true)
                                },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                    item {
                        ComicCarousel(
                            title = "For You",
                            comics = onlineCatalog,
                            comicsViewModel = comicsViewModel,
                            sessionViewModel = sessionViewModel,
                            onComicClick = onComicClick
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}