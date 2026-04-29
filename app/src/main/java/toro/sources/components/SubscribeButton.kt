package toro.sources.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubscribeButton(
    isComicSubscribed: Boolean,
    isLocalSideload: Boolean,
    onSubscribeToComic: () -> Unit,
    onSubscribeToAuthor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = !isLocalSideload

    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isComicSubscribed) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce_animation"
    )

    val tint by animateColorAsState(
        targetValue = if (isComicSubscribed) MaterialTheme.colorScheme.primary else Color.Gray,
        label = "color_animation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .combinedClickable(
                enabled = isEnabled,
                onClick = {
                    onSubscribeToComic()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSubscribeToAuthor()
                }
            )
            .padding(8.dp)
    ) {
        Icon(
            imageVector = if (isComicSubscribed) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions,
            contentDescription = "Subscribe to Comic",
            tint = if (isEnabled) tint else Color.LightGray.copy(alpha = 0.5f),
            modifier = Modifier.scale(if (isEnabled) scale else 1.0f)
        )
    }
}