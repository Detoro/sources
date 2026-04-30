package toro.sources.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.ThumbUpAlt
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.ThumbDownOffAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LikeButton(
    likeCount: Int = 0,
    isPostLiked: Boolean,
    onLikePost: () -> Unit,
    modifier: Modifier = Modifier
) {

    val scale by animateFloatAsState(
        targetValue = if (isPostLiked) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_animation"
    )

    val tint by animateColorAsState(
        targetValue = if (isPostLiked) MaterialTheme.colorScheme.primary else Color.Gray,
        label = "color_animation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                onClick = {
                    onLikePost()
                }
            )
            .padding(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (isPostLiked) Icons.Default.ThumbUpAlt else Icons.Outlined.ThumbDownOffAlt,
                contentDescription = "Like Post",
                tint = tint,
                modifier = Modifier.scale(if (isPostLiked) scale else 1.0f)
            )
            Text(likeCount.toString())
        }
    }
}