package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun InteractiveRatingStars(
    initialRating: Int,
    onRatingSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Int = 25
) {
    Row(modifier = modifier) {
        repeat(5) { index ->
            val starIndex = index + 1
            val isFilled = starIndex <= initialRating
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "Rate $starIndex stars",
                tint = if (isFilled) Color.Green else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(starSize.dp)
                    .clickable {
                        val newRating = if (starIndex == initialRating) 0 else starIndex
                        onRatingSelected(newRating)
                    }
            )
        }
    }
}
