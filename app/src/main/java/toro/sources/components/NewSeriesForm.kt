package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.toro.models.*
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.CommunityViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewSeriesForm(
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    communityViewModel: CommunityViewModel,
    onUploadComplete: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var ratingExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }
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

    val chapterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedChapterUris = uris }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedAudioUris = uris }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> selectedCoverUri = uri }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create New Series", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = {
                        title = ""
                        description = ""
                        selectedChapterUris = emptyList()
                        selectedCoverUri = null
                        selectedComicRating = PgRating.ALL
                        selectedScrollDirection = ScrollDirection.VERTICAL
                        selectedComicGenres.clear()
                    }) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                },
                windowInsets = WindowInsets(top = 0.dp)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                FormSection(title = "Visuals") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp, 220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedCoverUri != null) {
                                AsyncImage(
                                    model = selectedCoverUri,
                                    contentDescription = "Cover Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Add Cover", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }

                FormSection(title = "Series Details") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            placeholder = { Text("What is your comic called?") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Synopsis") },
                            placeholder = { Text("Briefly describe your story...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                FormSection(title = "Identity") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Genres", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            genreOptions.forEach { genre ->
                                val isSelected = selectedComicGenres.contains(genre)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedComicGenres.remove(genre)
                                        else selectedComicGenres.add(genre)
                                    },
                                    label = { Text(genre.name.lowercase().capitalize()) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DropdownField(
                                label = "Rating",
                                value = selectedComicRating.name,
                                modifier = Modifier.weight(1f),
                                expanded = ratingExpanded,
                                onExpandedChange = { ratingExpanded = it }
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

                            DropdownField(
                                label = "Direction",
                                value = selectedScrollDirection.name.lowercase().capitalize(),
                                modifier = Modifier.weight(1f),
                                expanded = directionExpanded,
                                onExpandedChange = { directionExpanded = it }
                            ) {
                                scrollDirectionOptions.forEach { direction ->
                                    DropdownMenuItem(
                                        text = { Text(direction.name.lowercase().capitalize()) },
                                        onClick = {
                                            selectedScrollDirection = direction
                                            directionExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                FormSection(title = "Team") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedAuthors.forEach { creator ->
                                InputChip(
                                    selected = true,
                                    onClick = { },
                                    label = { Text("${creator.name} (${creator.role.name.lowercase().capitalize()})") },
                                    trailingIcon = {
                                        if (creator.id != currentUser?.id) {
                                            IconButton(
                                                onClick = { selectedAuthors = selectedAuthors.filter { it.id != creator.id } },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    },
                                    avatar = {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                            AssistChip(
                                onClick = { showAuthorSearch = true },
                                label = { Text("Add Co-Creator") },
                                leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }

                FormSection(title = "Media & Chapters") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MediaUploadButton(
                            label = "Upload Chapters (.cbz)",
                            count = selectedChapterUris.size,
                            icon = Icons.Default.CloudUpload,
                            onClick = { chapterPickerLauncher.launch("application/*") }
                        )

                        MediaUploadButton(
                            label = "Background Music (Optional)",
                            count = selectedAudioUris.size,
                            icon = Icons.Default.LibraryMusic,
                            onClick = { audioPickerLauncher.launch("audio/*") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isValid = title.isNotBlank() && selectedChapterUris.isNotEmpty() && !isUploading
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
                            genres = selectedComicGenres.toList(),
                            startingChapterNumber = 1f
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isValid,
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Uploading...")
                    } else {
                        Text("Create Series & Start Upload", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isUploading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
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

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        content()
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaUploadButton(
    label: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                if (count > 0) {
                    Text("$count files selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (count > 0) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun String.capitalize(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }