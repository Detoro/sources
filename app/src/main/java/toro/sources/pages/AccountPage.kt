package toro.sources.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.SettingSectionTitle
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import toro.sources.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun AccountPage(
    viewModel: AppViewModel,
    onLogoutClick: () -> Unit
) {
    var darkThemeEnabled by remember { mutableStateOf(true) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showMotiveDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val githubLink = stringResource(R.string.github_link)
    val currentUser by viewModel.currentUser.collectAsState()
    val avatarUri = currentUser.avatarUrl
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                viewModel.uploadAvatar(context, it)
            }
        }
    )

    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Change Username") },
            text = {
                TextField(
                    value = newUsername,
                    onValueChange = { newUsername = it },
                    label = { Text("New Username") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // In a real app, call viewModel.updateUsername(newUsername)
                    showUsernameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMotiveDialog) {
        AlertDialog(
            onDismissRequest = { showMotiveDialog = false },
            title = { Text("Motive") },
            text = { Text("The goal of Toro Sources is to provide a seamless, community-driven platform for reading and sharing comics, focused on accessibility and user privacy.") },
            confirmButton = {
                TextButton(onClick = { showMotiveDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showResetPasswordDialog = false },
            title = { Text("Reset Password") },
            text = { Text("A password reset link has been sent to your registered email address.") },
            confirmButton = {
                TextButton(onClick = { showResetPasswordDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Storage Info") },
            text = { Text("Local Comics: 124MB\nCached Data: 45MB\nTotal: 169MB") },
            confirmButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Account") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mock Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable(onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        })
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile Picture",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentUser.username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Change username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = { showUsernameDialog = true })
                )
            }

            HorizontalDivider()

            // 2. Settings List
            SettingSectionTitle("App Settings")

            ListItem(
                headlineContent = { Text("Dark Theme") },
                supportingContent = { Text("Toggle application theme") },
                leadingContent = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = darkThemeEnabled,
                        onCheckedChange = { darkThemeEnabled = it }
                    )
                }
            )

            ListItem(
                headlineContent = { Text("Storage") },
                supportingContent = { Text("Manage downloaded .cbz files") },
                leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
                modifier = Modifier.clickable(onClick = { showStorageDialog = true })
            )

            SettingSectionTitle("Account Actions")

            ListItem(
                headlineContent = { Text("Reset Password") },
                supportingContent = { Text("You'll receive a link to your email") },
                leadingContent = { Icon(Icons.Default.Password, contentDescription = null) },
                modifier = Modifier.clickable(onClick = { showResetPasswordDialog = true })
            )

            ListItem(
                headlineContent = { Text("Clear Image Cache") },
                supportingContent = { Text("Free up memory used by Coil") },
                leadingContent = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                modifier = Modifier.clickable(onClick = {
                    val imageLoader = ImageLoader(context)
                    imageLoader.memoryCache?.clear()
                    imageLoader.diskCache?.clear()
                })
            )

            SettingSectionTitle("About")

            ListItem(
                headlineContent = { Text("Motive") },
                supportingContent = { Text("The reason behind the app") },
                leadingContent = { Icon(Icons.Default.BubbleChart, contentDescription = null) },
                modifier = Modifier.clickable(onClick = { showMotiveDialog = true })
            )

            ListItem(
                headlineContent = { Text("Contributing") },
                supportingContent = { Text("You can help improve the app") },
                leadingContent = { Icon(Icons.Default.Build, contentDescription = null) },
                modifier = Modifier.clickable(onClick = { uriHandler.openUri(githubLink) })
            )

            ListItem(
                headlineContent = { Text("Release Notes") },
                supportingContent = { Text("Important information") },
                leadingContent = { Icon(Icons.Default.Book, contentDescription = null) },
                modifier = Modifier.clickable(onClick = { uriHandler.openUri(githubLink) })
            )

            ListItem(
                headlineContent = { Text("App Version") },
                supportingContent = { Text("Version 1.0") },
                leadingContent = { Icon(Icons.Default.Difference, contentDescription = null) },
                modifier = Modifier.clickable(onClick = { uriHandler.openUri(githubLink) })
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Logout Button
            OutlinedButton(
                onClick = {
                    viewModel.logoutUser(onLogoutComplete = {
                        onLogoutClick()
                    })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}