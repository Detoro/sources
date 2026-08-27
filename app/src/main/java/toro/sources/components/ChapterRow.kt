package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import toro.sources.models.Chapter

@Composable
fun ChapterRow(
    chapter: Chapter,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clickable{ onClick() }
            .graphicsLayer(alpha = if (chapter.isRead) 0.5f else 1.0f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            Text(
                text = "Ch ${chapter.chapterNumber?.toInt() ?: 0}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = chapter.chapterTitle.take(15),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (chapter.isRead) FontWeight.Normal else FontWeight.SemiBold,
                color = if (chapter.isRead)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurface
            )
        }
    }
    HorizontalDivider()
}