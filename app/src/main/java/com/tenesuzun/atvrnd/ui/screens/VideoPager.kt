package com.tenesuzun.atvrnd.ui.screens

import android.app.Application
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tenesuzun.atvrnd.ui.components.VideoPerformanceMonitor
import com.tenesuzun.atvrnd.ui.components.VideoRepository
import com.tenesuzun.atvrnd.ui.components.VideoState
import com.tenesuzun.atvrnd.ui.viewmodels.VideoPagerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPager(
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val performanceMonitor = remember { VideoPerformanceMonitor() }

    val viewModel: VideoPagerViewModel = viewModel { VideoPagerViewModel(application, VideoRepository(), performanceMonitor) }

    val videos by viewModel.videos.collectAsState()
    val networkQuality by viewModel.networkQuality.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()

    val pagerState = rememberPagerState(pageCount = { videos.size })
    val scope = rememberCoroutineScope()

    val players = remember { mutableStateMapOf<Int, ExoPlayer>() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    players.keys.forEach {
                        if (it == pagerState.currentPage) {
                            players[it]?.pause()
                        }
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    players.keys.forEach {
                        if (it == pagerState.currentPage) {
                            players[it]?.pause()
                        }
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    players.values.forEach { it.release() }
                    players.clear()
                }
                Lifecycle.Event.ON_RESUME -> {
                    players.keys.forEach {
                        if (it == pagerState.currentPage) {
                            players[it]?.play()
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(pagerState.currentPage, networkQuality) {
        val currentPage = pagerState.currentPage
        val pagesToPreload = 10

        val startPage = (currentPage - pagesToPreload).coerceAtLeast(0)
        val endPage = (currentPage + pagesToPreload).coerceAtMost(videos.size - 1)

        // Cleanup old players
        viewModel.cleanupPlayers(players, currentPage, startPage, endPage)

        // Initialize or update players with current network quality
        (startPage..endPage).forEach { page ->
            if (!players.containsKey(page) && videos[page] is VideoState.Playing) {
                val videoState = videos[page] as VideoState.Playing

                performanceMonitor.startLoadingTimer(videoState.videoUrl)

                val player = viewModel.createPlayer(
                    context = context,
                    videoUrl = videoState.videoUrl,
                    networkQuality = networkQuality,
                    performanceMonitor = performanceMonitor,
                    previewDurationMs = 3000
                    )

                players[page] = player
            } else {
                // Update existing player's bitrate
                players[page]?.let { player ->
                    viewModel.updatePlayerQuality(player, networkQuality)
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

    var userScrollEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        scope.launch(Dispatchers.IO) {
            userScrollEnabled = false
            delay(500)
            userScrollEnabled = true
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = userScrollEnabled
    ) { page ->
        pagerState.currentPageOffsetFraction
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                trackColor = Color.LightGray,
                modifier = Modifier
                    .fillMaxSize()
//                    .background(Color.Black)
                    .wrapContentSize(Alignment.Center)
            )
        } else {
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxSize()
            ) {
                players[page]?.let { player ->
                    Box(modifier = Modifier.fillMaxSize()) {
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
//                        if (isBuffering) {
//                            CircularProgressIndicator(
//                                color = Color.White,
//                                trackColor = Color.Transparent,
//                                modifier = Modifier.align(Alignment.Center)
//                            )
//                        }
                    }
                }
            }
        }
    }
}