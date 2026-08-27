package toro.sources.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ReplyPreview(
    senderName: String,
    previewText: String,
    isFromMe: Boolean,
    shouldBeVisible: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {
    val accentColor = if (isFromMe) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.primary
    val scrimColor = if (isFromMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(scrimColor)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1
                )
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (shouldBeVisible) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel Reply",
                    tint = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}