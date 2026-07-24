package toro.sources.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.toro.models.*
import kotlinx.coroutines.launch
import toro.sources.R
import toro.sources.Screen
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.components.ComicRow
import toro.sources.viewmodel.ComicsViewModel

@Composable
fun ActivityPage(
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    communityViewModel: CommunityViewModel,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit,
) {
    val subscribed by comicsViewModel.subscribedComics.collectAsState()
    val recentlyRead by comicsViewModel.recentlyReadComics.collectAsState()
    val comments by communityViewModel.postComments.collectAsState()

    ActivityContent(
        subscribedComics = subscribed,
        recentlyReadComics = recentlyRead,
        comments = comments,
        onComicClick = onComicClick,
        onAddComic = onAddComic,
        sessionViewModel = sessionViewModel
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActivityContent(
    subscribedComics: List<Comic>,
    recentlyReadComics: List<Comic>,
    comments: List<Comment>,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit,
    sessionViewModel: SessionViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var dropDownSelection by remember { mutableStateOf("Recently Updated") }
    val tabs = listOf("Recents", "Subscribed", "Comments")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_list)) },
                windowInsets = WindowInsets(top = 3.dp),
                actions = {
                    IconButton(onClick = { sessionViewModel.handleNavigation(Screen.Engagement.route) }) {
                        Icon(Icons.Default.DynamicFeed, contentDescription = "Social Feed")
                    }
                }
            )},
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddComic() }) {
                Icon(Icons.Filled.Add, contentDescription = "Find a comic")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                val count = when (index) {
                                    0 -> recentlyReadComics.size
                                    1 -> subscribedComics.size
                                    2 -> comments.size
                                    else -> 0
                                }
                                if (count > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Badge(containerColor = Color.Blue, contentColor = Color.DarkGray) { Text(count.toString()) }
                                }
                            }
                        }
                    )
                }
            }

            if (pagerState.currentPage < 2) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Box {
                        TextButton(
                            onClick = { expanded = true }
                        ) {
                            Text(dropDownSelection)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Recently Read") },
                                onClick = {
                                    expanded = false
                                    dropDownSelection = "Recently Read"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Recently Updated") },
                                onClick = {
                                    expanded = false
                                    dropDownSelection = "Recently Updated"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Recently Subscribed") },
                                onClick = {
                                    expanded = false
                                    dropDownSelection = "Recently Subscribed"
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.alpha(0.4f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (page) {
                        0, 1 -> {
                            val baseList = if (page == 0) recentlyReadComics else subscribedComics
                            val sortedList = when (dropDownSelection) {
                                "Recently Read" -> baseList.sortedByDescending { it.lastReadTimestamp }
                                "Recently Updated" -> baseList.sortedByDescending { it.lastUpdateTimestamp }
                                "Recently Subscribed" -> baseList.sortedByDescending { it.subscribeTimestamp }
                                else -> baseList
                            }

                            items(sortedList) { comic ->
                                ComicRow(comic, onComicClick)
                                HorizontalDivider()
                            }

                            if (baseList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (page == 0) "Your recently read list is empty. Start exploring!" else "Your subscribed list is empty. Start exploring!",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        2 -> {
                            items(comments) { comment ->
                                ListItem(
                                    headlineContent = { Text(comment.content, maxLines = 2) },
                                    supportingContent = { Text("By ${comment.authorName}") }
                                )
                                HorizontalDivider()
                            }
                            if (comments.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No comments found.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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