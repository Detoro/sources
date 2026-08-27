package toro.sources.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import models.CommentLocation
import models.ShareType
import models.SharedContent
import toro.sources.R
import toro.sources.Screen
import toro.sources.components.CommentItem
import toro.sources.components.PostCard
import toro.sources.components.shimmerEffect
import toro.sources.models.Comic
import toro.sources.models.Comment
import toro.sources.models.Post
import toro.sources.models.authorName
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.utils.formatRelativeTimestamp
import toro.sources.utils.getOptimizedUrl

@Composable
fun ActivityPage(
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    communityViewModel: CommunityViewModel,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit,
    onCommentClick: (Comment) -> Unit = {},
) {
    val subscribed by comicsViewModel.subscribedComics.collectAsState()
    val recentlyRead by comicsViewModel.recentlyReadComics.collectAsState()
    val comments = communityViewModel.userComments.collectAsState(emptyList()).value
    val posts = communityViewModel.userPosts.collectAsState(emptyList()).value
    val comicsState by comicsViewModel.comicsUiState.collectAsState()

    ActivityContent(
        subscribedComics = subscribed,
        recentlyReadComics = recentlyRead,
        comments = comments,
        posts = posts,
        isLoading = comicsState.isLoading,
        onComicClick = onComicClick,
        onAddComic = onAddComic,
        onCommentClick = onCommentClick,
        sessionViewModel = sessionViewModel,
        comicsViewModel = comicsViewModel,
        communityViewModel = communityViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityContent(
    subscribedComics: List<Comic>,
    recentlyReadComics: List<Comic>,
    comments: List<Comment>,
    posts: List<Post>,
    isLoading: Boolean,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit,
    onCommentClick: (Comment) -> Unit = {},
    sessionViewModel: SessionViewModel,
    comicsViewModel: ComicsViewModel,
    communityViewModel: CommunityViewModel
) {
    val tabs = listOf(
        TabItem("Recents", Icons.Default.History),
        TabItem("Subscribed", Icons.Default.Bookmark),
        TabItem("Comments", Icons.AutoMirrored.Filled.Comment),
        TabItem("Posts", Icons.Default.PostAdd)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    var recentSort by remember { mutableStateOf("Read") }
    var subscribeSort by remember { mutableStateOf("Subbed") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.activity_list),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                actions = {
                    IconButton(onClick = { sessionViewModel.handleNavigation(Screen.Engagement.route) }) {
                        Icon(Icons.Default.DynamicFeed, contentDescription = "Social Feed")
                    }
                },
                windowInsets = WindowInsets(0.dp),
            )
        },
        floatingActionButton = {
            if (pagerState.currentPage < 2) {
                ExtendedFloatingActionButton(
                    onClick = onAddComic,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text("Add Series") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = pagerState.currentPage == index
                    Tab(
                        selected = selected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    tab.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentCount = when (pagerState.currentPage) {
                    0 -> recentlyReadComics.size
                    1 -> subscribedComics.size
                    2 -> comments.size
                    3 -> posts.size
                    else -> 0
                }

                Text(
                    text = "$currentCount ${when (pagerState.currentPage) { 2 -> "Comments" 3 -> "Posts" else -> "Series" } }",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (pagerState.currentPage < 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val options = if (pagerState.currentPage == 0) listOf("Read", "Updated") else listOf("Subbed", "Updated")
                        val current = if (pagerState.currentPage == 0) recentSort else subscribeSort

                        options.forEach { option ->
                            FilterChip(
                                selected = current == option,
                                onClick = { 
                                    if (pagerState.currentPage == 0) recentSort = option else subscribeSort = option 
                                },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = null,
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (isLoading) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        items(5) {
                            ActivityShimmerItem()
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                } else {
                    when (page) {
                        0, 1 -> {
                            val baseList = if (page == 0) recentlyReadComics else subscribedComics
                            val sortedList = remember(baseList, recentSort, subscribeSort) {
                                when (if (page == 0) recentSort else subscribeSort) {
                                    "Read" -> baseList.sortedByDescending { it.lastReadTimestamp }
                                    "Updated" -> baseList.sortedByDescending { it.lastUpdateTimestamp }
                                    "Subbed" -> baseList.sortedByDescending { it.subscribeTimestamp }
                                    else -> baseList
                                }
                            }

                            if (baseList.isEmpty()) {
                                EmptyActivityState(
                                    icon = if (page == 0) Icons.Default.History else Icons.Default.BookmarkBorder,
                                    message = if (page == 0) "No reading history yet." else "You haven't subscribed to any series.",
                                    actionText = "Start Exploring",
                                    onAction = { sessionViewModel.handleNavigation(Screen.Search.route) }
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(sortedList, key = { it.id }) { comic ->
                                        ActivityComicItem(comic, onComicClick)
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (comments.isEmpty()) {
                                EmptyActivityState(
                                    icon = Icons.Default.ChatBubbleOutline,
                                    message = "No comments found.",
                                    actionText = "Join the Conversation",
                                    onAction = { sessionViewModel.handleNavigation(Screen.Engagement.route) }
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(comments) { comment ->
                                        CommentItem(
                                            comment = comment,
                                            onCommentClick = { onCommentClick(comment) },
                                            onAuthorClick = { sessionViewModel.handleNavigation(Screen.Profile.createRoute(it)) },
                                            onLikeClick = { communityViewModel.likeComment(it.id, CommentLocation.ON_POST) },
                                            sessionViewModel = sessionViewModel,
                                            comicsViewModel = comicsViewModel
                                        )
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (posts.isEmpty()) {
                                EmptyActivityState(
                                    icon = Icons.Default.PostAdd,
                                    message = "No posts found.",
                                    actionText = "Start a community",
                                    onAction = { sessionViewModel.handleNavigation(Screen.Engagement.route) }
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(posts) { post ->
                                        PostCard(
                                            communityViewModel = communityViewModel,
                                            sessionViewModel = sessionViewModel,
                                            post = post,
                                            onCommentClick = {},
                                            comicsViewModel = comicsViewModel,
                                            onAuthorClick = { sessionViewModel.handleNavigation(Screen.Profile.createRoute(it)) },
                                            onShareClick = {
                                                sessionViewModel.setSharedContent(
                                                    SharedContent(
                                                        id = it.id,
                                                        type = ShareType.POST,
                                                        title = it.title
                                                            ?: "Post by ${it.authorName}",
                                                        previewText = it.content.take(50)
                                                    )
                                                )
                                                sessionViewModel.showShareDialog(true)
                                            }
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
}

@Composable
private fun ActivityComicItem(
    comic: Comic,
    onClick: (Comic) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(comic) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = comic.coverImageUrl.getOptimizedUrl(width = 300),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${comic.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Update, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (comic.lastReadTimestamp > 0) 
                            "Read ${formatRelativeTimestamp(comic.lastReadTimestamp)}"
                            else "Updated ${formatRelativeTimestamp(comic.lastUpdateTimestamp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (comic.chapterCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 
                            if (comic.chapterCount > 0) comic.readChapterCount.toFloat() / comic.chapterCount.toFloat() 
                            else 0f 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    Text(
                        text = "${comic.readChapterCount}/${comic.chapterCount} chapters",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            IconButton(onClick = { onClick(comic) }) {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyActivityState(
    icon: ImageVector,
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(50)
        ) {
            Text(actionText)
        }
    }
}

@Composable
private fun ActivityShimmerItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 110.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.width(120.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(80.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        }
    }
}

private data class TabItem(val title: String, val icon: ImageVector)