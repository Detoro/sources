package toro.sources.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LikeButton(
    modifier: Modifier = Modifier,
    likeCount: Int = 0,
    isLiked: Boolean,
    onLike: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_animation"
    )

    val tint by animateColorAsState(
        targetValue = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray,
        label = "color_animation"
    )

    Row(verticalAlignment = Alignment.CenterVertically)  {
        Icon(
            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Like Post",
            tint = tint,
            modifier = modifier
                .scale(if (isLiked) scale else 1.0f)
                .clickable(
                    onClick = onLike
                )
                .padding(8.dp)
        )
        Spacer(modifier = modifier.width(8.dp))
        Text(likeCount.toString(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}