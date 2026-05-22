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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.R
import toro.sources.components.ComicRow
import toro.sources.dataModels.Comic

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReadingList(
    viewModel: AppViewModel,
    onComicClick: (Comic) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val subscribed by viewModel.subscribedComics.collectAsState()
    val recentlyRead by viewModel.recentlyReadComics.collectAsState()

    val tabs = listOf("Recents", "Subscribed")

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inbox)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { }) {
                Icon(Icons.Filled.Add, contentDescription = "Send Friend Request")
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

            // The Content
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedTabIndex == 0) {
                    items(recentlyRead) { comic ->
                        ComicRow(comic, viewModel, onComicClick)
                        HorizontalDivider()
                    }
                } else {
                    items(subscribed) { comic ->
                        ComicRow(comic, viewModel, onComicClick)
                        HorizontalDivider()
                    }
                }

                if (recentlyRead.isEmpty() && subscribed.isEmpty()) {
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
