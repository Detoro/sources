package toro.sources.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import toro.sources.AppViewModel
import toro.sources.R
import toro.sources.components.ComicRow
import com.toro.models.*
import kotlinx.coroutines.launch

@Composable
fun ActivityPage(
    viewModel: AppViewModel,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit
) {
    val subscribed by viewModel.subscribedComics.collectAsState()
    val recentlyRead by viewModel.recentlyReadComics.collectAsState()
    val authors by viewModel.subscribedAuthors.collectAsState()
    val comments by viewModel.chapterComments.collectAsState()

    ActivityContent(
        subscribedComics = subscribed,
        recentlyReadComics = recentlyRead,
        comments = comments,
        subscribedAuthors = authors,
        onComicClick = onComicClick,
        onAddComic = onAddComic,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActivityContent(
    subscribedComics: List<Comic>,
    recentlyReadComics: List<Comic>,
    comments: List<Comment>,
    subscribedAuthors: List<UserProfile>,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit,
    viewModel: AppViewModel? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var dropDownSelection by remember { mutableStateOf("Recently Updated") }
    val tabs = listOf("Recents", "Subscribed", "Comments", "Authors")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reading_list)) },
                windowInsets = WindowInsets(top = 3.dp)
            ) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddComic() }) {
                Icon(Icons.Filled.Add, contentDescription = "Find a comic to subscribe to")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // The Tabs
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
                        text = { Text(title) }
                    )
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentSize = when (pagerState.currentPage) {
                    0 -> recentlyReadComics.size
                    1 -> subscribedComics.size
                    2 -> comments.size
                    3 -> subscribedAuthors.size
                    else -> 0
                }

                Text(
                    text = currentSize.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                if (pagerState.currentPage < 2) {
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
                // The Content
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
                                if (viewModel != null) {
                                    ComicRow(comic, viewModel, onComicClick)
                                    HorizontalDivider()
                                } else {
                                    ListItem(
                                        headlineContent = { Text(comic.title) },
                                        supportingContent = { Text(comic.writtenBy) }
                                    )
                                    HorizontalDivider()
                                }
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
                        3 -> {
                            items(subscribedAuthors) { author ->
                                ListItem(
                                    headlineContent = { Text(author.username) },
                                    supportingContent = { Text(author.bio ?: "No bio available") }
                                )
                                HorizontalDivider()
                            }
                            if (subscribedAuthors.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxSize()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No authors followed.",
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