package toro.sources.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import toro.sources.AppViewModel
import toro.sources.DataModels.Chapter
import toro.sources.components.ChapterRow
import toro.sources.components.SubscribeButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewPage(
    viewModel: AppViewModel,
    onBackClick: () -> Unit,
    onChapterClick: (Chapter) -> Unit
) {
    val comic by viewModel.currentComic.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()

    LaunchedEffect(comic?.id) {
        comic?.let { comic ->
            viewModel.getChaptersForComic(comic)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(comic?.title ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    comic?.let { safeComic ->
                        IconButton(onClick = {
                            viewModel.removeComicFromLibrary(safeComic.id, onRemoved = onBackClick)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove from Library")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (comic == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val safeComic = comic!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        AsyncImage(
                            model = safeComic.coverImageUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(120.dp)
                                .height(180.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = safeComic.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "By ${safeComic.author}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            SubscribeButton(
                                isComicSubscribed = isSubscribed,
                                isLocalSideload = safeComic.isLocalSideload,
                                onSubscribeToComic = {
                                    viewModel.toggleComicSubscription(safeComic.id)
                                },
                                onSubscribeToAuthor = {
                                    viewModel.subscribeToAuthor(safeComic.author)
                                }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = safeComic.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()
                }

                item {
                    Text(
                        text = "Chapters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                items(chapters) { chapter ->
                    ChapterRow(
                        chapter = chapter,
                        onClick = { onChapterClick(chapter) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}