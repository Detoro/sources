package toro.sources.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import toro.sources.components.UserSearchDialog
import toro.sources.viewmodel.ChatViewModel

@Composable
fun FriendRequestPage(
    chatViewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val userSuggestions by chatViewModel.userSuggestions.collectAsState()
    
    UserSearchDialog(
        userSuggestions = userSuggestions,
        onSearch = { chatViewModel.searchUsers(it) },
        onClearSuggestions = { chatViewModel.clearUserSuggestions() },
        title = "Find Friends",
        onDismiss = onDismiss,
        onUserSelected = { selectedUser, _ ->
            chatViewModel.sendChatRequest(selectedUser.id) {
                onDismiss()
            }
        }
    )
}