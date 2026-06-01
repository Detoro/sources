package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toro.models.Chapter

@Composable
fun ChapterRow(chapter: Chapter, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Text(
                text = chapter.chapterTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (chapter.lastReadPageIndex > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val totalPages = if (chapter.pageCount > 0) chapter.pageCount else 1
                ReadingProgressBar(
                    progress = chapter.lastReadPageIndex.toFloat() / totalPages,
                    icon = Icons.AutoMirrored.Filled.MenuBook
                )
                Text(
                    text = "Page ${chapter.lastReadPageIndex} of $totalPages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
