package toro.sources.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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
import toro.sources.AppViewModel
import com.toro.models.Comic
import com.toro.models.ShareType
import com.toro.models.SharedContent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComicCoverCard(
    comic: Comic,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { showOptionsMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        AsyncImage(
            model = comic.coverImageUrl,
            contentDescription = "${comic.title} cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )

        DropdownMenu(
            expanded = showOptionsMenu,
            onDismissRequest = { showOptionsMenu = false }
        ) {

            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    showOptionsMenu = false
                    viewModel.setSharedContent(
                        SharedContent(
                            id = comic.id,
                            type = ShareType.COMIC,
                            title = comic.title,
                            previewText = "Comic"
                        )
                    )
                    viewModel.showShareDialog(true)
                }
            )

            DropdownMenuItem(
                text = { Text("Remove") },
                onClick = {
                    showOptionsMenu = false
                    viewModel.removeComicFromLibrary(comic.id)
                }
            )
        }
    }
}