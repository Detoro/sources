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
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.toro.models.ChatMessage
import com.toro.models.ShareType
import kotlinx.coroutines.launch
import toro.sources.AppViewModel
import toro.sources.Screen
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ChatBubble(
    message: ChatMessage,
    isFromMe: Boolean,
    viewModel: AppViewModel,
    showStatus: Boolean = false,
    threadMessages: List<ChatMessage> = emptyList(),
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    val actualSharedId = message.sharedId ?: message.sharedComicId
    val actualSharedType = if (message.sharedId != null) message.sharedType else if (message.sharedComicId != null) ShareType.COMIC else null
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val text = remember(message.content, message.isEncrypted) {
        if (message.isEncrypted) viewModel.decryptMessage(message.content) else message.content
    }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.2f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        onDismiss = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setReplyTarget(message)

                coroutineScope.launch {
                    dismissState.reset()
                }
            }
        },
        backgroundContent = {}
    ){
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
                                if (actualSharedId != null && actualSharedType != null) {
                                    when (actualSharedType) {
                                        ShareType.COMIC -> viewModel.loadAndNavigateToComic(
                                            actualSharedId
                                        )

                                        ShareType.POST -> viewModel.handleNavigation(
                                            Screen.PostComments.createRoute(
                                                actualSharedId
                                            )
                                        )

                                        ShareType.COMMENT -> viewModel.handleNavigation(Screen.Engagement.route)
                                        ShareType.USER -> viewModel.handleNavigation(
                                            Screen.Profile.createRoute(
                                                actualSharedId
                                            )
                                        )
                                    }
                                }
                            },
                            onLongClick = { showOptionsMenu = true }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column {
                        if (message.replyToMessageId != null) {
                            val repliedMessage = threadMessages.find { it.id == message.replyToMessageId }

                            if (repliedMessage != null) {
                                val rawDecrypted = if (repliedMessage.isEncrypted) viewModel.decryptMessage(repliedMessage.content) else repliedMessage.content
                                val cleanDecryptedText = rawDecrypted.replace("\u0000", "").trim()

                                val repliedSharedType = if (repliedMessage.sharedId != null) repliedMessage.sharedType else if (repliedMessage.sharedComicId != null) ShareType.COMIC else null

                                val replyDisplay = when {
                                    repliedMessage.isSpoiler -> "Spoiler detected"
                                    cleanDecryptedText.isNotEmpty() && cleanDecryptedText != "null" -> cleanDecryptedText
                                    repliedMessage.imageUrls.isNotEmpty() -> "Shared Image"
                                    repliedMessage.videoUrls.isNotEmpty() -> "Shared Video"
                                    repliedSharedType != null -> "Shared ${repliedSharedType.name.lowercase().replaceFirstChar { it.uppercase() }}"
                                    repliedMessage.sharedId != null || repliedMessage.sharedComicId != null -> "Shared Content"
                                    else -> "Attachment"
                                }

                                val myUserId = viewModel.userProfile.collectAsState().value?.id
                                val isReplyToMe = repliedMessage.senderId == myUserId

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isReplyToMe) "You" else "Replied Message",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = replyDisplay,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "Message deleted",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        if (actualSharedType != null && actualSharedId != null) {
                            SharedContentPlaceholder(
                                type = actualSharedType,
                                title = message.sharedType?.name
                                    ?: "Shared ${actualSharedType.name.lowercase()}",
                                previewText = message.mediaType ?: "Tap to view details",
                                imageUrl = message.imageUrls.firstOrNull(),
                                modifier = Modifier.padding(bottom = 8.dp),
                                onClick = {
                                    when (actualSharedType) {
                                        ShareType.COMIC -> viewModel.loadAndNavigateToComic(
                                            actualSharedId
                                        )

                                        ShareType.POST -> viewModel.handleNavigation(
                                            Screen.PostComments.createRoute(
                                                actualSharedId
                                            )
                                        )

                                        ShareType.COMMENT -> viewModel.handleNavigation(
                                            Screen.PostComments.createRoute(
                                                actualSharedId
                                            )
                                        )

                                        ShareType.USER -> viewModel.handleNavigation(
                                            Screen.Profile.createRoute(
                                                actualSharedId
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        message.imageUrls.forEach { imageUrl ->
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Shared Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 8.dp)
                            )
                        }

                        message.videoUrls.forEach { videoUrl ->
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

                        if (text.isNotEmpty()) {
                            val mentionColor =
                                if (isFromMe) Color.White else MaterialTheme.colorScheme.primary
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

                            var textLayoutResult by remember {
                                mutableStateOf<TextLayoutResult?>(
                                    null
                                )
                            }

                            SelectionContainer {
                                var isRevealed by remember { mutableStateOf(false) }
                                val blurRadius by animateFloatAsState(
                                    targetValue = if (!message.isSpoiler || isRevealed) 0f else 12f,
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
                                                    if (message.isSpoiler && !isRevealed) {
                                                        isRevealed = true
                                                        return@detectTapGestures
                                                    }
                                                    val offset =
                                                        textLayoutResult?.getOffsetForPosition(pos)
                                                            ?: -1
                                                    if (offset != -1) {
                                                        annotatedString.getStringAnnotations(
                                                            "USER",
                                                            offset,
                                                            offset
                                                        )
                                                            .firstOrNull()?.let { annotation ->
                                                                viewModel.findUserByUsername(
                                                                    annotation.item
                                                                ) { userId ->
                                                                    viewModel.handleNavigation(
                                                                        Screen.Profile.createRoute(
                                                                            userId
                                                                        )
                                                                    )
                                                                }
                                                                return@detectTapGestures
                                                            }
                                                    }

                                                    if (actualSharedId != null && actualSharedType != null) {
                                                        when (actualSharedType) {
                                                            ShareType.COMIC -> viewModel.loadAndNavigateToComic(
                                                                actualSharedId
                                                            )

                                                            ShareType.POST -> viewModel.handleNavigation(
                                                                Screen.PostComments.createRoute(
                                                                    actualSharedId
                                                                )
                                                            )

                                                            ShareType.COMMENT -> viewModel.handleNavigation(
                                                                Screen.Engagement.route
                                                            )

                                                            ShareType.USER -> viewModel.handleNavigation(
                                                                Screen.Profile.createRoute(
                                                                    actualSharedId
                                                                )
                                                            )
                                                        }
                                                    } else if (message.isSpoiler) {
                                                        isRevealed = false
                                                    }
                                                },
                                                onLongPress = { showOptionsMenu = true }
                                            )
                                        }
                                )
                            }
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
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showOptionsMenu = false
                                viewModel.setEditingMessage(message)
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showOptionsMenu = false
                                viewModel.deleteMessage(message.conversationId, message.id)
                            }
                        )
                    }
                }
                if (isFromMe && showStatus) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, end = 8.dp)
                    ) {
                        val icon = when {
                            message.isRead -> Icons.Default.DoneAll
                            message.isDelivered -> Icons.Default.DoneAll
                            else -> Icons.Default.Done
                        }
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