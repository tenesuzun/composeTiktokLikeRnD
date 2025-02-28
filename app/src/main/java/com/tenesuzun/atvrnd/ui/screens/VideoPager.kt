package com.tenesuzun.atvrnd.ui.screens

import android.app.Application
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tenesuzun.atvrnd.ui.components.VideoPerformanceMonitor
import com.tenesuzun.atvrnd.ui.components.VideoRepository
import com.tenesuzun.atvrnd.ui.components.VideoState
import com.tenesuzun.atvrnd.ui.viewmodels.VideoPagerViewModel

@OptIn(UnstableApi::class)
@Composable
fun VideoPager(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val performanceMonitor = remember { VideoPerformanceMonitor() }

    val viewModel: VideoPagerViewModel = viewModel { VideoPagerViewModel(application, VideoRepository(), performanceMonitor) }

    val videos by viewModel.videos.collectAsState()
    val networkQuality by viewModel.networkQuality.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val pagerState = rememberPagerState(pageCount = { videos.size })
    val scope = rememberCoroutineScope()

    val players = remember { mutableStateMapOf<Int, ExoPlayer>() }
    val preloadedPlayers = remember { mutableStateListOf<Int>() }
    val fullyLoadedPlayers = remember { mutableStateListOf<Int>() }


    DisposableEffect(Unit) {
        onDispose {
            players.values.forEach { it.release() }
            players.clear()
            preloadedPlayers.clear()
            fullyLoadedPlayers.clear()
        }
    }

    LaunchedEffect(pagerState.currentPage, networkQuality) {
        val currentPage = pagerState.currentPage
        val pagesToPreload = 5

        val startPage = (currentPage - pagesToPreload).coerceAtLeast(0)
        val endPage = (currentPage + pagesToPreload).coerceAtMost(videos.size - 1)

        // Cleanup old players
        viewModel.cleanupPlayers(players, currentPage, startPage, endPage)

        // Initialize or update players with current network quality
        (startPage..endPage).forEach { page ->
            if (!players.containsKey(page) && videos[page] is VideoState.Playing) {
                val videoState = videos[page] as VideoState.Playing

                performanceMonitor.startLoadingTimer(videoState.videoUrl)

                val isCurrentOrAdjacent = page == currentPage ||page == currentPage -1 || page == currentPage + 1
                val previewMode = !isCurrentOrAdjacent

                val player = viewModel.createPlayer(
                    context = context,
                    videoUrl = videoState.videoUrl,
                    networkQuality = networkQuality,
                    performanceMonitor = performanceMonitor,
                    previewMode = previewMode,
                    previewDurationMs = 3000
                    )

                players[page] = player

                if (previewMode) {
                    preloadedPlayers.add(page)
                } else {
                    fullyLoadedPlayers.add(page)
                }

            } else {
                // Update existing player's bitrate
                players[page]?.let { player ->
                    viewModel.updatePlayerQuality(player, networkQuality)

                    if (page == currentPage && preloadedPlayers.contains(page) && !fullyLoadedPlayers.contains(page)) {
                        viewModel.convertToFullPlayer(context, player, (videos[page] as? VideoState.Playing)?.videoUrl ?: "", networkQuality)
                        preloadedPlayers.remove(page)
                        fullyLoadedPlayers.add(page)
                    }
                }
            }
        }

        // Play/pause logic
        players.forEach { (page, player) ->
            if (page == currentPage) {
                player.play()
            } else {
                player.pause()
            }
        }
    }

    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize()
                .wrapContentSize(Alignment.Center)
        )
    } else {
        VerticalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
        ) { page ->
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxSize()
            ) {
                players[page]?.let { player ->
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                this.player = player
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                                controllerAutoShow = false
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}