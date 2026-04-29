package toro.sources.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import toro.sources.AppViewModel
import toro.sources.DataModels.Comic

@Composable
fun SmartContentPage(comic: Comic, pageIndex: Int, viewModel: AppViewModel) {
    val pageData by produceState<Any?>(initialValue = null, key1 = pageIndex) {
        value = viewModel.getPageData(comic, pageIndex)
    }

    if (pageData != null) {
        AsyncImage(
            model = pageData,
            contentDescription = "Page $pageIndex",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    } else {
        CircularProgressIndicator()
    }
}