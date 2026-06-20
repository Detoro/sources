package toro.sources.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import toro.sources.R
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import toro.sources.AppViewModel

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    viewModel: AppViewModel,
    onLogoutClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val userProfile by viewModel.userProfile.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    var showMotiveDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var showClearDbDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    val repoLink = stringResource(R.string.github_link)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                ),
                windowInsets = WindowInsets(top = 3.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp), // Padding for the grouped cards
            verticalArrangement = Arrangement.spacedBy(24.dp) // Space between setting groups
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 3. Grouped Cards for Settings
            SettingsGroup(title = "Profile Privacy") {
                ListItem(
                    headlineContent = { Text("Private Profile") },
                    supportingContent = { Text("Only followers can see your posts and works") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = userProfile?.isPrivate ?: false,
                            onCheckedChange = { viewModel.toggleProfilePrivacy(userProfile!!.id) }
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            SettingsGroup(title = "App Settings") {
                ListItem(
                    headlineContent = { Text("Dark Theme") },
                    leadingContent = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                    trailingContent = {
                        Switch(checked = isDarkTheme, onCheckedChange = { viewModel.toggleDarkTheme(it) })
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            SettingsGroup(title = "Account Actions") {
                ListItem(
                    headlineContent = { Text("Change Username") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable { showUsernameDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Reset Password") },
                    leadingContent = { Icon(Icons.Default.Password, contentDescription = null) },
                    modifier = Modifier.clickable { showResetPasswordDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Clear Image Cache") },
                    leadingContent = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val imageLoader = ImageLoader(context)
                        imageLoader.memoryCache?.clear()
                        imageLoader.diskCache?.clear()
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Clear Local Database") },
                    leadingContent = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showClearDbDialog = true },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = MaterialTheme.colorScheme.error
                    )
                )
            }

            SettingsGroup(title = "About") {
                ListItem(
                    headlineContent = { Text("Motive") },
                    leadingContent = { Icon(Icons.Default.BubbleChart, contentDescription = null) },
                    modifier = Modifier.clickable { showMotiveDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Contribute") },
                    leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
                    modifier = Modifier.clickable { uriHandler.openUri(repoLink) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            ListItem(
                headlineContent = { Text("Log Out") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { viewModel.logoutUser(onLogoutClick) },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    headlineColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showMotiveDialog) {
        AlertDialog(
            onDismissRequest = { showMotiveDialog = false },
            title = { Text("Motive") },
            text = { Text("The goal of Toro Sources is to provide a seamless, community-driven platform for reading and sharing comics, focused on accessibility and user privacy.") },
            confirmButton = {
                TextButton(onClick = { showMotiveDialog = false }) { Text("Close") }
            }
        )
    }

    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showResetPasswordDialog = false },
            title = { Text("Reset Password") },
            text = { Text("A password reset link has been sent to your registered email address.") },
            confirmButton = {
                TextButton(onClick = { showResetPasswordDialog = false }) { Text("OK") }
            }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Storage Info") },
            text = { Text("Local Comics: 124MB\nCached Data: 45MB\nTotal: 169MB") },
            confirmButton = {
                TextButton(onClick = { showStorageDialog = false }) { Text("OK") }
            }
        )
    }

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
                    if (newUsername.isNotBlank()) {
                        userProfile?.username = newUsername
                        viewModel.updateUsername(newUsername)
                    }
                    showUsernameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearDbDialog) {
        AlertDialog(
            onDismissRequest = { showClearDbDialog = false },
            title = { Text("Clear Local Database") },
            text = { Text("This will permanently delete all local comics and reading progress. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLocalDatabase()
                        showClearDbDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDbDialog = false }) { Text("Cancel") }
            }
        )
    }
}


@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            content()
        }
    }
}