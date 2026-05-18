package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import toro.sources.AppViewModel
import toro.sources.dataModels.UserProfile

@Composable
fun SmartInput(
    onSend: (text: String, tags: List<String>, mentions: List<String>, attachment: Uri?) -> Unit,
    placeholder: String = "Type a message...",
    supportTags: Boolean = false,
    supportUpload: Boolean = false,
    viewModel: AppViewModel? = null
) {
    var inputText by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showTagInput by remember { mutableStateOf(false) }
    val userSuggestions by viewModel?.userSuggestions?.collectAsState() ?: remember { mutableStateOf(emptyList<UserProfile>()) }
    var showMentions by remember { mutableStateOf(false) }
    val mentionedUserIds = remember { mutableStateListOf<String>() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedUri = uri
    }

    LaunchedEffect(inputText) {
        val lastWord = inputText.substringAfterLast(' ', inputText)
        if (lastWord.startsWith("@")) {
            val searchQuery = lastWord.removePrefix("@")
            viewModel?.searchUsers(searchQuery)
            showMentions = true
        } else {
            showMentions = false
        }
    }

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (showMentions && userSuggestions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(userSuggestions) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val lastSpace = inputText.lastIndexOf(' ')
                                        val prefix = if (lastSpace == -1) "" else inputText.substring(0, lastSpace + 1)
                                        inputText = "$prefix@${user.username} "
                                        if (!mentionedUserIds.contains(user.id)) {
                                            mentionedUserIds.add(user.id)
                                        }
                                        showMentions = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DefaultAvatar(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(user.username)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            if (selectedUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = "Attachment",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { selectedUri = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }

            AnimatedVisibility(visible = showTagInput) {
                TextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    placeholder = { Text("Tags (comma separated)...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium,
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (supportUpload) {
                    IconButton(onClick = { launcher.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Upload")
                    }
                }

                if (supportTags) {
                    IconButton(onClick = { showTagInput = !showTagInput }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Label,
                            contentDescription = "Tags",
                            tint = if (showTagInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() || selectedUri != null) {
                            val tags = if (tagsText.isBlank()) {
                                emptyList()
                            } else {
                                tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            }
                            
                            onSend(inputText, tags, mentionedUserIds.toList(), selectedUri)
                            inputText = ""
                            tagsText = ""
                            selectedUri = null
                            showTagInput = false
                            mentionedUserIds.clear()
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = inputText.isNotBlank() || selectedUri != null
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
