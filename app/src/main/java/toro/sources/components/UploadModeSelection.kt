package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class UploadMode { NEW_SERIES, ADD_CHAPTER }

@Composable
fun UploadModeSelection(onModeSelected: (UploadMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "What would you like to do?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onModeSelected(UploadMode.NEW_SERIES) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Start a New Series", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Upload your first chapter and set up your series metadata.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onModeSelected(UploadMode.ADD_CHAPTER) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add Chapters to Existing Series", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Continue an ongoing series with new chapters.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}