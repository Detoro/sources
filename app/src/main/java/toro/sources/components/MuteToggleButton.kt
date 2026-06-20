package toro.sources.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MuteToggleButton(
    isMuted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 4.dp
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MusicOff else Icons.Default.MusicNote,
                contentDescription = "Toggle Music",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}