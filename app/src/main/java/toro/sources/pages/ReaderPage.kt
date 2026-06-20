package toro.sources.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.CommentsSection
import toro.sources.components.MuteToggleButton
import toro.sources.components.ReaderNavigationBar
import toro.sources.components.SmartContentPage
import toro.sources.components.ChapterBgmPlayer // Import the new player
import com.toro.models.Comic
import com.toro.models.ScrollDirection

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderPage(
    pageCount: Int,
    comic: Comic,
    viewModel: AppViewModel,
    chapterId: String,
    startingIndex: Int = 0,
    onPageChanged: (Int) -> Unit,
    onNextChapter: () -> Unit = {},
    onPreviousChapter: () -> Unit = {},
    onLikeChapter: () -> Unit = {},
    onViewAllComments: (String) -> Unit = {},
    onCommentThreadClick: (String, String) -> Unit = { _, _ -> }
) {
    if (pageCount == 0) return

    val chapters by viewModel.chapters.collectAsState()

    // 1. Isolate the current chapter to check for its specific audioUrl
    val currentChapter = remember(chapters, chapterId) {
        chapters.find { it.id == chapterId }
    }

    val isLiked = currentChapter?.isLiked ?: false

    // 2. Track the mute state for the music player
    var isMuted by remember { mutableStateOf(false) }

    LaunchedEffect(comic.id) {
        viewModel.getChapterComments(chapterId)
    }

    val pagerState = rememberPagerState(
        initialPage = startingIndex,
        pageCount = { if (comic.scrollDirection == "HORIZONTAL") pageCount + 1 else pageCount }
    )
    val listState = rememberLazyListState()

    // Scroll direction tracking
    var scrollingDown by remember { mutableStateOf(false) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > previousIndex) {
                    scrollingDown = true
                } else if (index < previousIndex) {
                    scrollingDown = false
                } else {
                    if (offset > previousScrollOffset) {
                        scrollingDown = true
                    } else if (offset < previousScrollOffset) {
                        scrollingDown = false
                    }
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < pageCount) {
            onPageChanged(pagerState.currentPage)
        }
    }

    var globalScale by remember { mutableFloatStateOf(1f) }
    var globalOffset by remember { mutableStateOf(Offset.Zero) }

    // Navbar visibility logic
    val isImeVisible = WindowInsets.isImeVisible
    val isNavbarVisible by remember {
        derivedStateOf {
            if (isImeVisible || globalScale > 1f) {
                false
            } else if (comic.scrollDirection == "HORIZONTAL") {
                // Show at the end (comments page)
                pagerState.currentPage == pageCount
            } else {
                // Show when scrolling down or at the end
                (scrollingDown && (listState.firstVisibleItemScrollOffset > 0 || listState.firstVisibleItemIndex > 0))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        ChapterBgmPlayer(
            audioUrl = currentChapter?.audioUrl,
            isMuted = isMuted
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = globalScale,
                    scaleY = globalScale,
                    translationX = globalOffset.x,
                    translationY = globalOffset.y
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (globalScale > 1f) {
                                globalScale = 1f
                                globalOffset = Offset.Zero
                            } else {
                                globalScale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (zoom != 1f || pan != Offset.Zero) {
                                val newScale = (globalScale * zoom).coerceIn(1f, 5f)
                                globalScale = newScale

                                if (globalScale > 1f) {
                                    val maxX = (size.width * (globalScale - 1)) / 2
                                    val maxY = (size.height * (globalScale - 1)) / 2

                                    globalOffset = Offset(
                                        x = (globalOffset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (globalOffset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                    event.changes.forEach { it.consume() }
                                } else {
                                    globalOffset = Offset.Zero
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            if (comic.scrollDirection == ScrollDirection.HORIZONTAL.name) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    if (pageIndex < pageCount) {
                        SmartContentPage(pageIndex, viewModel)
                        val chapter = chapters.find { it.id == chapterId }
                        chapter?.lastReadPageIndex = pageIndex
                    } else {
                        CommentsSection(
                            viewModel = viewModel,
                            onViewAllClick = { onViewAllComments(chapterId) },
                            onMakeFirstComment = { onViewAllComments(chapterId) },
                            onCommentClick = { comment -> onCommentThreadClick(chapterId, comment.id) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                ) {
                    items(count = pageCount) { pageIndex ->
                        SmartContentPage(pageIndex, viewModel)
                    }
                    item {
                        CommentsSection(
                            viewModel = viewModel,
                            onViewAllClick = { onViewAllComments(chapterId) },
                            onMakeFirstComment = { onViewAllComments(chapterId) },
                            onCommentClick = { comment -> onCommentThreadClick(chapterId, comment.id) },
                        )
                    }
                }
            }
        }
        if (!currentChapter?.audioUrl.isNullOrBlank()) {
            MuteToggleButton(
                isMuted = isMuted,
                onToggle = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }

        AnimatedVisibility(
            visible = isNavbarVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            ReaderNavigationBar(
                isLiked = isLiked,
                onPrev = onPreviousChapter,
                onNext = onNextChapter,
                onLike = onLikeChapter
            )
        }
    }
}