package toro.sources.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toro.models.Comic

@Composable
fun SharedComicCard(comic: Comic) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            coil.compose.AsyncImage(
                model = comic.coverImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = comic.title, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(text = comic.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}