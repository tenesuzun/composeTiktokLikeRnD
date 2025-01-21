package com.tenesuzun.atvrnd.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tenesuzun.atvrnd.domain.m3u8List
import com.tenesuzun.atvrnd.domain.mp4List
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun VideoPager() {
    val context = LocalContext.current
//    val pagerState = rememberPagerState(pageCount = { m3u8List.size })
    val pagerState = rememberPagerState(pageCount = { mp4List.size })
    val scope = rememberCoroutineScope()

    val players = remember {
        mutableStateMapOf<Int, ExoPlayer>()
    }

    DisposableEffect(Unit) {
        onDispose {
            players.values.forEach { it.release() }
            players.clear()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        val pagesToPreload = 5

        val startPage = (currentPage - pagesToPreload).coerceAtLeast(0)
        val endPage = (currentPage + pagesToPreload).coerceAtMost(mp4List.size - 1)
//        val endPage = (currentPage + pagesToPreload).coerceAtMost(m3u8List.size - 1)

        val cleanupThreshold = 10
        if (players.size > cleanupThreshold) {
            players.keys.toList()
                .filter { it !in (startPage..endPage) }
                .sortedBy { abs(it - currentPage) }
                .take(players.size - cleanupThreshold)
                .forEach { page ->
                    players[page]?.release()
                    players.remove(page)
                }
        }

        (startPage..endPage).forEach { page ->
            if (!players.containsKey(page)) {
                val player = ExoPlayer.Builder(context).build().apply {
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    playWhenReady = true
                    setMediaItem(MediaItem.fromUri(mp4List[page]))
//                    setMediaItem(MediaItem.fromUri(m3u8List[page]))
                    prepare()
                }
                players[page] = player
            }
        }

        players.forEach { (page, player) ->
            if (page == currentPage) {
                player.play()
            } else {
                player.pause()
            }
        }
    }

    LaunchedEffect(mp4List.size) {
//    LaunchedEffect(m3u8List.size) {
        players.keys.toList()
            .filter { it >= mp4List.size }
//            .filter { it >= m3u8List.size }
            .forEach { page ->
                players[page]?.release()
                players.remove(page)
            }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            players[page]?.let { player ->
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            this.player = player
                            useController = true // false olursa hiç gözükmüyor
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            controllerAutoShow = false
                            hideController()
                        }
                    }, modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}