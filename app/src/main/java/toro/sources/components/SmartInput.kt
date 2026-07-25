package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.toro.models.*
import kotlinx.coroutines.delay
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.CommunityViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartInput(
    modifier: Modifier = Modifier,
    title: String? = null,
    onTitleChange: ((String) -> Unit)? = null,
    supportTitle: Boolean = false,
    onTextChange: ((String) -> Unit)? = null,
    onSend: (title: String?, text: String, mentions: List<String>, sharedComicIds: List<String>, attachment: Uri?, isSpoiler: Boolean) -> Unit,
    initialText: String = "",
    placeholder: String = "Type a message...",
    supportUpload: Boolean = false,
    communityViewModel: CommunityViewModel = hiltViewModel(),
    comicsViewModel: ComicsViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel,
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

    val userSuggestions by communityViewModel.userSuggestions.collectAsState()
    val comicSuggestions by comicsViewModel.onlineLibrary.collectAsState()
    val comic by comicsViewModel.currentComic.collectAsState()
    val sharedContent by sessionViewModel.sharedContent.collectAsState()

    var showMentions by remember { mutableStateOf(false) }
    var showComicSearch by remember { mutableStateOf(false) }

    val mentionedUsers = remember { mutableStateListOf<Pair<String, String>>() }
    val sharedComicIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(sharedContent) {
        val content = sharedContent
        if (content?.type == ShareType.COMIC) {
            comicsViewModel.setCurrentComic(Comic(id = content.id, title = content.title, description = "", coverImageUrl = ""))
        } else {
            comicsViewModel.clearCurrentComic()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> selectedUri = uri }

    LaunchedEffect(inputText) {
        onTextChange?.invoke(inputText)
        mentionedUsers.removeAll { (_, username) -> !inputText.contains("@$username") }

        val lastWord = inputText.substringAfterLast(' ', inputText)
        if (lastWord.startsWith("@")) {
            val searchQuery = lastWord.removePrefix("@")
            showMentions = true
            showComicSearch = false
            delay(250.milliseconds)
            communityViewModel.searchUsers(searchQuery)
        } else {
            showMentions = false
            showComicSearch = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
                            if (mentionedUsers.none { it.first == user.id }) mentionedUsers.add(user.id to user.username)
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

                // Attachments Preview Area
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
                            val isVideo = selectedUri?.toString()?.contains("video") == true || selectedUri?.toString()?.endsWith(".mp4") == true

                            AsyncImage(
                                model = selectedUri,
                                contentDescription = "Attachment",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (isVideo) "Video Attached" else "Image Attached", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
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
                                    title = sharedContent!!.title,
                                    previewText = sharedContent!!.previewText,
                                    onClick = { },
                                    clickable = false,
                                    modifier = Modifier.weight(1f))
                            }
                            IconButton(onClick = { sessionViewModel.setSharedContent(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }

                // Title Field
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

                // Main Text Body
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

                // Action Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (supportUpload) {
                        IconButton(onClick = {
                            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Upload Media", tint = MaterialTheme.colorScheme.primary)
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
                                onSend(if (supportTitle) titleText else null, inputText, mentionedUsers.map { it.first }, sharedComicIds.toList(), selectedUri, isSpoiler)
                                inputText = ""
                                titleText = ""
                                selectedUri = null
                                mentionedUsers.clear()
                                sharedComicIds.clear()
                                isSpoiler = false
                                sessionViewModel.setSharedContent(null)
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
            ComicSearchBottomSheet(
                comicsViewModel = comicsViewModel,
                sessionViewModel = sessionViewModel,
                onDismiss = { showAttachComicSheet = false }
            )
        }
    }
}