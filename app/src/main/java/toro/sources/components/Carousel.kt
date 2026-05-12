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
import toro.sources.dataModels.Comic

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
            itemWidth = 160.dp,
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
