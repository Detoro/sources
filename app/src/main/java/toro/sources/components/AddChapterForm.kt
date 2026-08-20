package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChapterForm(
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    profileViewModel: ProfileViewModel,
    onUploadComplete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedChapterUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val userWorks by profileViewModel.userWorks.collectAsState()
    var selectedComicId by remember { mutableStateOf<String?>(null) }
    var selectedComicTitle by remember { mutableStateOf("") }
    val isUploading by comicsViewModel.isUploading.collectAsState()
    val uploadSuccess by comicsViewModel.uploadSuccess.collectAsState()
    val currentUser by sessionViewModel.userProfile.collectAsState()
    var selectedAudioUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    fun clearForm() {
        expanded = false
        selectedChapterUris = emptyList()
        selectedAudioUris = emptyList()
        selectedComicId = null
        selectedComicTitle = ""
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedAudioUris = uris }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            profileViewModel.getUserWorks(currentUser!!.id)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Chapters") },
                actions = {
                    IconButton(onClick = { clearForm() }, enabled = !isUploading) {
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Series", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                if (userWorks.isEmpty()) {
                    Text(
                        "You don't have any series yet — create one before adding chapters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (!isUploading) expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedComicTitle,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isUploading,
                            label = { Text("Select Series") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            userWorks.forEach { comic ->
                                DropdownMenuItem(
                                    text = { Text(comic.title) },
                                    onClick = {
                                        selectedComicId = comic.id
                                        selectedComicTitle = comic.title
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Files", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                OutlinedButton(
                    onClick = { chapterPickerLauncher.launch("application/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedComicId != null && !isUploading
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedChapterUris.isNotEmpty()) "${selectedChapterUris.size} Chapters Selected" else "Select .cbz Files")
                }

                OutlinedButton(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = selectedComicId != null && !isUploading
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedAudioUris.isNotEmpty()) "${selectedAudioUris.size} Songs Selected" else "Select Background Music (Optional)")
                }
            }

            val isValid = selectedComicId != null && selectedChapterUris.isNotEmpty() && !isUploading
            Button(
                onClick = {
                    comicsViewModel.uploadNewChapters(
                        comicId = selectedComicId,
                        chapterUris = selectedChapterUris,
                        audioUris = selectedAudioUris
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = isValid
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Uploading...")
                } else {
                    Text("Upload Chapters")
                }
            }
        }
    }
}