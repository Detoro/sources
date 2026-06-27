package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

@Composable
fun InteractiveRatingStars(
    initialRating: Float,
    onRatingSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Int = 25
) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            val starIndex = index + 1
            val fillFraction = when {
                initialRating >= starIndex -> 1f
                initialRating > index -> initialRating - index
                else -> 0f
            }

            Box(
                modifier = Modifier
                    .size(starSize.dp)
                    .clickable {
                        val newRating = if (starIndex.toFloat() == initialRating) 0f else starIndex.toFloat()
                        onRatingSelected(newRating)
                    }
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxSize()
                )
                if (fillFraction > 0f) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rate $starIndex stars",
                        tint = Color.Green,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(right = size.width * fillFraction) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
            }
        }
    }
}