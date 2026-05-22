package toro.sources.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.BillboardCarousel
import toro.sources.components.ComicCarousel
import toro.sources.dataModels.Comic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    viewModel: AppViewModel,
    onComicClick: (Comic) -> Unit,
    onAccountClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val libraryList by viewModel.myLibrary.collectAsState()
    val catalog by viewModel.catalog.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }

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
                title = { Text("Explore Sources") },
                actions = {
                    IconButton(onClick = { onNotificationsClick() }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = { filePickerLauncher.launch("application/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Import Comic")
                    }
                    IconButton(onClick = { onAccountClick() }) {
                        Icon(Icons.Default.Person, contentDescription = "Account")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (libraryList.isEmpty() && catalog.isEmpty()) {
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
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {

                item {
                    BillboardCarousel(
                        comics = catalog.take(5),
                        onComicClick = onComicClick,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                // My Library Section
                if (libraryList.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        ComicCarousel(
                            title = "My Library",
                            comics = libraryList,
                            viewModel = viewModel,
                            onComicClick = onComicClick
                        )
                    }
                }
                // Top Stories Section
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    ComicCarousel(
                        title = "Top Stories",
                        comics = catalog.shuffled().take(8),
                        viewModel = viewModel,
                        onComicClick = onComicClick
                    )
                }

                // For You Section
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    ComicCarousel(
                        title = "For You",
                        comics = catalog.shuffled().take(8),
                        viewModel = viewModel,
                        onComicClick = onComicClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
