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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPage(
    viewModel: AppViewModel,
    onLogoutClick: () -> Unit
) {
    var darkThemeEnabled by remember { mutableStateOf(true) }
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
                    text = viewModel.currentUser.collectAsState().value.username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Change username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = {})
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
                modifier = Modifier.clickable(onClick = {})
            )

            SettingSectionTitle("Account Actions")

            ListItem(
                headlineContent = { Text("Reset Password") },
                supportingContent = { Text("You'll receive a link to your email") },
                leadingContent = { Icon(Icons.Default.Password, contentDescription = null) },
                modifier = Modifier.clickable(onClick = {})
            )

            ListItem(
                headlineContent = { Text("Clear Image Cache") },
                supportingContent = { Text("Free up memory used by Coil") },
                leadingContent = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                modifier = Modifier.clickable(onClick = {})
            )

            SettingSectionTitle("About")

            ListItem(
                headlineContent = { Text("Motive") },
                supportingContent = { Text("The reason behind the app") },
                leadingContent = { Icon(Icons.Default.BubbleChart, contentDescription = null) },
                modifier = Modifier.clickable(onClick = {})
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
                supportingContent = { Text("Current Version") },
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