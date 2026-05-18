package toro.sources.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MuteToggleButton(modifier: Modifier = Modifier) {
    var isMuted by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        tonalElevation = 4.dp
    ) {
        IconButton(onClick = { isMuted = !isMuted }) {
            Icon(
                imageVector = if (isMuted) Icons.Default.MusicOff else Icons.Default.MusicNote,
                contentDescription = "Toggle Music",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}