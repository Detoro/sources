package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import toro.sources.AppViewModel

@Composable
fun ActiveChatsList(
    viewModel: AppViewModel,
    onChatClick: (String) -> Unit,
    onProfileClick: (String) -> Unit
) {
    val activeChats by viewModel.filteredInbox.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(activeChats) { chat ->
            val conversationId = chat.conversationId
            val username = chat.otherUserName
            val otherUserId = chat.otherUserId
            val lastMessage = chat.lastMessage ?: ""

            ListItem(
                headlineContent = { Text(username, fontWeight = FontWeight.Bold) },
                supportingContent = {
                    Text(lastMessage, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = { 
                    Box(modifier = Modifier.clickable { onProfileClick(otherUserId) }) {
                        DefaultAvatar()
                    }
                },
                modifier = Modifier.clickable { onChatClick(conversationId) }
            )
        }
    }
}