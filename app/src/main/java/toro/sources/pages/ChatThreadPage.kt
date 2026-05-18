package toro.sources.pages

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import toro.sources.AppViewModel
import toro.sources.components.ChatBubble
import toro.sources.components.SmartInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadPage(
    targetUserId: String,
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val me by viewModel.currentUser.collectAsState()

    LaunchedEffect(targetUserId) {
        viewModel.getChatMessages(targetUserId)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Chatting with $targetUserId") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            SmartInput(
                onSend = { text, _, _, _ ->
                    viewModel.sendMessage(targetUserId, text)
                },
                placeholder = "Type a message...",
                supportUpload = true,
                viewModel = viewModel
            )
        }
    ) { paddingValues ->
        // The Messages List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            reverseLayout = true
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    text = msg.content,
                    isFromMe = msg.senderId == me.userId)
            }
        }
    }
}