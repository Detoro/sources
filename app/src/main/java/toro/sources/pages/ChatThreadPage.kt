package toro.sources.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toro.models.ShareType
import kotlinx.coroutines.delay
import toro.sources.AppViewModel
import toro.sources.components.ChatBubble
import toro.sources.components.SmartInput
import toro.sources.components.TypingIndicator
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadPage(
    conversationId: String,
    viewModel: AppViewModel,
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val me by viewModel.userProfile.collectAsState()
    val inbox by viewModel.filteredInbox.collectAsState()
    val replyingToMessage by viewModel.replyingToMessage.collectAsState(null)
    val typingUsers by viewModel.typingUsers.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val activeChat = remember(conversationId, inbox) {
        inbox.find { it.conversationId == conversationId }
    }
    val targetUserId = activeChat?.otherUserId ?: ""

    val filteredMessages = remember(messages, searchQuery, isSearching) {
        if (!isSearching || searchQuery.isEmpty()) {
            messages
        } else {
            messages.filter { msg ->
                val decrypted = if (msg.isEncrypted) viewModel.decryptMessage(msg.content) else msg.content
                decrypted.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(conversationId, targetUserId) {
        viewModel.resetChatState()
        viewModel.getChatMessages(conversationId)
        if (targetUserId.isNotEmpty()) {
            viewModel.getUserProfile(targetUserId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetChatState()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .imePadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = isSearching,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "SearchTransition"
                    ) { searching ->
                        if (searching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search messages...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(50)
                                    ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = { onProfileClick(targetUserId) },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = activeChat?.otherUserName?.uppercase() ?: "CHAT",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) searchQuery = ""
                            else isSearching = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Search Chat") },
                                    onClick = {
                                        expanded = false
                                        isSearching = true
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                                DropdownMenuItem(
                                    text = { Text("Unadd Friend") },
                                    onClick = {
                                        expanded = false
                                        viewModel.unAddFriend(targetUserId)
                                        onBackClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Chat", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        expanded = false
                                        viewModel.clearChatHistory(conversationId)
                                    }
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                windowInsets = WindowInsets(top = 3.dp)
            )
        },
        bottomBar = {
            if (!isSearching) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AnimatedVisibility(
                            visible = replyingToMessage != null,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp)
                        ) {
                            replyingToMessage?.let { replyTarget ->
                                val rawDecrypted = if (replyTarget.isEncrypted) viewModel.decryptMessage(replyTarget.content) else replyTarget.content
                                val cleanDecryptedText = rawDecrypted.replace("\u0000", "").trim()
                                val repliedSharedType = if (replyTarget.sharedId != null) replyTarget.sharedType else if (replyTarget.sharedComicId != null) ShareType.COMIC else null
                                val replyText = when {
                                    replyTarget.isSpoiler -> "Spoiler detected"
                                    cleanDecryptedText.isNotEmpty() && cleanDecryptedText != "null" -> cleanDecryptedText
                                    replyTarget.imageUrl != null -> "Shared Image"
                                    replyTarget.videoUrl != null -> "Shared Video"
                                    repliedSharedType != null -> "Shared ${
                                        repliedSharedType.name.lowercase()
                                            .replaceFirstChar { it.uppercase() }
                                    }"
                                    replyTarget.sharedId != null -> "Shared Content"
                                    else -> "Attachment"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(40.dp)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (replyTarget.senderId == me?.id) "You" else "${activeChat?.otherUserName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = replyText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(onClick = { viewModel.setReplyTarget(null) }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel Reply",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        val sharedContent by viewModel.sharedContent.collectAsState()
                        val editingMessage by viewModel.editingMessage.collectAsState()
                        var isCurrentlyTyping by remember { mutableStateOf(false) }
                        LaunchedEffect(isCurrentlyTyping) {
                            if (isCurrentlyTyping) {
                                viewModel.sendTypingIndicator(conversationId, true)
                                delay(3000.milliseconds)
                                isCurrentlyTyping = false
                                viewModel.sendTypingIndicator(conversationId, false)
                            }
                        }
                        SmartInput(
                            onTextChange = { newText ->
                                if (newText.isNotEmpty() && !isCurrentlyTyping) {
                                    isCurrentlyTyping = true
                                }
                            },
                            onSend = { _, text, _, _, attachment, isSpoiler ->
                                isCurrentlyTyping = false
                                viewModel.sendTypingIndicator(conversationId, false)
                                val currentEditingMessage = editingMessage
                                if (currentEditingMessage != null) {
                                    viewModel.editMessage(
                                        currentEditingMessage.conversationId,
                                        currentEditingMessage.id,
                                        text
                                    )
                                    viewModel.setEditingMessage(null)
                                } else if (targetUserId.isNotEmpty()) {
                                    viewModel.sendMessage(
                                        conversationId,
                                        text,
                                        isSpoiler = isSpoiler,
                                        sharedId = sharedContent?.id,
                                        sharedType = sharedContent?.type,
                                        attachment = attachment
                                    )
                                }
                            },
                            initialText = remember(editingMessage) {
                                editingMessage?.let { msg ->
                                    if (msg.isEncrypted) viewModel.decryptMessage(msg.content) else msg.content
                                } ?: ""
                            },
                            placeholder = if (editingMessage != null) "Edit message..." else "Type a message...",
                            supportUpload = editingMessage == null,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val lastUserMessageIndex = messages.indexOfFirst { it.senderId == me?.id }
        val isTargetTyping = typingUsers.contains(targetUserId)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isTargetTyping) {
                item {
                    TypingIndicator()
                }
            }
            itemsIndexed(filteredMessages, key = { _, msg -> msg.id }) { index, msg ->
                val isFromMe = remember(msg.senderId, me?.id) {
                    msg.senderId.isNotBlank() && msg.senderId.equals(me?.id, ignoreCase = true)
                }

                LaunchedEffect(msg.id) {
                    if (!isFromMe && !msg.isRead) {
                        viewModel.markMessageAsRead(msg.id)
                    }
                }

                ChatBubble(
                    message = msg,
                    isFromMe = isFromMe,
                    showStatus = isFromMe && index == lastUserMessageIndex,
                    viewModel = viewModel
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}