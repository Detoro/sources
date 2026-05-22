package toro.sources.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import toro.sources.R
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import toro.sources.AppViewModel
import toro.sources.components.SettingSectionTitle

@OptIn(ExperimentalCoilApi::class)
@Composable
fun SettingsPage(
    viewModel: AppViewModel,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val userProfile by viewModel.userProfile.collectAsState()
    var darkThemeEnabled by remember { mutableStateOf(true) }
    var showMotiveDialog by remember { mutableStateOf(false) }
    var showResetPasswordDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    val repoLink = stringResource(R.string.github_link)

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        SettingSectionTitle("Profile Privacy")
        ListItem(
            headlineContent = { Text("Private Profile") },
            supportingContent = { Text("Only followers can see your posts and works") },
            leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = userProfile?.isPrivate ?: false,
                    onCheckedChange = { viewModel.toggleProfilePrivacy(userProfile!!.id) }
                )
            }
        )

        SettingSectionTitle("App Settings")
        ListItem(
            headlineContent = { Text("Dark Theme") },
            leadingContent = { Icon(Icons.Default.ColorLens, contentDescription = null) },
            trailingContent = {
                Switch(checked = darkThemeEnabled, onCheckedChange = { darkThemeEnabled = it })
            }
        )

        SettingSectionTitle("Account Actions")
        ListItem(
            headlineContent = { Text("Change Username") },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
            modifier = Modifier.clickable { showUsernameDialog = true }
        )
        ListItem(
            headlineContent = { Text("Reset Password") },
            leadingContent = { Icon(Icons.Default.Password, contentDescription = null) },
            modifier = Modifier.clickable { showResetPasswordDialog = true }
        )
        ListItem(
            headlineContent = { Text("Clear Image Cache") },
            leadingContent = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            modifier = Modifier.clickable {
                val imageLoader = ImageLoader(context)
                imageLoader.memoryCache?.clear()
                imageLoader.diskCache?.clear()
            }
        )
        ListItem(
            headlineContent = { Text("Motive") },
            leadingContent = { Icon(Icons.Default.BubbleChart, contentDescription = null) },
            modifier = Modifier.clickable { showMotiveDialog = true }
        )
        ListItem(
            headlineContent = { Text("Contribute") },
            leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
            modifier = Modifier.clickable { uriHandler.openUri(repoLink) }
        )
        ListItem(
            headlineContent = { Text("Log Out") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            modifier = Modifier.clickable { viewModel.logoutUser(onLogoutClick) },
            colors = ListItemDefaults.colors(headlineColor = MaterialTheme.colorScheme.error)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showMotiveDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Motive") },
            text = { Text("The goal of Toro Sources is to provide a seamless, community-driven platform for reading and sharing comics, focused on accessibility and user privacy.") },
            confirmButton = {
                TextButton(onClick = { }) { Text("Close") }
            }
        )
    }

    if (showResetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Reset Password") },
            text = { Text("A password reset link has been sent to your registered email address.") },
            confirmButton = {
                TextButton(onClick = { }) { Text("OK") }
            }
        )
    }

    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Storage Info") },
            text = { Text("Local Comics: 124MB\nCached Data: 45MB\nTotal: 169MB") },
            confirmButton = {
                TextButton(onClick = { }) { Text("OK") }
            }
        )
    }

    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { },
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
                        viewModel.updateUsername(newUsername)
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancel") }
            }
        )
    }
}