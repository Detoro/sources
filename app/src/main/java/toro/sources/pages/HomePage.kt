package toro.sources.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.DataModels.Comic
import toro.sources.components.ComicCoverCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    viewModel: AppViewModel,
    onComicClick: (Comic) -> Unit,
    onAccountClick: () -> Unit
) {
    val libraryList by viewModel.myLibrary.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importLocalComic(
                title = "Imported Comic",
                author = "Someone you appreciate",
                description = "Imported from device",
                comicUri = it
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                actions = {
                    IconButton(onClick = { viewModel.getCatalog() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync Catalog")
                    }
                    IconButton(onClick = { onAccountClick()
                    }) {
                        Icon(Icons.Default.Person, contentDescription = "Account")
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(onClick = {
                filePickerLauncher.launch("application/*")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Import File")
            }
        }
    ) { paddingValues ->

        if (libraryList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your library is empty.\nTap + to import a .cbz file!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(libraryList) { comic ->
                    ComicCoverCard(
                        comic = comic,
                        viewModel,
                        onClick = { onComicClick(comic) }
                    )
                }
            }
        }
    }
}