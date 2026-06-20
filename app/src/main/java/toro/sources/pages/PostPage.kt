package toro.sources.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import toro.sources.AppViewModel
import toro.sources.components.SmartInput
import toro.sources.components.SharedContentPlaceholder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostPage(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    var postTitle by remember { mutableStateOf("") }
    var postText by remember { mutableStateOf("") }
    val currentUser by viewModel.currentUser.collectAsState()
    val sharedContent by viewModel.sharedContent.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Make a Post", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(top = 3.dp)
            )
        },
        bottomBar = {
            SmartInput(
                supportTitle = true,
                supportUpload = true,
                viewModel = viewModel,
                onSend = { title, text, mentions, _, _, isSpoiler ->
                    viewModel.makePost(
                        title = title,
                        isSpoiler = isSpoiler,
                        postContent = text,
                        tags = emptyList()
                    )
                },
                onTitleChange = { postTitle = it },
                onValueChange = { title, text ->
                    postTitle = title ?: ""
                    postText = text
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
                .verticalScroll(scrollState),
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
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(currentUser.username.take(1).uppercase(), fontSize = 14.sp, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = currentUser.username, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            Text(text = "Just now", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = postTitle.ifBlank { "Post Title" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (sharedContent != null) {
                        SharedContentPlaceholder(
                            type = sharedContent!!.type,
                            title = "Nothing",
                            previewText = "Next to nothing",
                            onClick = { /* No-op in post preview */ }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = postText.ifBlank { "Waiting on your post..." },
                        fontSize = 14.sp,
                        color = if (postText.isBlank()) Color.Gray else Color.White
                    )
                }
            }
        }
    }
}