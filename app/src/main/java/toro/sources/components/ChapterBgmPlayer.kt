package toro.sources.components

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun ChapterBgmPlayer(audioUrl: String?, isMuted: Boolean = false) {
    if (audioUrl.isNullOrBlank()) return

    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(audioUrl) {
        try {
            mediaPlayer.apply {
                setDataSource(audioUrl)
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    val volume = if (isMuted) 0f else 0.26f
                    mp.setVolume(volume, volume)
                    mp.start()
                }
            }
        } catch (e: Exception) {
            Log.e("BGMPlayer", "Failed to initialize MediaPlayer", e)
        }

        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                Log.e("BGMPlayer", "Error releasing MediaPlayer", e)
            }
        }
    }

    LaunchedEffect(isMuted) {
        if (mediaPlayer.isPlaying || mediaPlayer.trackInfo.isNotEmpty()) {
            if (isMuted) {
                mediaPlayer.setVolume(0f, 0f)
            } else {
                mediaPlayer.setVolume(1f, 0.26f)
            }
        }
    }
}