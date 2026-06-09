package toro.sources.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.ChatBubble
import toro.sources.components.SmartInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadPage(
    conversationId: String,
    viewModel: AppViewModel,
    onBackClick: () -> Unit,
    onProfileClick: (String) -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val me by viewModel.currentUser.collectAsState()
    val inbox by viewModel.inbox.collectAsState()
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

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search messages...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            trailingIcon = {
                                IconButton(onClick = { 
                                    if (searchQuery.isNotEmpty()) searchQuery = "" 
                                    else isSearching = false 
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Search")
                                }
                            }
                        )
                    } else {
                        TextButton(
                            onClick = { onProfileClick(targetUserId) }
                        ) {
                            Text("${activeChat?.otherUserName}".uppercase())
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
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Unadd Friend") },
                                    onClick = { 
                                        expanded = false
                                        viewModel.unAddFriend(targetUserId)
                                        onBackClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Chat") },
                                    onClick = { 
                                        expanded = false
                                        viewModel.clearChatHistory(conversationId)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Search Chat") },
                                    onClick = {
                                        expanded = false
                                        isSearching = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isSearching) {
                val sharedContent by viewModel.sharedContent.collectAsState()
                SmartInput(
                    onSend = { _, text, _, _, _ ->
                        if (targetUserId.isNotEmpty()) {
                            viewModel.sendMessage(
                                conversationId,
                                targetUserId,
                                text,
                                sharedId = sharedContent?.id,
                                sharedType = sharedContent?.type
                            )
                        }
                    },
                    placeholder = "Type a message...",
                    supportUpload = true,
                    viewModel = viewModel
                )
            }
        }
    ) { paddingValues ->
        val lastUserMessageIndex = messages.indexOfFirst { it.senderId == me.userId }

        // The Messages List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            itemsIndexed(filteredMessages, key = { _, msg -> msg.id }) { index, msg ->
                val isFromMe = msg.senderId == me.userId
                
                val displayContent = remember(msg.content, msg.isEncrypted) {
                    if (msg.isEncrypted) viewModel.decryptMessage(msg.content) else msg.content
                }

                ChatBubble(
                    text = displayContent,
                    isFromMe = isFromMe,
                    isDelivered = msg.isDelivered,
                    showStatus = isFromMe && index == lastUserMessageIndex,
                    sharedComicId = msg.sharedComicId,
                    sharedId = msg.sharedId,
                    sharedType = msg.sharedType,
                    viewModel = viewModel
                )
            }
        }
    }
}