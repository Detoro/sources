package toro.sources.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SpoilerText(
    text: String,
    modifier: Modifier = Modifier,
    isSpoiler: Boolean = false,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null
) {
    SpoilerText(
        annotatedString = AnnotatedString(text),
        modifier = modifier,
        isSpoiler = isSpoiler,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        onClick = onClick
    )
}

@Composable
fun SpoilerText(
    annotatedString: AnnotatedString,
    modifier: Modifier = Modifier,
    isSpoiler: Boolean = false,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    onClick: (() -> Unit)? = null,
    onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {}
) {
    if (!isSpoiler) {
        Text(
            text = annotatedString,
            modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = onTextLayout
        )
        return
    }

    var isRevealed by remember { mutableStateOf(false) }
    val blurRadius by animateFloatAsState(
        targetValue = if (isRevealed) 0f else 12f,
        label = "SpoilerBlur"
    )

    Box(
        modifier = modifier.clickable { 
            isRevealed = !isRevealed 
        }
    ) {
        Text(
            text = annotatedString,
            modifier = Modifier.blur(blurRadius.dp),
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = onTextLayout
        )
    }
}