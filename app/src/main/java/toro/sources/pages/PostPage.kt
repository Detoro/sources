package toro.sources.pages

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import toro.sources.components.SmartInput
import toro.sources.components.SharedContentPlaceholder
import toro.sources.viewmodel.CommunityViewModel
import toro.sources.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostPage(
    communityViewModel: CommunityViewModel,
    sessionViewModel: SessionViewModel
) {
    var postTitle by remember { mutableStateOf("") }
    var postText by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val currentUser by sessionViewModel.userProfile.collectAsState()
    val sharedContent by sessionViewModel.sharedContent.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Make a Post", style = MaterialTheme.typography.titleLarge) },
                windowInsets = WindowInsets(top = 3.dp)
            )
        },
        bottomBar = {
            SmartInput(
                supportTitle = true,
                supportUpload = true,
                communityViewModel = communityViewModel,
                sessionViewModel = sessionViewModel,
                onSend = { title, text, mentions, _, attachment, isSpoiler ->
                    communityViewModel.makePost(
                        title = title,
                        isSpoiler = isSpoiler,
                        content = text,
                        tags = mentions,
                        attachment = attachment
                    )
                    sessionViewModel.setSharedContent(null)
                },
                onTitleChange = { postTitle = it },
                onValueChange = { title, text ->
                    postTitle = title ?: ""
                    postText = text
                },
                onTextChange = {
                    postText = it
                },
                onMediaSelected = {
                    selectedUri = it
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Preview",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentUser?.username?.take(1)?.uppercase() ?: "U",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = currentUser?.username ?: "User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(text = "Just now", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = postTitle.ifBlank { "Post Title" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (sharedContent != null) {
                        SharedContentPlaceholder(
                            type = sharedContent!!.type,
                            title = sharedContent!!.title,
                            previewText = sharedContent!!.previewText,
                            onClick = { },
                            clickable = false
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (selectedUri != null) {
                         AsyncImage(
                            model = selectedUri,
                            contentDescription = "Post Content",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = postText.ifBlank { "Waiting on your post..." },
                        fontSize = 14.sp,
                        color = if (postText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}