package toro.sources.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import toro.sources.AppViewModel

@Composable
fun AuthorsRow(
    viewModel: AppViewModel,
    onAddAuthorClick: () -> Unit
) {
    val authors by viewModel.subscribedAuthors.collectAsState()
    val selectedAuthorIds by viewModel.selectedAuthorIds.collectAsState()

    val ringColors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Magenta
    )

    LaunchedEffect(Unit) {
        viewModel.getSubscribedAuthors()
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clickable { onAddAuthorClick() },
                    contentAlignment = Alignment.Center
                ) {
                    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = strokeColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Author",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Add",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        itemsIndexed(authors) { index, user ->
            val isSelected = selectedAuthorIds.contains(user.id)
            val ringColor = ringColors[index % ringColors.size]
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(2.dp)
                        .clickable { viewModel.toggleAuthorFilter(user.id) },
                    contentAlignment = Alignment.Center
                ) {
                    // Ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = if (isSelected) ringColor else ringColor.copy(alpha = 0.5f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    
                    // Avatar
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (user.avatarUrl != null) {
                            DefaultAvatar(avatarUrl = user.avatarUrl)
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    user.username.first().toString().uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user.username,
                    fontSize = 12.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}