package toro.sources.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import toro.sources.AppViewModel
import toro.sources.DataModels.Comic

@Composable
fun SmartContentPage(comic: Comic, pageIndex: Int, viewModel: AppViewModel) {
    val pageData by produceState<Any?>(initialValue = null, key1 = pageIndex) {
        value = viewModel.getPageData(comic, pageIndex)
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    if (pageData != null) {
        AsyncImage(
            model = pageData,
            contentDescription = "Page $pageIndex",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
                .clipToBounds()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                        }
                    }
                )
            }.pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        // zooming between 1x and 4x
                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = newScale

                        if (scale > 1f) {
                            val maxX = (size.width * (scale - 1)) / 2
                            val maxY = (size.height * (scale - 1)) / 2

                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        } else {
                            offset = Offset.Zero
                        }
                    }
                }
        )
    } else {
        CircularProgressIndicator()
    }
}