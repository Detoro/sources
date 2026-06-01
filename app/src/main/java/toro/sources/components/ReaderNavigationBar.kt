package toro.sources.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLike) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like Chapter",
                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(
                onClick = onPrev,
                modifier = Modifier.scale(2.0f, 1.5f)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowLeft,
                    contentDescription = "Previous Chapter"
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = onNext,
                modifier = Modifier.scale(2.0f, 1.5f)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = "Next Chapter")
            }
        }
    }
}