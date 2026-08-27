package toro.sources.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import models.Role
import models.ShareType
import toro.sources.components.*
import toro.sources.models.Chapter
import toro.sources.models.Comic
import toro.sources.models.authorId
import toro.sources.utils.getOptimizedUrl
import toro.sources.viewmodel.ChatViewModel
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.ProfileViewModel
import toro.sources.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OverviewPage(
    comicsViewModel: ComicsViewModel,
    profileViewModel: ProfileViewModel,
    sessionViewModel: SessionViewModel,
    chatViewModel: ChatViewModel,
    onAuthorClick: (String) -> Unit,
    onComicClick: (Comic) -> Unit,
    onChapterClick: (Chapter) -> Unit
) {
    val comic by comicsViewModel.currentComic.collectAsState()
    val chapters by comicsViewModel.chapters.collectAsState()
    val userRating = comic?.rating ?: 0f
    
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    
    var expanded by remember { mutableStateOf(false) }
    var dropDownSelection by remember { mutableStateOf("Latest First") }
    var synopsisExpanded by remember { mutableStateOf(false) }

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
    var showRateComicDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    LaunchedEffect(comic?.id) {
        comic?.let { comic ->
            comicsViewModel.getChaptersForComic(comic)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    comic?.let { currentComic ->
                        IconButton(
                            onClick = {
                                showRateComicDialog = true
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Outlined.Stars, contentDescription = "Rate")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                profileViewModel.getUserProfile(currentComic.authorId)
                                showInfoSheet = true
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = "Author info")
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

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 32.dp)
            ) {

                item {
                    val headerHeight = 450.dp
                    val headerHeightPx = with(density) { headerHeight.toPx() }
                    
                    val translationY = remember {
                        derivedStateOf {
                            if (listState.firstVisibleItemIndex == 0) {
                                listState.firstVisibleItemScrollOffset.toFloat() / 2f
                            } else {
                                0f
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(headerHeight)
                            .graphicsLayer {
                                this.translationY = translationY.value
                            }
                    ) {
                        AsyncImage(
                            model = safeComic.coverImageUrl.getOptimizedUrl(width = 1080),
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
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = headerHeightPx * 0.4f
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = safeComic.title,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val writers = safeComic.authors.filter { it.role == Role.WRITER || it.role.name.equals("AUTHOR", ignoreCase = true) }
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                writers.forEachIndexed { index, writer ->
                                    Text(
                                        text = writer.name + if (index < writers.size - 1) ", " else "",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { onAuthorClick(writer.id) }
                                    )
                                }
                            }

                            if (safeComic.genres.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    safeComic.genres.forEach { genre ->
                                        SuggestionChip(
                                            onClick = { },
                                            label = { 
                                                Text(
                                                    genre.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, 
                                                    style = MaterialTheme.typography.labelSmall
                                                ) 
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            shape = RoundedCornerShape(50)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val firstUnread = chapters.find { !it.isRead }
                        val buttonText = when {
                            chapters.isEmpty() -> "No Chapters"
                            firstUnread == null -> "Re-read from start"
                            chapters.any { it.isRead } -> "Continue Chapter ${firstUnread.chapterNumber?.toInt() ?: 1}"
                            else -> "Read Chapter 1"
                        }
                        
                        Button(
                            onClick = {
                                val target = firstUnread ?: chapters.firstOrNull()
                                target?.let { onChapterClick(it) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = chapters.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SubscribeButton(
                            isComicSubscribed = safeComic.isSubscribed,
                            isLocalSideload = safeComic.isLocalSideload,
                            onSubscribeToComic = { comicsViewModel.toggleComicSubscription(safeComic.id) },
                            onSubscribeToAuthor = { comicsViewModel.subscribeToAuthor(safeComic.authorId) }
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            text = safeComic.description,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (synopsisExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (safeComic.description.length > 100) {
                            TextButton(
                                onClick = { synopsisExpanded = !synopsisExpanded },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    if (synopsisExpanded) "Read Less" else "Read More",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(top = 10.dp, bottom = 2.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chapters",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(dropDownSelection, style = MaterialTheme.typography.labelLarge)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.Sort, null)
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
                    HorizontalDivider()
                }

                items(sortedChapters, key = { chapter -> chapter.id }) { chapter ->
                    ChapterRow(
                        chapter = chapter,
                        onClick = {
                            onChapterClick(chapter)
                        }
                    )
                }
            }

            if (showActionSheet) {
                ComicActionBottomSheet(
                    onDismiss = { showActionSheet = false },
                    onShare = { showShareDialog = true },
                    onRemove = {
                        comicsViewModel.removeComicFromLibrary(safeComic.id)
                    }
                )
            }

            if (showInfoSheet) {
                ComicInfoBottomSheet(
                    comic = safeComic,
                    profileViewModel = profileViewModel,
                    sessionViewModel = sessionViewModel,
                    onComicClick = onComicClick,
                    onDismiss = { showInfoSheet = false }
                )
            }

            if (showShareDialog) {
                ShareDialog(
                    sessionViewModel = sessionViewModel,
                    chatViewModel = chatViewModel,
                    sharedId = safeComic.id,
                    sharedType = ShareType.COMIC,
                    sharedTitle = safeComic.title,
                    sharedPreview = safeComic.description.take(50),
                    onDismiss = { showShareDialog = false }
                )
            }

            if (showRateComicDialog) {
                AlertDialog(
                    onDismissRequest = { showRateComicDialog = false },
                    title = { Text("Rate Comic") },
                    text = { Text("How would you rate this comic?") },
                    confirmButton = {
                        Box {
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
                                                comicsViewModel.rateComic(safeComic.id, rating)
                                            }
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = {
                                            showRateComicDialog = false
                                        }) { Text("Rate") }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}