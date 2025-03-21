package com.tenesuzun.atvrnd.ui.screens

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.DefaultStrokeLineCap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun VideoProgressBar(
    player: ExoPlayer,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(player) {
        // Continuously update progress (e.g., every 100ms)
        while (true) {
            val duration = player.duration
            val currentPosition = player.currentPosition
            progress = if (duration > 0) currentPosition / duration.toFloat() else 0f
            delay(100L)
        }
    }

    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = Color.White,
        trackColor = Color.LightGray,
        gapSize = 0.dp,
        strokeCap = DefaultStrokeLineCap,
        drawStopIndicator = {}
    )
}