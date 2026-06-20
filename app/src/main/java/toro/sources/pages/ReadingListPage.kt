package toro.sources.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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

@Composable
fun ReadingListPage(
    viewModel: AppViewModel,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit
) {
    val subscribed by viewModel.subscribedComics.collectAsState()
    val recentlyRead by viewModel.recentlyReadComics.collectAsState()

    ReadingListContent(
        subscribedComics = subscribed,
        recentlyReadComics = recentlyRead,
        onComicClick = onComicClick,
        onAddComic = onAddComic,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReadingListContent(
    subscribedComics: List<Comic>,
    recentlyReadComics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    onAddComic: () -> Unit,
    viewModel: AppViewModel? = null
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var dropDownSelection by remember { mutableStateOf("Recently Updated") }
    val tabs = listOf("Recents", "Subscribed")
    val currentList = if (selectedTabIndex == 0) recentlyReadComics else subscribedComics

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
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTabIndex == 0) recentlyReadComics.size.toString() else subscribedComics.size.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 17.sp
                )

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
                                currentList.sortedByDescending { it.lastReadTimestamp }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recently Updated") },
                            onClick = {
                                expanded = false
                                dropDownSelection = "Recently Updated"
                                currentList.sortedByDescending { it.lastUpdateTimestamp }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recently Subscribed") },
                            onClick = {
                                expanded = false
                                dropDownSelection = "Recently Subscribed"
                                currentList.sortedByDescending { it.subscribeTimestamp }
                            }
                        )
                    }
                }
            }
            HorizontalDivider(Modifier.alpha(0.4f))

            // The Content
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {

                items(currentList) { comic ->
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

                if (recentlyReadComics.isEmpty() && subscribedComics.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Your reading list is empty. Start exploring!",
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