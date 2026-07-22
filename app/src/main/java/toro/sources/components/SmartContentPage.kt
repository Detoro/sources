package toro.sources.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun SmartContentPage(pageIndex: Int, pageData: Any?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f),
        contentAlignment = Alignment.Center
    ) {
        if (pageData != null) {
            AsyncImage(
                model = pageData,
                contentDescription = "Page $pageIndex",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth(),
                onLoading = { /* Show something? */ },
                onSuccess = { /* Done */ }
            )
        } else {
            CircularProgressIndicator()
        }
    }
}