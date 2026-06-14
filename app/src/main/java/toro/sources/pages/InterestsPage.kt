package toro.sources.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toro.models.Genre

@Composable
fun InterestsPage(
    username: String,
    onComplete: (List<Genre>) -> Unit,
    onProceed: () -> Unit
) {
    var selectedGenres by remember { mutableStateOf(setOf<Genre>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome, $username!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select genres you're interested in to personalize your feed",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Genre.entries) { genre ->
                GenreItem(
                    genre = genre,
                    isSelected = selectedGenres.contains(genre),
                    onToggle = {
                        selectedGenres = if (selectedGenres.contains(genre)) {
                            selectedGenres - genre
                        } else {
                            selectedGenres + genre
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onComplete(selectedGenres.toList())
                onProceed() },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedGenres.isNotEmpty(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue", modifier = Modifier.padding(vertical = 8.dp))
        }

        TextButton(
            onClick = {
                onComplete(emptyList())
                onProceed() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}

@Composable
fun GenreItem(
    genre: Genre,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val icon = getIconForGenre(genre)
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = genre.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getIconForGenre(genre: Genre): ImageVector {
    return when (genre) {
        Genre.ACTION -> Icons.Default.FlashOn
        Genre.THRILLER -> Icons.Default.Visibility
        Genre.COMEDY -> Icons.Default.SentimentVerySatisfied
        Genre.ROMANCE -> Icons.Default.Favorite
        Genre.HORROR -> Icons.Default.Warning
        Genre.HISTORICAL -> Icons.Default.HistoryEdu
        Genre.DRAMA -> Icons.Default.TheaterComedy
        Genre.FANTASY -> Icons.Default.AutoAwesome
    }
}