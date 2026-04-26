package toro.sources.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ActiveChatsList(onChatClick: (String) -> Unit) {
    // Mock Data
    val activeChats = listOf(
        Pair("user_789", "SarahReader" to "Did you finish chapter 4 yet?!"),
        Pair("user_456", "ComicNerd99" to "Yeah, the artwork is insane.")
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(activeChats) { chat ->
            val userId = chat.first
            val username = chat.second.first
            val lastMessage = chat.second.second

            ListItem(
                headlineContent = { Text(username, fontWeight = FontWeight.Bold) },
                supportingContent = {
                    Text(lastMessage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = { DefaultAvatar() },
                modifier = Modifier.clickable { onChatClick(userId) }
            )
            HorizontalDivider()
        }
    }
}