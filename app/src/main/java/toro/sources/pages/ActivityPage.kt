package toro.sources.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    onCommentClick: (Comment) -> Unit = {},
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
        onCommentClick = onCommentClick,
        sessionViewModel = sessionViewModel
    )
}

private val sortOptionsByTab = listOf(
    listOf("Recently Read", "Recently Updated"),           // Recents tab
    listOf("Recently Subscribed", "Recently Updated"),     // Subscribed tab
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActivityContent(
    subscribedComics: List<Comic>,
    recentlyReadComics: List<Comic>,
    comments: List<Comment>,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit,
    onCommentClick: (Comment) -> Unit = {},
    sessionViewModel: SessionViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    var dropDownSelection by remember { mutableStateOf(sortOptionsByTab[0].first()) }
    val tabs = listOf("Recents", "Subscribed", "Comments")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (page < sortOptionsByTab.size) {
            dropDownSelection = sortOptionsByTab[page].first()
        }
    }

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
                Icon(Icons.Filled.Add, contentDescription = "Add Comic")
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
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) { Text(count.toString()) }
                                }
                            }
                        }
                    )
                }
            }

            if (pagerState.currentPage < sortOptionsByTab.size) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(dropDownSelection)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            sortOptionsByTab[pagerState.currentPage].forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        expanded = false
                                        dropDownSelection = option
                                    }
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.alpha(0.4f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                                    supportingContent = { Text("By ${comment.authorName}") },
                                    modifier = Modifier.clickable { onCommentClick(comment) }
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