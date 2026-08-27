package toro.sources.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import models.SearchSource
import models.ShareType
import models.SharedContent
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicSearchBottomSheet(
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    onDismiss: () -> Unit,
) {
    val searchQuery by comicsViewModel.searchQuery.collectAsState()
    val searchResults by comicsViewModel.searchResults.collectAsState()
    comicsViewModel.updateSearchFilter("ALL")
    comicsViewModel.updateSearchSource(SearchSource.ONLINE)

    var isSearchFocused by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = if (isSearchFocused) Modifier.fillMaxHeight(0.95f) else Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .imePadding()
        ) {
            // The Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { comicsViewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .onFocusChanged {
                        isSearchFocused = it.isFocused
                    },
                placeholder = { Text("Search by title or author...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { comicsViewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(searchResults) { comic ->
                    ComicRow(
                        comic = comic,
                        onClick = {
                            sessionViewModel.setSharedContent(
                                SharedContent(
                                    id = comic.id,
                                    type = ShareType.COMIC,
                                    title = comic.title,
                                    previewText = comic.description.take(50)
                                )
                            )
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}