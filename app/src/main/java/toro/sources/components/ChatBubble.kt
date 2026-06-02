package toro.sources.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import toro.sources.AppViewModel
import toro.sources.Screen
import com.toro.models.ShareType

@Composable
fun ChatBubble(
    text: String,
    isFromMe: Boolean,
    isDelivered: Boolean = false,
    showStatus: Boolean = false,
    sharedComicId: String? = null,
    sharedId: String? = null,
    sharedType: ShareType? = null,
    viewModel: AppViewModel? = null
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    val catalog by viewModel?.catalog?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    
    val actualSharedId = sharedId ?: sharedComicId
    val actualSharedType = if (sharedId != null) sharedType else if (sharedComicId != null) ShareType.COMIC else null

    val sharedComic = remember(actualSharedId, actualSharedType, catalog) {
        if (actualSharedType == ShareType.COMIC) catalog.find { it.id == actualSharedId } else null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
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
                                when (sharedType) {
                                    ShareType.COMIC -> {
                                        viewModel?.handleNavigation(Screen.Overview.createRoute(sharedId!!))
                                    }
                                    ShareType.POST -> {
                                        viewModel?.handleNavigation(Screen.PostComments.createRoute(sharedId!!))
                                    }
                                    ShareType.COMMENT -> {
                                        // In this case, we don't know the parent post ID from the sharedId alone easily
                                        // unless we fetch it. For now, route to comments if possible.
                                        viewModel?.handleNavigation(Screen.PostComments.createRoute(sharedId!!))
                                    }
                                    ShareType.USER -> {
                                        viewModel?.handleNavigation(Screen.Profile.createRoute(sharedId!!))
                                    }
                                    else -> {}
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (actualSharedType != null && actualSharedId != null) {
                        SharedContentPlaceholder(
                            type = actualSharedType,
                            onClick = {
                                if (viewModel != null) {
                                    when (actualSharedType) {
                                        ShareType.COMIC -> viewModel.handleNavigation(Screen.Overview.createRoute(actualSharedId))
                                        ShareType.POST -> viewModel.handleNavigation(Screen.PostComments.createRoute(actualSharedId))
                                        ShareType.COMMENT -> viewModel.handleNavigation(Screen.PostComments.createRoute(actualSharedId))
                                        ShareType.USER -> viewModel.handleNavigation(Screen.Profile.createRoute(actualSharedId))
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (text.isNotBlank()) {
                        val annotatedString = buildAnnotatedString {
                            val pattern = Regex("(@\\w+)|(#\\w+)")
                            val matches = pattern.findAll(text)
                            var lastIndex = 0
                            matches.forEach { match ->
                                append(text.substring(lastIndex, match.range.first))
                                val matchValue = match.value
                                pushStringAnnotation(
                                    tag = if (matchValue.startsWith("@")) "USER" else "COMIC",
                                    annotation = matchValue.drop(1)
                                )
                                withStyle(style = SpanStyle(
                                    color = if (isFromMe) Color.Cyan else MaterialTheme.colorScheme.primary,
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

                        ClickableText(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    // Fallback click for the whole bubble if it's a shared item
                                    if (actualSharedId != null && actualSharedType != null && viewModel != null) {
                                        when (actualSharedType) {
                                            ShareType.COMIC -> {
                                                viewModel.loadAndNavigateToSharedComic(actualSharedId)
                                            }
                                            ShareType.POST -> {
                                                viewModel.handleNavigation(Screen.PostComments.createRoute(actualSharedId))
                                            }
                                            ShareType.COMMENT -> {
                                                // Since we only have the comment ID (not the parent post/chapter ID),
                                                // routing to the main engagement feed is the safest fallback for now.
                                                viewModel.handleNavigation(Screen.Engagement.route)
                                            }
                                            ShareType.USER -> {
                                                viewModel.handleNavigation(Screen.Profile.createRoute(actualSharedId))
                                            }
                                        }
                                    }
                                },
                                onLongClick = { showOptionsMenu = true }
                            ),
                            onClick = { offset ->
                                var handled = false
                                annotatedString.getStringAnnotations(tag = "USER", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        viewModel?.findUserByUsername(annotation.item) { userId ->
                                            viewModel.handleNavigation(Screen.Profile.createRoute(userId))
                                        }
                                        handled = true
                                    }
                                
                                if (!handled) {
                                    annotatedString.getStringAnnotations(tag = "COMIC", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            viewModel?.findComicByTitle(annotation.item) { comic ->
                                                viewModel.setCurrentComic(comic)
                                                viewModel.handleNavigation(Screen.Overview.route)
                                            }
                                            handled = true
                                        }
                                }

                                // If no link was clicked, trigger the bubble's onClick
                                if (!handled && actualSharedId != null && actualSharedType != null && viewModel != null) {
                                    when (actualSharedType) {
                                        ShareType.COMIC -> {
                                            if (sharedComic != null) {
                                                viewModel.setCurrentComic(sharedComic)
                                                viewModel.handleNavigation(Screen.Overview.route)
                                            }
                                        }
                                        ShareType.POST -> viewModel.handleNavigation(Screen.Engagement.route)
                                        ShareType.COMMENT -> viewModel.handleNavigation(Screen.PostComments.createRoute(actualSharedId))
                                        ShareType.USER -> viewModel.getUserProfile(actualSharedId)
                                    }
                                }
                            }
                        )
                    }
                }

                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showOptionsMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showOptionsMenu = false }
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
