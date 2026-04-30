package toro.sources.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import toro.sources.AppViewModel
import toro.sources.DataModels.Comic
import toro.sources.components.SmartContentPage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    pageCount: Int,
    comic: Comic,
    viewModel: AppViewModel,
    startingIndex: Int = 0,
    onPageChanged: (Int) -> Unit
) {
    if (pageCount == 0) return

    val pagerState = rememberPagerState(
        initialPage = startingIndex,
        pageCount = { pageCount }
    )

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    if (comic.scrollDirection == "HORIZONTAL") {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            SmartContentPage(comic, pageIndex, viewModel)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(count = pageCount) { pageIndex ->
                SmartContentPage(comic, pageIndex, viewModel)
            }
        }
    }
}