package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toro.models.*
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.CommunityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSeriesForm(
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    communityViewModel: CommunityViewModel,
    onCancel: () -> Unit,
    onUploadComplete: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var ratingExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }
    var genreExpanded by remember { mutableStateOf(false) }
    var showAuthorSearch by remember { mutableStateOf(false) }
    val currentUser by sessionViewModel.userProfile.collectAsState()
    var description by remember { mutableStateOf("") }
    var selectedChapterUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedAudioUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var selectedComicRating by remember { mutableStateOf(PgRating.ALL) }
    var selectedScrollDirection by remember { mutableStateOf(ScrollDirection.VERTICAL) }
    val selectedComicGenres = remember { mutableStateListOf<Genre>() }
    val isUploading by comicsViewModel.isUploading.collectAsState()
    val uploadSuccess by comicsViewModel.uploadSuccess.collectAsState()
    val ratingOptions = PgRating.entries
    val genreOptions = Genre.entries
    val roleOptions = Role.entries
    val scrollDirectionOptions = ScrollDirection.entries
    var selectedAuthors by remember {
        mutableStateOf(
            listOf(
                Creator(
                    id = currentUser?.id ?: "fallback-123",
                    name = currentUser?.username ?: "User",
                    role = Role.WRITER
                )
            )
        )
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null && selectedAuthors.any { it.id == "fallback-123" }) {
            selectedAuthors = listOf(
                Creator(
                    id = currentUser!!.id,
                    name = currentUser!!.username,
                    role = Role.WRITER
                )
            )
        }
    }

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            onUploadComplete()
            comicsViewModel.resetUploadState()
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
                        Icon(Icons.Default.Close, contentDescription = "Clear")
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

            Text("Authors", style = MaterialTheme.typography.titleMedium)
            selectedAuthors.forEach { creator ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("• ${creator.name} (${creator.role})")
                    if (creator.id != currentUser?.id) {
                        IconButton(onClick = {
                            selectedAuthors = selectedAuthors.filter { it.id != creator.id }
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }

            TextButton(onClick = { showAuthorSearch = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Co-Author")
            }

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
                    comicsViewModel.uploadNewChapters(
                        title = title,
                        authors = selectedAuthors,
                        scrollDirection = selectedScrollDirection,
                        pgRating = selectedComicRating,
                        description = description,
                        chapterUris = selectedChapterUris,
                        selectedCover = selectedCoverUri,
                        audioUris = selectedAudioUris,
                        genres = selectedComicGenres.toList()
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
        if (showAuthorSearch) {
            val userSuggestions by communityViewModel.userSuggestions.collectAsState()
            UserSearchDialog(
                userSuggestions = userSuggestions,
                onSearch = { communityViewModel.searchUsers(it) },
                onClearSuggestions = { communityViewModel.clearUserSuggestions() },
                title = "Add Co-Creator",
                roles = roleOptions,
                onDismiss = { showAuthorSearch = false },
                onUserSelected = { selectedUser, selectedRole ->
                    val newCreator = Creator(
                        id = selectedUser.id,
                        name = selectedUser.username,
                        role = selectedRole ?: Role.WRITER
                    )
                    selectedAuthors = selectedAuthors + newCreator
                    showAuthorSearch = false
                }
            )
        }
    }
}
