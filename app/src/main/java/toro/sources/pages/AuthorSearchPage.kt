package toro.sources.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import toro.sources.components.UserSearchDialog
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.viewmodel.ComicsViewModel

@Composable
fun AuthorSearchPage(
    communityViewModel: CommunityViewModel,
    comicsViewModel: ComicsViewModel,
    onDismiss: () -> Unit
) {
    val userSuggestions by communityViewModel.userSuggestions.collectAsState()
    
    UserSearchDialog(
        userSuggestions = userSuggestions,
        onSearch = { communityViewModel.searchUsers(it) },
        onClearSuggestions = { communityViewModel.clearUserSuggestions() },
        title = "Find Authors",
        roles = emptyList(),
        onDismiss = onDismiss,
        onUserSelected = { selectedUser, _ ->
            comicsViewModel.subscribeToAuthor(selectedUser.id)
            onDismiss()
        }
    )
}