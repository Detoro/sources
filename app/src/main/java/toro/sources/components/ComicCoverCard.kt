package toro.sources.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import toro.sources.utils.getOptimizedUrl
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.SessionViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import models.ShareType
import models.SharedContent
import toro.sources.models.Comic

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicCoverCard(
    comic: Comic,
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    modifier: Modifier = Modifier
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { comicsViewModel.loadAndNavigateToComic(comic.id) },
                onLongClick = { showOptionsMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        AsyncImage(
            model = comic.coverImageUrl.getOptimizedUrl(width = 500),
            contentDescription = "${comic.title} cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = comic.title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        DropdownMenu(
            expanded = showOptionsMenu,
            onDismissRequest = { showOptionsMenu = false }
        ) {

            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    showOptionsMenu = false
                    sessionViewModel.setSharedContent(
                        SharedContent(
                            id = comic.id,
                            type = ShareType.COMIC,
                            title = comic.title,
                            previewText = "Comic"
                        )
                    )
                    sessionViewModel.showShareDialog(true)
                }
            )

            DropdownMenuItem(
                text = { Text("Remove") },
                onClick = {
                    showOptionsMenu = false
                    comicsViewModel.removeComicFromLibrary(comic.id)
                }
            )
        }
    }
}