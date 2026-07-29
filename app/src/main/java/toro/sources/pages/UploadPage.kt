package toro.sources.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.CenterAlignedTopAppBar
import toro.sources.components.UploadMode
import toro.sources.components.UploadModeSelection
import toro.sources.viewmodel.ComicsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPage(
    comicsViewModel: ComicsViewModel,
    onBackClick: () -> Unit,
    onUploadNewComic: () -> Unit,
    onUploadNewChapter: () -> Unit,
    onUploadComplete: () -> Unit
) {
    var uploadMode by remember { mutableStateOf<UploadMode?>(null) }

    val isUploading by comicsViewModel.isUploading.collectAsState()
    val uploadSuccess by comicsViewModel.uploadSuccess.collectAsState()

    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            onUploadComplete()
            comicsViewModel.resetUploadState()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Creator Studio") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBackClick()
                            uploadMode = null
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (uploadMode == null) {
                    UploadModeSelection(
                        onModeSelected = { mode ->
                            uploadMode = mode
                        }
                    )
                } else {
                    when (uploadMode) {
                        UploadMode.NEW_SERIES -> {
                            onUploadNewComic()
                        }
                        UploadMode.ADD_CHAPTER -> {
                            onUploadNewChapter()
                        }
                        else -> {}
                    }
                }
            }

            if (isUploading) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}