package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    onCancel: () -> Unit,
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
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        expanded = false
                        selectedChapterUris = emptyList()
                        selectedComicId = null
                        selectedComicTitle = ""
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
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedComicTitle,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Series") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
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

            OutlinedButton(
                onClick = { chapterPickerLauncher.launch("application/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedComicId != null
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (selectedChapterUris.isNotEmpty()) "${selectedChapterUris.size} Chapters Selected" else "Select .cbz Files")
            }

            OutlinedButton(
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedComicId != null
            ) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (selectedAudioUris.isNotEmpty()) "${selectedAudioUris.size} Songs Selected" else "Select Background Music (Optional)")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = isValid
            ) {
                Text("Upload Chapters")
            }
        }
    }
}
