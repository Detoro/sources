package toro.sources.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.toro.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewPage(
    viewModel: AppViewModel,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onComicClick: (Comic) -> Unit,
    onChapterClick: (Chapter) -> Unit
) {
    val comic by viewModel.currentComic.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val userRating = comic?.rating ?: 0f
    var expanded by remember { mutableStateOf(false) }
    var dropDownSelection by remember { mutableStateOf("Latest First") }

    val sortedChapters by remember {
        derivedStateOf {
            if (dropDownSelection == "Latest First") {
                chapters.asReversed()
            } else {
                chapters
            }
        }
    }

    var showActionSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(comic?.id) {
        comic?.let { comic ->
            viewModel.getChaptersForComic(comic)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    comic?.let { currentComic ->
                        IconButton(
                            onClick = {
                                showInfoSheet = true
                                viewModel.getUserWorks(currentComic.authorId)
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = "Actions")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showActionSheet = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(top = 3.dp)
            )
        }
    ) { paddingValues ->
        if (comic == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val safeComic = comic!!
            val artists = safeComic.artBy


            if (safeComic.artBy.isNotEmpty()) {
                Text(text = "Art by $artists")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp)
                    ) {
                        AsyncImage(
                            model = safeComic.coverImageUrl,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 100f
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = safeComic.title,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Written by ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                val writers = safeComic.authors.filter { it.role == Role.WRITER || it.role.name.lowercase() == "AUTHOR" }
                                writers.forEachIndexed { index, writer ->
                                    TextButton(
                                        onClick = { onAuthorClick(writer.id) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = writer.name + if (index < writers.size - 1) ", " else "",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                InteractiveRatingStars(
                                    initialRating = userRating,
                                    onRatingSelected = { rating ->
                                        viewModel.rateComic(safeComic.id, rating)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SubscribeButton(
                                isComicSubscribed = safeComic.isSubscribed,
                                isLocalSideload = safeComic.isLocalSideload,
                                onSubscribeToComic = { viewModel.toggleComicSubscription(safeComic.id) },
                                onSubscribeToAuthor = { viewModel.subscribeToAuthor(safeComic.authorId) }
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            text = "Synopsis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = safeComic.description,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chapters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
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
                                    text = { Text("Latest First") },
                                    onClick = {
                                        expanded = false
                                        dropDownSelection = "Latest First"
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Oldest First") },
                                    onClick = {
                                        expanded = false
                                        dropDownSelection = "Oldest First"
                                    }
                                )
                            }
                        }
                    }
                }

                items(sortedChapters, key = {chapter -> chapter.id}) { chapter ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ChapterRow(
                            chapter = chapter,
                            onClick = {
                                onChapterClick(chapter)
                            }
                        )
                    }
                }
            }

            if (showActionSheet) {
                ComicActionBottomSheet(
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
                    viewModel = viewModel,
                    onComicClick = onComicClick,
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
        }
    }
}