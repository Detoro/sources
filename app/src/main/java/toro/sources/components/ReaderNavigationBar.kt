package toro.sources.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ReaderNavigationBar(
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier.scale(2.0f, 1.5f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowLeft, contentDescription = "Previous Chapter")
            }
            IconButton(onClick = onLike) {
                Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like Chapter",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.scale(2.0f, 1.5f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = "Next Chapter")
            }
        }
    }
}