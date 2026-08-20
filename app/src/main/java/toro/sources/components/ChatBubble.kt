package toro.sources.components

import android.content.ClipData
import android.content.Context
import android.content.ClipboardManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.toro.models.*
import toro.sources.Screen
import toro.sources.sharing.handleSharedNavigation
import toro.sources.viewmodel.ChatViewModel
import toro.sources.viewmodel.ComicsViewModel
import toro.sources.viewmodel.ProfileViewModel
import toro.sources.viewmodel.SessionViewModel

@Composable
private fun ChatImage(imageUrl: String) {
    AsyncImage(
        model = imageUrl,
        contentDescription = "Shared Image",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(bottom = 8.dp)
    )
}

@Composable
private fun ChatVideo(videoUrl: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = videoUrl,
            contentDescription = "Shared Video",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun ChatText(
    text: String,
    isFromMe: Boolean,
    isSpoiler: Boolean,
    content: MessageContent,
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    profileViewModel: ProfileViewModel,
    onLongPress: () -> Unit
) {
    val mentionColor = if (isFromMe) Color.White else MaterialTheme.colorScheme.primary
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        val mentionPattern = Regex("@(\\w+)")
        mentionPattern.findAll(text).forEach { match ->
            append(text.substring(lastIndex, match.range.first))
            val matchValue = match.value
            pushStringAnnotation(
                tag = "USER",
                annotation = matchValue.removePrefix("@")
            )
            withStyle(
                style = SpanStyle(
                    color = mentionColor,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(matchValue)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        append(text.substring(lastIndex))
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    SelectionContainer {
        var isRevealed by remember { mutableStateOf(false) }
        val blurRadius by animateFloatAsState(
            targetValue = if (!isSpoiler || isRevealed) 0f else 12f,
            label = "SpoilerBlur"
        )

        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            ),
            onTextLayout = { textLayoutResult = it },
            modifier = Modifier
                .blur(blurRadius.dp)
                .pointerInput(annotatedString) {
                    detectTapGestures(
                        onTap = { pos ->
                            if (isSpoiler && !isRevealed) {
                                isRevealed = true
                                return@detectTapGestures
                            }
                            val offset = textLayoutResult?.getOffsetForPosition(pos) ?: -1
                            if (offset != -1) {
                                annotatedString.getStringAnnotations("USER", offset, offset)
                                    .firstOrNull()?.let { annotation ->
                                        profileViewModel.findUserByUsername(annotation.item) { userId ->
                                            sessionViewModel.handleNavigation(Screen.Profile.createRoute(userId))
                                        }
                                        return@detectTapGestures
                                    }
                            }

                            if (content is MessageContent.Shared) {
                                handleSharedNavigation(
                                    id = content.id,
                                    type = content.type,
                                    comicsViewModel = comicsViewModel,
                                    sessionViewModel = sessionViewModel
                                )
                            } else if (isSpoiler) {
                                isRevealed = false
                            }
                        },
                        onLongPress = { _ -> onLongPress() }
                    )
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBubble(
    message: ChatMessage,
    conversationId: String,
    isFromMe: Boolean,
    chatViewModel: ChatViewModel,
    comicsViewModel: ComicsViewModel,
    sessionViewModel: SessionViewModel,
    profileViewModel: ProfileViewModel,
    showStatus: Boolean = false,
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    val content = remember(message) { message.toContent() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val text = remember(message.content) {
        message.content
    }

    val swipeState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.25f }
    )

    LaunchedEffect(swipeState.currentValue, conversationId) {
        if (swipeState.currentValue == SwipeToDismissBoxValue.StartToEnd) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            chatViewModel.setReplyTarget(message)
        }
        swipeState.reset()
    }

    SwipeToDismissBox(
        state = swipeState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
        ) {
            Column(
                horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (isFromMe) 20.dp else 4.dp,
                                bottomEnd = if (isFromMe) 4.dp else 20.dp
                            )
                        )
                        .background(
                            if (isFromMe) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        .combinedClickable(
                            onClick = {
                                when (content) {
                                    is MessageContent.Shared -> handleSharedNavigation(
                                        id = content.id,
                                        type = content.type,
                                        comicsViewModel = comicsViewModel,
                                        sessionViewModel = sessionViewModel
                                    )
                                    is MessageContent.Text,
                                    is MessageContent.TextWithMedia,
                                    is MessageContent.Image,
                                    is MessageContent.Video,
                                    is MessageContent.System -> Unit
                                }
                            },
                            onLongClick = { showOptionsMenu = true }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column {
                        when (content) {
                            is MessageContent.Shared -> {
                                SharedContentPlaceholder(
                                    type = content.type,
                                    title = "Shared ${content.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                    previewText = message.mediaType ?: "Tap to view details",
                                    imageUrl = message.imageUrls.firstOrNull(),
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    onClick = {
                                        handleSharedNavigation(
                                            id = content.id,
                                            type = content.type,
                                            comicsViewModel = comicsViewModel,
                                            sessionViewModel = sessionViewModel
                                        )
                                    }
                                )
                            }
                            is MessageContent.Image -> {
                                content.urls.forEach { imageUrl ->
                                    ChatImage(imageUrl)
                                }
                            }
                            is MessageContent.Video -> {
                                content.urls.forEach { videoUrl ->
                                    ChatVideo(videoUrl)
                                }
                            }
                            is MessageContent.TextWithMedia -> {
                                content.imageUrls.forEach { ChatImage(it) }
                                content.videoUrls.forEach { ChatVideo(it) }
                                ChatText(
                                    text = content.body,
                                    isFromMe = isFromMe,
                                    isSpoiler = message.isSpoiler,
                                    content = content,
                                    comicsViewModel = comicsViewModel,
                                    sessionViewModel = sessionViewModel,
                                    profileViewModel = profileViewModel,
                                    onLongPress = { showOptionsMenu = true }
                                )
                            }
                            is MessageContent.Text -> {
                                if (content.body.isNotEmpty()) {
                                    ChatText(
                                        text = content.body,
                                        isFromMe = isFromMe,
                                        isSpoiler = message.isSpoiler,
                                        content = content,
                                        comicsViewModel = comicsViewModel,
                                        sessionViewModel = sessionViewModel,
                                        profileViewModel = profileViewModel,
                                        onLongPress = { showOptionsMenu = true }
                                    )
                                }
                            }
                            is MessageContent.System -> Unit
                        }
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            onClick = {
                                showOptionsMenu = false
                                val clipboardManager =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Copied Text",
                                        text
                                    )
                                )
                            }
                        )
                        if (isFromMe) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showOptionsMenu = false
                                    chatViewModel.setEditingMessage(message)
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOptionsMenu = false
                                    chatViewModel.deleteMessage(message.conversationId, message.id)
                                }
                            )
                        }
                    }
                }
                if (isFromMe && showStatus) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                    ) {
                        val icon = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done
                        val tint = when {
                            message.isRead -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = when {
                                message.isRead -> "Read"
                                message.isDelivered -> "Delivered"
                                else -> "Sent"
                            },
                            modifier = Modifier.size(16.dp),
                            tint = tint
                        )
                    }
                }
            }
        }
    }
}