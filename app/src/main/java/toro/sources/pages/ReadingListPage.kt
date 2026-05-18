package toro.sources.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.ComicRow
import toro.sources.components.SectionHeader
import toro.sources.dataModels.Comic

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadingList(
    viewModel: AppViewModel,
    onComicClick: (Comic) -> Unit
) {
    val subscribed by viewModel.subscribedComics.collectAsState()
    val recentlyRead by viewModel.recentlyReadComics.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recentlyRead.isNotEmpty()) {
            stickyHeader {
                SectionHeader("Recently Read")
            }
            items(recentlyRead) { comic ->
                ComicRow(comic, viewModel, onComicClick)
            }
        }

        if (subscribed.isNotEmpty()) {
            stickyHeader {
                SectionHeader("Subscribed")
            }
            items(subscribed) { comic ->
                ComicRow(comic, viewModel, onComicClick)
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
