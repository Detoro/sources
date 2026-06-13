package toro.sources.components

import androidx.compose.foundation.background
import android.content.ClipData
import android.content.Context
import android.content.ClipboardManager
import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.toro.models.ChatMessage
import toro.sources.AppViewModel
import toro.sources.Screen
import com.toro.models.ShareType

@Composable
fun ChatBubble(
    message: ChatMessage,
    isFromMe: Boolean,
    viewModel: AppViewModel,
    showStatus: Boolean = false,
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    val catalog by viewModel.catalog.collectAsState()
    val actualSharedId = message.sharedId ?: message.sharedComicId
    val actualSharedType = if (message.sharedId != null) message.sharedType else if (message.sharedComicId != null) ShareType.COMIC else null
    val context = LocalContext.current
    val isDelivered = message.isDelivered
    val text = remember(message.content, message.isEncrypted) {
        if (message.isEncrypted) viewModel.decryptMessage(message.content) else message.content
    }

    val sharedComic = remember(actualSharedId, actualSharedType, catalog) {
        if (actualSharedType == ShareType.COMIC) catalog.find { it.id == actualSharedId } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isFromMe) 16.dp else 4.dp,
                            bottomEnd = if (isFromMe) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    if (sharedComic != null) {
                        SharedComicCard(
                            sharedComic,
                            onClick = {
                                viewModel.handleNavigation(Screen.Overview.createRoute(message.sharedId!!))
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (actualSharedType != null && actualSharedId != null) {
                        SharedContentPlaceholder(
                            type = actualSharedType,
                            onClick = {
                                when (actualSharedType) {
                                    ShareType.COMIC -> viewModel.handleNavigation(Screen.Overview.createRoute(actualSharedId))
                                    ShareType.POST -> viewModel.handleNavigation(Screen.PostComments.createRoute(actualSharedId))
                                    ShareType.COMMENT -> viewModel.handleNavigation(Screen.PostComments.createRoute(actualSharedId))
                                    ShareType.USER -> viewModel.handleNavigation(Screen.Profile.createRoute(actualSharedId))
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (message.imageUrl != null) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Shared Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(bottom = 4.dp)
                        )
                    }

                    if (message.videoUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Video: ${message.videoUrl}", color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (text.isNotBlank()) {
                        val mentionColor = if (isFromMe) Color.Cyan else MaterialTheme.colorScheme.primary
                        val annotatedString = remember(text, isFromMe, mentionColor) {
                            buildAnnotatedString {
                                val pattern = Regex("@(\\w+(?:\\s\\w+)?)(?=\\s|$)")
                                val matches = pattern.findAll(text)
                                var lastIndex = 0
                                matches.forEach { match ->
                                    append(text.substring(lastIndex, match.range.first))
                                    val matchValue = match.value
                                    val username = matchValue.drop(1)
                                    
                                    pushStringAnnotation(
                                        tag = "USER",
                                        annotation = username
                                    )
                                    withStyle(style = SpanStyle(
                                        color = mentionColor,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )) {
                                        append(matchValue)
                                    }
                                    pop()
                                    lastIndex = match.range.last + 1
                                }
                                append(text.substring(lastIndex))
                            }
                        }
                        
                        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier.pointerInput(annotatedString) {
                                detectTapGestures(
                                    onTap = { pos ->
                                        val offset = textLayoutResult?.getOffsetForPosition(pos) ?: -1
                                        if (offset != -1) {
                                            annotatedString.getStringAnnotations("USER", offset, offset)
                                                .firstOrNull()?.let { annotation ->
                                                    viewModel.findUserByUsername(annotation.item) { userId ->
                                                        Log.i("annotated userid", userId)
                                                        viewModel.handleNavigation(Screen.Profile.createRoute(userId))
                                                    }
                                                    return@detectTapGestures
                                                }
                                        }

                                        // Fallback click for the whole bubble if it's a shared item
                                        if (actualSharedId != null && actualSharedType != null) {
                                            when (actualSharedType) {
                                                ShareType.COMIC -> {
                                                    viewModel.loadAndNavigateToSharedComic(actualSharedId)
                                                }
                                                ShareType.POST -> {
                                                    viewModel.handleNavigation(Screen.PostComments.createRoute(actualSharedId))
                                                }
                                                ShareType.COMMENT -> {
                                                    viewModel.handleNavigation(Screen.Engagement.route)
                                                }
                                                ShareType.USER -> {
                                                    viewModel.handleNavigation(Screen.Profile.createRoute(actualSharedId))
                                                }
                                            }
                                        }
                                    },
                                    onLongPress = { showOptionsMenu = true }
                                )
                            }
                        )
                    }
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = {
                            showOptionsMenu = false
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = ClipData.newPlainText("Copied Text", text)
                            clipboardManager.setPrimaryClip(clipData)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showOptionsMenu = false
                            viewModel.setEditingMessage(message)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
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
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isDelivered) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = if (isDelivered) "Delivered" else "Sent",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}