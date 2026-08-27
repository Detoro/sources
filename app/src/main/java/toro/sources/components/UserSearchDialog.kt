package toro.sources.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import models.Role
import toro.sources.models.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchDialog(
    userSuggestions: List<UserProfile>,
    onSearch: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    title: String,
    roles: List<Role> = emptyList(),
    onDismiss: () -> Unit,
    onUserSelected: (UserProfile, Role?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(roles.firstOrNull()) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            onSearch(searchQuery)
        } else {
            onClearSuggestions()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (roles.isNotEmpty() && selectedRole != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Assign Role:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.width(8.dp))

                        Box {
                            Surface(
                                onClick = { roleDropdownExpanded = true },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedRole?.name ?: "", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = roleDropdownExpanded,
                                onDismissRequest = { roleDropdownExpanded = false }
                            ) {
                                roles.forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role.name) },
                                        onClick = {
                                            selectedRole = role
                                            roleDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by username...") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Results List
                if (userSuggestions.isEmpty() && searchQuery.length >= 2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No users found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(userSuggestions) { user ->
                            UserSuggestionItem(
                                username = user.username,
                                onSendRequest = {
                                    onUserSelected(user, selectedRole)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}