package toro.sources.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import toro.sources.AppViewModel
import toro.sources.components.*
import com.toro.models.ShareType
import com.toro.models.Chapter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewPage(
    viewModel: AppViewModel,
    onBackClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onChapterClick: (Chapter) -> Unit
) {
    val comic by viewModel.currentComic.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val userRating by viewModel.userRating.collectAsState()
    
    var showActionSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(comic?.id) {
        comic?.let { comic ->
            viewModel.getChaptersForComic(comic)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    comic?.let {
                        IconButton(onClick = {
                            showInfoSheet = true
                            viewModel.getUserWorks(comic!!.authorId)
                        }) {
                            Icon(Icons.Outlined.Info, contentDescription = "Actions")
                        }
                        IconButton(onClick = { showActionSheet = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Actions")
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

            if (showActionSheet) {
                ComicActionBottomSheet(
                    comic = safeComic,
                    onDismiss = { showActionSheet = false },
                    onShare = { showShareDialog = true },
                    onRemove = {
                        viewModel.removeComicFromLibrary(safeComic.id, onRemoved = onBackClick)
                    }
                )
            }

            if (showInfoSheet) {
                ComicInfoBottomSheet(
                    comic = safeComic,
                    viewModel,
                    onDismiss = { showInfoSheet = false }
                )
            }

            if (showShareDialog) {
                ShareDialog(
                    viewModel = viewModel,
                    sharedId = safeComic.id,
                    sharedType = ShareType.COMIC,
                    sharedTitle = safeComic.title,
                    sharedPreview = safeComic.description.take(50),
                    onDismiss = { showShareDialog = false }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = safeComic.coverImageUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = safeComic.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        TextButton(
                            onClick = onAuthorClick
                        ) {
                            Text(
                                text = safeComic.authorName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Rating Section
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InteractiveRatingStars(
                                initialRating = userRating,
                                onRatingSelected = { rating ->
                                    viewModel.rateComic(safeComic.id, rating)
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", userRating),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            SubscribeButton(
                                isComicSubscribed = safeComic.isSubscribed,
                                isLocalSideload = safeComic.isLocalSideload,
                                onSubscribeToComic = {
                                    viewModel.toggleComicSubscription(safeComic.id)
                                },
                                onSubscribeToAuthor = {
                                    viewModel.subscribeToAuthor(safeComic.authorId)
                                }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Synopsis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )
                    Text(
                        text = safeComic.description,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
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
                        onClick = {
                            onChapterClick(chapter)
                            viewModel.markChapterAsRead(safeComic.id, chapter.id)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}