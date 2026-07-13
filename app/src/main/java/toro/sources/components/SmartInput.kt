package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import toro.sources.AppViewModel
import com.toro.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartInput(
    title: String? = null,
    onTitleChange: ((String) -> Unit)? = null,
    supportTitle: Boolean = false,
    onTextChange: ((String) -> Unit)? = null,
    onSend: (title: String?, text: String, mentions: List<String>, sharedComicIds: List<String>, attachment: Uri?, isSpoiler: Boolean) -> Unit,
    initialText: String = "",
    placeholder: String = "Type a message...",
    supportUpload: Boolean = false,
    viewModel: AppViewModel,
    onValueChange: ((String?, String) -> Unit)? = null,
    maxChars: Int = 1000
) {
    var inputText by remember(initialText) { mutableStateOf(initialText) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isSpoiler by remember { mutableStateOf(false) }
    var titleText by remember(title) { mutableStateOf(title ?: "") }
    var showAttachComicSheet by remember { mutableStateOf(false) }

    LaunchedEffect(titleText, inputText) {
        onValueChange?.invoke(titleText, inputText)
    }

    val userSuggestions by viewModel.userSuggestions.collectAsState()
    val comicSuggestions by viewModel.catalog.collectAsState()
    val comic by viewModel.currentComic.collectAsState()
    val sharedContent by viewModel.sharedContent.collectAsState()

    var showMentions by remember { mutableStateOf(false) }
    var showComicSearch by remember { mutableStateOf(false) }

    val mentionedUserIds = remember { mutableStateListOf<String>() }
    val sharedComicIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(sharedContent) {
        sharedContent?.let { content ->
            if (content.type == ShareType.COMIC) {
                viewModel.getComicById(content.id)
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedUri = uri }

    LaunchedEffect(inputText) {
        onTextChange?.invoke(inputText)
        val lastWord = inputText.substringAfterLast(' ', inputText)
        if (lastWord.startsWith("@")) {
            val searchQuery = lastWord.removePrefix("@")
            viewModel.searchUsers(searchQuery)
            showMentions = true
            showComicSearch = false
        } else {
            showMentions = false
            showComicSearch = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Mentions & Suggestions Floating Surface
        if (showMentions && userSuggestions.isNotEmpty()) {
            SuggestionsSurface {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(userSuggestions) { user ->
                        SuggestionItem(user.username, user.avatarUrl) {
                            val lastSpace = inputText.lastIndexOf(' ')
                            val prefix = if (lastSpace == -1) "" else inputText.substring(0, lastSpace + 1)
                            inputText = "$prefix@${user.username} "
                            if (!mentionedUserIds.contains(user.id)) mentionedUserIds.add(user.id)
                            showMentions = false
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showComicSearch && comicSuggestions.isNotEmpty()) {
            SuggestionsSurface {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(comicSuggestions) { c ->
                        SuggestionItem(c.title, c.coverImageUrl) {
                            val lastSpace = inputText.lastIndexOf(' ')
                            val prefix = if (lastSpace == -1) "" else inputText.substring(0, lastSpace + 1)
                            inputText = "$prefix#${c.title} "
                            if (!sharedComicIds.contains(c.id)) sharedComicIds.add(c.id)
                            showComicSearch = false
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Main Input Surface
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // 1. Attachments Preview Area
                if (sharedContent != null || selectedUri != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedUri != null) {
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = "Attachment",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Image Attached", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { selectedUri = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        } else if (sharedContent != null) {
                            if (sharedContent!!.type == ShareType.COMIC && comic != null) {
                                AsyncImage(
                                    model = comic!!.coverImageUrl,
                                    contentDescription = "Attached Comic",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(comic!!.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = Ellipsis)
                                    Text(comic!!.authorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                SharedContentPlaceholder(
                                    type = sharedContent!!.type,
                                    title = "Nothing",
                                    previewText = "Next to nothing",
                                    onClick = { },
                                    modifier = Modifier.weight(1f))
                            }
                            IconButton(onClick = { viewModel.setSharedContent(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }

                // 2. Title Field (If needed)
                if (supportTitle) {
                    TextField(
                        value = titleText,
                        onValueChange = { if (it.length <= 100) { titleText = it; onTitleChange?.invoke(it) } },
                        placeholder = { Text("Title", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )
                }

                // 3. Main Text Body
                TextField(
                    value = inputText,
                    onValueChange = { if (it.length <= maxChars) inputText = it },
                    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    minLines = 1,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                )

                // 4. Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (supportUpload) {
                        IconButton(onClick = { launcher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Image", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { showAttachComicSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Attach Comic", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconToggleButton(checked = isSpoiler, onCheckedChange = { isSpoiler = it }) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Spoiler",
                            tint = if (isSpoiler) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${inputText.length}/$maxChars",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (inputText.length >= maxChars) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 12.dp)
                    )

                    val canSend = inputText.isNotBlank() || selectedUri != null || sharedContent != null
                    IconButton(
                        onClick = {
                            if (canSend) {
                                onSend(if (supportTitle) titleText else null, inputText, mentionedUserIds.toList(), sharedComicIds.toList(), selectedUri, isSpoiler)
                                inputText = ""
                                titleText = ""
                                selectedUri = null
                                mentionedUserIds.clear()
                                sharedComicIds.clear()
                                isSpoiler = false
                                viewModel.setSharedContent(null)
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        enabled = canSend
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        if (showAttachComicSheet) {
            ComicSearchBottomSheet(viewModel, onDismiss = { showAttachComicSheet = false })
        }
    }
}