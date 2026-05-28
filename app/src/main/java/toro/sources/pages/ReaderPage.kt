package toro.sources.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.CommentsSection
import toro.sources.components.MuteToggleButton
import toro.sources.components.ReaderNavigationBar
import toro.sources.components.SmartContentPage
import toro.sources.dataModels.Comic

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ReaderScreen(
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
    val isLiked = remember(chapters, chapterId) {
        chapters.find { it.id == chapterId }?.isLiked ?: false
    }

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

    // Navbar visibility logic
    val isImeVisible = WindowInsets.isImeVisible
    val isNavbarVisible by remember {
        derivedStateOf {
            if (isImeVisible) {
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

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < pageCount) {
            onPageChanged(pagerState.currentPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        if (comic.scrollDirection == "HORIZONTAL") {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                if (pageIndex < pageCount) {
                    SmartContentPage(pageIndex, viewModel)
                } else {
                    CommentsSection(
                        viewModel = viewModel,
                        chapterId = chapterId,
                        onViewAllClick = { onViewAllComments(chapterId) },
                        onCommentClick = { comment -> onCommentThreadClick(chapterId, comment.id) }
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
                        chapterId = chapterId,
                        onViewAllClick = { onViewAllComments(chapterId) },
                        onCommentClick = { comment -> onCommentThreadClick(chapterId, comment.id) }
                    )
                }
            }
        }

        if (comic.hasMusic) {
            MuteToggleButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
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