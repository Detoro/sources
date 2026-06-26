package toro.sources.pages

import androidx.compose.runtime.Composable
import toro.sources.AppViewModel
import toro.sources.components.UserSearchDialog

@Composable
fun AuthorSearchPage(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    UserSearchDialog(
        viewModel = viewModel,
        title = "Find Authors",
        roles = emptyList(),
        onDismiss = onDismiss,
        onUserSelected = { selectedUser, _ ->
            viewModel.subscribeToAuthor(selectedUser.id)
            onDismiss()
        }
    )
}