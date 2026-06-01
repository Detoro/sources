package toro.sources.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import toro.sources.AppViewModel
import com.toro.models.ShareType

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SmartInput(
    title: String? = null,
    onTitleChange: ((String) -> Unit)? = null,
    supportTitle: Boolean = false,
    onSend: (title: String?, text: String, mentions: List<String>, sharedComicIds: List<String>, attachment: Uri?) -> Unit,
    initialText: String = "",
    placeholder: String = "Type a message...",
    supportUpload: Boolean = false,
    viewModel: AppViewModel? = null,
    onValueChange: ((String?, String) -> Unit)? = null
) {
    var inputText by remember(initialText) { mutableStateOf(initialText) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isSpoiler by remember {mutableStateOf(false)}
    var titleText by remember(title) { mutableStateOf(title ?: "") }
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(titleText, inputText) {
        onValueChange?.invoke(
            titleText,
            inputText
        )
    }
    
    val userSuggestions by viewModel?.userSuggestions?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val comicSuggestions by viewModel?.catalog?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    
    var showMentions by remember { mutableStateOf(false) }
    var showComicSearch by remember { mutableStateOf(false) }
    
    val mentionedUserIds = remember { mutableStateListOf<String>() }
    val sharedComicIds = remember { mutableStateListOf<String>() }

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
            showComicSearch = false
        } else if (lastWord.startsWith("#")) {
            val searchQuery = lastWord.removePrefix("#")
            viewModel?.searchComics(searchQuery)
            showComicSearch = true
            showMentions = false
        } else {
            showMentions = false
            showComicSearch = false
        }
    }

    val sharedContent by viewModel?.sharedContent?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(sharedContent) {
        sharedContent?.let {
            if (inputText.isEmpty()) {
                inputText = "Sharing ${it.type.name.lowercase()}: ${it.title}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .imePadding(),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (sharedContent != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sharedContent!!.type == ShareType.COMIC) {
                    // Logic to find comic by ID for preview if needed, 
                    // or just use SharedContentPlaceholder
                    SharedContentPlaceholder(sharedContent!!.type)
                } else {
                    SharedContentPlaceholder(sharedContent!!.type)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(
                    onClick = { viewModel?.setSharedContent(null) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Mentions Suggestions
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
        }

        // Comic Suggestions
        if (showComicSearch && comicSuggestions.isNotEmpty()) {
            SuggestionsSurface {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(comicSuggestions) { comic ->
                        SuggestionItem(comic.title, comic.coverImageUrl) {
                            val lastSpace = inputText.lastIndexOf(' ')
                            val prefix = if (lastSpace == -1) "" else inputText.substring(0, lastSpace + 1)
                            inputText = "$prefix#${comic.title} "
                            if (!sharedComicIds.contains(comic.id)) sharedComicIds.add(comic.id)
                            showComicSearch = false
                        }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut())
                },
                modifier = Modifier.weight(1f),
                label = "InputExpansion"
            ) { expanded ->
                if (expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            if (supportUpload) {
                                IconButton(
                                    onClick = { launcher.launch("image/*") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Upload",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Collapse",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 4.dp,
                            shadowElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (supportTitle) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                        if (titleText.isEmpty()) {
                                            Text(
                                                "Title...",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        BasicTextField(
                                            value = titleText,
                                            onValueChange = {
                                                titleText = it
                                                onTitleChange?.invoke(it)
                                            },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            placeholder,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    BasicTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        minLines = 3,
                                        maxLines = 10,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Switch(
                                    checked = isSpoiler,
                                    onCheckedChange = { isSpoiler = it }
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .clickable { isExpanded = true }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                if (inputText.isEmpty()) placeholder else inputText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (inputText.isEmpty()) 
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) 
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = Ellipsis
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || selectedUri != null) {
                        onSend(
                            if (supportTitle) titleText else null,
                            inputText,
                            mentionedUserIds.toList(),
                            sharedComicIds.toList(),
                            selectedUri
                        )
                        inputText = ""
                        titleText = ""
                        selectedUri = null
                        mentionedUserIds.clear()
                        sharedComicIds.clear()
                        isExpanded = false
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .padding(bottom = 4.dp),
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