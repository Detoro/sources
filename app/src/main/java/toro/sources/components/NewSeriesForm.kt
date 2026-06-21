package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import toro.sources.AppViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateListOf
import com.toro.models.Creator
import com.toro.models.Genre
import com.toro.models.PgRating
import com.toro.models.ScrollDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSeriesForm(
    viewModel: AppViewModel,
    onCancel: () -> Unit,
    onUploadComplete: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var ratingExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }
    var genreExpanded by remember { mutableStateOf(false) }
    val author by viewModel.currentUser.collectAsState()
    var description by remember { mutableStateOf("") }
    var selectedChapterUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedAudioUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var selectedComicRating by remember { mutableStateOf(PgRating.ALL) }
    var selectedScrollDirection by remember { mutableStateOf(ScrollDirection.VERTICAL) }
    val selectedComicGenres = remember { mutableStateListOf<Genre>() }
    val context = LocalContext.current
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadSuccess by viewModel.uploadSuccess.collectAsState()
    val ratingOptions = PgRating.entries
    val genreOptions = Genre.entries
    val scrollDirectionOptions = ScrollDirection.entries

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            onUploadComplete()
            viewModel.resetUploadState()
        }
    }

    val displayText = if (selectedComicGenres.isEmpty()) {
        "Select Genres"
    } else {
        selectedComicGenres.joinToString(", ")
    }

    val chapterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedChapterUris = uris }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedAudioUris = uris }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedCoverUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Series") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        title = ""
                        description = ""
                        selectedChapterUris = emptyList()
                        selectedCoverUri = null
                        selectedComicRating = PgRating.ALL
                        selectedScrollDirection = ScrollDirection.VERTICAL
                        selectedComicGenres.clear()
                    }) {
                        Icon(Filled.Close, contentDescription = "Post")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Comic Title") },
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
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
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
                expanded = directionExpanded,
                onExpandedChange = { directionExpanded = !directionExpanded }
            ) {
                OutlinedTextField(
                    value = selectedScrollDirection.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Reading direction") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionExpanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = directionExpanded,
                    onDismissRequest = { directionExpanded = false }
                ) {
                    scrollDirectionOptions.forEach { direction ->
                        DropdownMenuItem(
                            text = { Text(direction.name) },
                            onClick = {
                                selectedScrollDirection = direction
                                directionExpanded = false
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
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .fillMaxWidth()
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
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.LibraryMusic, "Upload Audio")
                Spacer(Modifier.width(8.dp))
                Text(if (selectedAudioUris.isNotEmpty()) "${selectedAudioUris.size} Songs Selected" else "Select Background Music (Optional)")
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
                title.isNotBlank() && selectedChapterUris.isNotEmpty() && !isUploading
            Button(
                onClick = {
                    val defaultCreator = Creator(
                        id = author.userId,
                        name = author.username,
                        role = "Creator"
                    )
                    viewModel.uploadNewChapters(
                        context = context,
                        title = title,
                        authors = listOf(defaultCreator),
                        scrollDirection = selectedScrollDirection,
                        pgRating = selectedComicRating,
                        description = description,
                        chapterUris = selectedChapterUris,
                        selectedCover = selectedCoverUri,
                        audioUris = selectedAudioUris
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = isValid
            ) {
                Text("Create Series & Upload")
            }
        }
    }
}