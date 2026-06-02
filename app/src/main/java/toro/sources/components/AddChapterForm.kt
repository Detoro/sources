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
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChapterForm(
    viewModel: AppViewModel,
    onCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedChapterUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val context = LocalContext.current
    val userWorks by viewModel.userWorks.collectAsState()
    var selectedComicId by remember { mutableStateOf<String?>(null) }
    var selectedComicTitle by remember { mutableStateOf("") }
    val isUploading by viewModel.isUploading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getUserWorks(viewModel.currentUser.value.userId)
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
                        .menuAnchor()
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
                Icon(Icons.Default.CloudUpload, "Upload Chapter")
                Spacer(Modifier.width(8.dp))
                Text(if (selectedChapterUris.isNotEmpty()) "${selectedChapterUris.size} Chapters Selected" else "Select .cbz Files")
            }

            val isValid = selectedComicId != null && selectedChapterUris.isNotEmpty() && !isUploading
            Button(
                onClick = {
                    viewModel.uploadNewChapters(
                        context = context,
                        comicId = selectedComicId,
                        chapterUris = selectedChapterUris
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
