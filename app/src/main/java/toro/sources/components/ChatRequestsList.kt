package toro.sources.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import toro.sources.AppViewModel

@Composable
fun ChatRequestsList(
    viewModel: AppViewModel
) {
    val requests by viewModel.chatRequests.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(requests) { request ->
            ListItem(
                headlineContent = { Text(request.senderName, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Wants to connect with you") },
                leadingContent = { DefaultAvatar() },
                trailingContent = {
                    Row {
                        IconButton(onClick = { viewModel.acceptFriend(request.id)}) {
                            Icon(Icons.Default.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.declineFriend(request.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
            HorizontalDivider()
        }
    }
}