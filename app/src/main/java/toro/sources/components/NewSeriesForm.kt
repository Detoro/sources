package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import toro.sources.AppViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import com.toro.models.Genre
import com.toro.models.PgRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSeriesForm(
    viewModel: AppViewModel,
    onCancel: () -> Unit
)
{
    var title by remember { mutableStateOf("") }
    var ratingExpanded by remember { mutableStateOf(false) }
    var genreExpanded by remember { mutableStateOf(false) }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedChapterUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var selectedComicRating by remember { mutableStateOf(PgRating.ALL) }
    val selectedComicGenres = remember { mutableStateListOf<Genre>() }
    val context = LocalContext.current
    val isUploading by viewModel.isUploading.collectAsState()
    val ratingOptions = PgRating.entries
    val genreOptions = Genre.entries

    val displayText = if (selectedComicGenres.isEmpty()) {
        "Select Genres"
    } else {
        selectedComicGenres.joinToString(", ")
    }

    val chapterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedChapterUris = uris }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedCoverUri = uri }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "New Series",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Cancel",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onCancel() })
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Comic Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text("Author / Creator") },
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = ratingExpanded,
            onExpandedChange = { ratingExpanded = !ratingExpanded }
        ) {
            OutlinedTextField(
                value = selectedComicRating.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Series Rating") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ratingExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = ratingExpanded,
                onDismissRequest = { ratingExpanded = false }
            ) {
                ratingOptions.forEach { rating ->
                    DropdownMenuItem(
                        text = { Text(rating.name) },
                        onClick = {
                            selectedComicRating = rating
                            ratingExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = genreExpanded,
            onExpandedChange = { genreExpanded = it }
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Series Genres") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genreExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = genreExpanded,
                onDismissRequest = { genreExpanded = false }
            ) {
                genreOptions.forEach { genre ->
                    val isSelected = selectedComicGenres.contains(genre)
                    DropdownMenuItem(
                        text = { Text(genre.name) },
                        onClick = {
                            if (isSelected) {
                                selectedComicGenres.remove(genre)
                            } else {
                                selectedComicGenres.add(genre)
                            }
                        },
                        leadingIcon = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Synopsis") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedButton(
            onClick = { coverPickerLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Image, null)
            Spacer(Modifier.width(8.dp))
            Text(if (selectedCoverUri != null) "Cover Selected" else "Select Cover Image")
        }

        OutlinedButton(
            onClick = { chapterPickerLauncher.launch("application/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CloudUpload, null)
            Spacer(Modifier.width(8.dp))
            Text(if (selectedChapterUris.isNotEmpty()) "${selectedChapterUris.size} Chapters Selected" else "Select .cbz Files")
        }

        val isValid =
            title.isNotBlank() && author.isNotBlank() && selectedChapterUris.isNotEmpty() && !isUploading
        Button(
            onClick = {
                viewModel.uploadNewChapters(
                    context = context,
                    title = title,
                    author = author,
                    pgRating = selectedComicRating,
                    description = description,
                    chapterUris = selectedChapterUris,
                    selectedCover = selectedCoverUri
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = isValid
        ) {
            Text("Create Series & Upload")
        }
    }
}