package toro.sources.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import toro.sources.viewmodel.SessionViewModel
import models.SearchSource
import toro.sources.components.ComicCoverCard
import toro.sources.models.Comic
import toro.sources.models.authorName
import toro.sources.viewmodel.ComicsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    onComicClick: (Comic) -> Unit
) {
    val searchQuery by comicsViewModel.searchQuery.collectAsState()
    val searchResults by comicsViewModel.searchResults.collectAsState()
    val searchSource by comicsViewModel.searchSource.collectAsState()
    val currentFilter by comicsViewModel.searchFilter.collectAsState()

    var isGridView by remember { mutableStateOf(true) }
    val searchFilters = listOf("All", "Comics", "Authors", "Tags")

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Search") },
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
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
                windowInsets = WindowInsets(top = 8.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { comicsViewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                placeholder = { Text("Start Search...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { comicsViewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(4.dp)
            ) {
                SearchSource.entries.forEach { source ->
                    val selected = source == searchSource
                    val backgroundColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(durationMillis = 300),
                        label = "background"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 300),
                        label = "text"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .clickable { comicsViewModel.updateSearchSource(source) }
                            .padding(vertical = 8.dp)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = source.name,
                            color = textColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Filter Tags Row (All, Comics, Authors, Tags)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(searchFilters) { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { comicsViewModel.updateSearchFilter(filter) },
                        label = { Text(filter) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            // The Results Area (Animated Grid/List Toggle)
            AnimatedContent(
                targetState = searchQuery.isBlank() to searchResults.isEmpty(),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SearchResults"
            ) { (isBlank, isEmpty) ->
                when {
                    isBlank -> {
                        EmptySearchState("Find your next adventure.")
                    }
                    isEmpty -> {
                        EmptySearchState("No results found for \"$searchQuery\"")
                    }
                    else -> {
                        AnimatedContent(
                            targetState = isGridView,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "ViewToggle"
                        ) { showGrid ->
                            if (showGrid) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 110.dp),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(searchResults) { comic ->
                                        ComicCoverCard(comic = comic, comicsViewModel = comicsViewModel, sessionViewModel = sessionViewModel)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(searchResults) { comic ->
                                        ComicListCard(comic = comic, onClick = { onComicClick(comic) })
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
fun ComicListCard(comic: Comic, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            AsyncImage(
                model = comic.coverImageUrl,
                contentDescription = "Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(75.dp).fillMaxHeight()
            )
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comic.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptySearchState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}