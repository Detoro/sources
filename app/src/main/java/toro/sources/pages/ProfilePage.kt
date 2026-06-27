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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.toro.models.Conversation
import toro.sources.Screen
import toro.sources.AppViewModel
import toro.sources.components.PostCard
import toro.sources.components.ComicCoverCard
import com.toro.models.SharedContent
import com.toro.models.ShareType
import toro.sources.components.DefaultAvatar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
fun ProfilePage(
    viewModel: AppViewModel,
    userId: String? = null,
    onSettingsClick: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val me by viewModel.userProfile.collectAsState()
    val targetUserId = userId ?: me?.id
    val isMyProfile = targetUserId == me?.id

    val userProfile by (if (isMyProfile) viewModel.userProfile else viewModel.targetUserProfile).collectAsState()
    val userPosts by (if (isMyProfile) viewModel.userPosts else viewModel.targetUserPosts).collectAsState()
    val userPostsCount = userPosts.size
    val userWorks by (if (isMyProfile) viewModel.userWorks else viewModel.targetUserWorks).collectAsState()
    val userWorksCount = userWorks.size
    val userFriends by viewModel.inbox.collectAsState()
    val userFriendsCount = userFriends.size

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBioDialog by remember { mutableStateOf(false) }
    var newBio by remember { mutableStateOf("") }

    LaunchedEffect(targetUserId) {
        if (targetUserId?.isNotEmpty() == true) {
            viewModel.getUserProfile(targetUserId)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.uploadAvatar(it) }
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
                    userProfile?.bio = newBio
                    viewModel.updateBio(newBio)
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
            LargeTopAppBar(
                title = { Text(if (isMyProfile) "My Profile" else "Profile") },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (isMyProfile) {
                        IconButton(onClick = { onSettingsClick() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
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
                                ProfileStat("Friends", profile.friendsCount)
                                if (profile.isAuthor) ProfileStat("Works", profile.worksCount)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val tabs = mutableListOf("Posts", "Friends")
                    if (profile.isAuthor) tabs.add("Works")

                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
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
                                            0 -> userPostsCount
                                            1 -> userFriendsCount
                                            2 -> userWorksCount
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

                    Box(modifier = Modifier.weight(1f)) {
                        when (tabs[selectedTab]) {
                            "Posts" -> {
                                if (userPosts.isEmpty()) {
                                    EmptyState("No posts yet.")
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(userPosts) { post ->
                                            PostCard(
                                                viewModel = viewModel,
                                                post = post,
                                                onCommentClick = { viewModel.handleNavigation(Screen.PostComments.createRoute(post.id)) },
                                                onAuthorClick = { userId ->
                                                    if (userId != targetUserId) viewModel.handleNavigation(Screen.Profile.createRoute(userId))
                                                },
                                                onShareClick = { p ->
                                                    viewModel.setSharedContent(
                                                        SharedContent(
                                                            id = p.id,
                                                            type = ShareType.POST,
                                                            title = p.title ?: "Post by ${p.authorName}",
                                                            previewText = p.content.take(50)
                                                        )
                                                    )
                                                    viewModel.showShareDialog(true)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            "Works" -> {
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
                                            ComicCoverCard(comic, viewModel, onClick = {})
                                        }
                                    }
                                }
                            }

                            "Friends" -> {
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
                                                onChatClick = { viewModel.handleNavigation("Screen.Chat.route/${chat.conversationId}") },
                                                onProfileClick = { viewModel.getUserProfile(chat.otherUserId) }
                                            )
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
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
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
                if (friend.otherUserAvatarUrl != null) {
                    DefaultAvatar(
                        avatarUrl = friend.otherUserAvatarUrl,
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = friend.otherUserName.first().uppercase(),
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
                    text = friend.otherUserName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = "Friend Profile",
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
}