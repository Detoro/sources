package toro.sources.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.CommentsSection
import toro.sources.components.MuteToggleButton
import toro.sources.components.ReaderNavigationBar
import toro.sources.components.SmartContentPage
import toro.sources.dataModels.Comic

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    pageCount: Int,
    comic: Comic,
    chapterId: String,
    viewModel: AppViewModel,
    startingIndex: Int = 0,
    onPageChanged: (Int) -> Unit,
    onNextChapter: () -> Unit = {},
    onPreviousChapter: () -> Unit = {},
    onLikeChapter: () -> Unit = {},
    onViewAllComments: (String) -> Unit = {},
    onCommentThreadClick: (String, String) -> Unit = { _, _ -> }
) {
    if (pageCount == 0) return

    val pagerState = rememberPagerState(
        initialPage = startingIndex,
        pageCount = { if (comic.scrollDirection == "HORIZONTAL") pageCount + 1 else pageCount }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < pageCount) {
            onPageChanged(pagerState.currentPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        onViewAllClick = { onViewAllComments(chapterId) },
                        onCommentClick = { comment -> onCommentThreadClick(chapterId, comment.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(count = pageCount) { pageIndex ->
                    SmartContentPage(pageIndex, viewModel)
                }
                item {
                    CommentsSection(
                        viewModel = viewModel,
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

        ReaderNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onPrev = onPreviousChapter,
            onNext = onNextChapter,
            onLike = onLikeChapter
        )
    }
}