package toro.sources.pages

import androidx.compose.runtime.Composable
import toro.sources.AppViewModel
import toro.sources.components.UserSearchDialog

@Composable
fun FriendRequestPage(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    UserSearchDialog(
        viewModel = viewModel,
        title = "Find Friends",
        onDismiss = onDismiss,
        onUserSelected = { selectedUser, _ ->
            viewModel.sendChatRequest(selectedUser.id) {
                onDismiss()
            }
        }
    )
}