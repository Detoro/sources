package toro.sources.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import toro.sources.viewmodel.ProfileViewModel
import toro.sources.Screen
import toro.sources.viewmodel.SessionViewModel
import toro.sources.viewmodel.ChatViewModel
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.components.ComicCoverCard
import toro.sources.viewmodel.ComicsViewModel
import androidx.compose.runtime.collectAsState
import toro.sources.models.Conversation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun ProfilePage(
    profileViewModel: ProfileViewModel,
    sessionViewModel: SessionViewModel,
    communityViewModel: CommunityViewModel,
    comicsViewModel: ComicsViewModel,
    chatViewModel: ChatViewModel,
    userId: String? = null,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val me by sessionViewModel.userProfile.collectAsState()
    val targetUserId = userId ?: me?.id
    val isMyProfile = targetUserId == me?.id

    val userProfile by (if (isMyProfile) sessionViewModel.userProfile else profileViewModel.targetUserProfile).collectAsState()
    val userWorks by (if (isMyProfile) profileViewModel.userWorks else profileViewModel.targetUserWorks).collectAsState()
    val userFriends by chatViewModel.inbox.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBioDialog by remember { mutableStateOf(false) }
    var newBio by remember { mutableStateOf("") }

    LaunchedEffect(targetUserId) {
        if (targetUserId?.isNotEmpty() == true) {
            profileViewModel.getUserProfile(targetUserId)
            chatViewModel.getInbox()
            communityViewModel.getCommunityPosts()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { profileViewModel.uploadAvatar(it) }
        }
    )

    if (showBioDialog && isMyProfile) {
        AlertDialog(
            onDismissRequest = { showBioDialog = false },
            title = { Text("Update Bio") },
            text = {
                TextField(
                    value = newBio,
                    onValueChange = { newBio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    profileViewModel.updateBio(newBio)
                    showBioDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBioDialog = false }) { Text("Cancel") }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isMyProfile) "My Profile" else "Profile") },
                actions = {
                    if (isMyProfile) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                ),
                windowInsets = WindowInsets(top = 3.dp)
            )
        }
    ) { paddingValues ->
        if (userProfile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val profile = userProfile!!
            val canSeeProfile = !profile.isPrivate || isMyProfile || profile.isFollowing

            if (!canSeeProfile) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Private Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Follow this user to see their activity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable(enabled = isMyProfile) {
                                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarUrl = if (isMyProfile) me?.avatarUrl else profile.avatarUrl
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Profile Picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(54.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = profile.username,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            if (profile.isAuthor) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "AUTHOR",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }

                            Text(
                                text = profile.bio ?: "No bio yet. Tap to add one!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable(enabled = isMyProfile) {
                                        newBio = profile.bio ?: ""
                                        showBioDialog = true
                                    }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ProfileStat("Posts", profile.postsCount)
                                if (profile.isAuthor) ProfileStat("Followers", profile.followersCount)
                                ProfileStat("Friends", userFriends.size)
                                if (profile.isAuthor) ProfileStat("Works", profile.worksCount)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val tabs = mutableListOf("Friends")
                    if (profile.isAuthor) tabs.add("Works")
                    val pagerState = rememberPagerState(pageCount = { tabs.size })
                    val coroutineScope = rememberCoroutineScope()

                    SecondaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        val count = when (index) {
                                            0 -> userFriends.size
                                            1 -> userWorks.size
                                            else -> 0
                                        }

                                        if (count > 0) {
                                            Badge(
                                                contentColor = Color.DarkGray
                                            ) {
                                                Text(count.toString())

                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        when (page) {
                            0 -> {
                                if (userFriends.isEmpty()) {
                                    EmptyState("No Friends to see")
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(userFriends) { chat ->
                                            FriendCard(
                                                friend = chat,
                                                onChatClick = { sessionViewModel.handleNavigation(Screen.Chat.createRoute(chat.conversationId)) },
                                                onProfileClick = { profileViewModel.getUserProfile(chat.otherUser.userId) }
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> {
                                if (userWorks.isEmpty()) {
                                    EmptyState("No works published yet.")
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        contentPadding = PaddingValues(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(userWorks) { comic ->
                                            ComicCoverCard(comic, comicsViewModel, sessionViewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun FriendCard(
    friend: Conversation,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                if (friend.otherUser.avatarUrl != null) {
                    AsyncImage(
                        model = friend.otherUser.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = friend.otherUser.username.firstOrNull()?.uppercase() ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.otherUser.username,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val lastMsg = friend.lastMessage
                if (lastMsg != null) {
                    Text(
                        text = lastMsg.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onProfileClick) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onChatClick) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Message,
                        contentDescription = "Chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}