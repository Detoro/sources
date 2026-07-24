package toro.sources.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import toro.sources.R
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SuccessfulTaskPage(
    text: String,
    onTimeElapsed: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(3000.milliseconds)
        onTimeElapsed()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val image = painterResource(R.drawable.ic_launcher_background)
        Image(painter = image, contentDescription = null)
        Text(
            text = text,
            modifier = Modifier.padding(top = 24.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}