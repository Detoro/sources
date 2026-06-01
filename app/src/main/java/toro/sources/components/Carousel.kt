package toro.sources.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import com.toro.models.Comic
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@Composable
fun BillboardCarousel(
    comics: List<Comic>,
    onComicClick: (Comic) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comics.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { comics.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            val nextPage = (pagerState.currentPage + 1) % comics.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 16.dp
    ) { page ->
        val comic = comics[page]
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                val pageOffset = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue

                val scale = 1f - (pageOffset * 0.15f).coerceIn(0f, 1f)
                scaleX = scale
                scaleY = scale
                alpha = 1f - (pageOffset * 0.5f).coerceIn(0f, 1f)
                }
                .clickable { onComicClick(comic) },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = comic.coverImageUrl,
                    contentDescription = comic.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 300f
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = comic.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = comic.author,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicCarousel(
    title: String,
    comics: List<Comic>,
    viewModel: AppViewModel,
    onComicClick: (Comic) -> Unit,
    modifier: Modifier = Modifier
) {
    if (comics.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        HorizontalUncontainedCarousel(
            state = rememberCarouselState { comics.size },
            modifier = Modifier.fillMaxWidth(),
            itemWidth = 100.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { index ->
            val comic = comics[index]
            ComicCoverCard(
                comic = comic,
                viewModel = viewModel,
                onClick = { onComicClick(comic) }
            )
        }
    }
}
