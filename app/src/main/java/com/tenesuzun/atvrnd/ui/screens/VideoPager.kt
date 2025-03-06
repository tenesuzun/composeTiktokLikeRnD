package com.tenesuzun.atvrnd.ui.screens

import android.app.Application
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
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

    // Sample user data (in a real app, this would come from your data model)
    val sampleUsers = remember {
        listOf(
            Triple("johndoe", "Check out this amazing view! 🏞️ #travel #adventure #vacation", "45.2K"),
            Triple("travel_lover", "Beautiful sunset at the beach today! #sunset #beachlife #relax", "22.8K"),
            Triple("tech_geek", "Testing out the new camera features #technology #smartphone #photography", "33.1K"),
            Triple("fitness_guru", "Morning workout routine - join me! #fitness #workout #motivation", "51.7K"),
            Triple("foodie_delights", "Homemade pasta recipe - so delicious! #food #cooking #recipe", "18.3K")
        )
    }

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

        viewModel.cleanupPlayers(players, currentPage, startPage, endPage)

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
                players[page]?.let { player ->
                    viewModel.updatePlayerQuality(player, networkQuality)
                }
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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxSize()
            ) {
                players[page]?.let { player ->
//                    var isPlayerLoading by remember { mutableStateOf(true) }
//
//                    player.addListener(object : Player.Listener {
//                        override fun onPlaybackStateChanged(playbackState: Int) {
//                            when (playbackState) {
//                                Player.STATE_READY -> {
//                                    isPlayerLoading = false
//                                }
//
//                                Player.STATE_BUFFERING -> {
//                                    isPlayerLoading = false
//                                }
//
//                                Player.STATE_IDLE -> {
//                                    isPlayerLoading = true
//                                }
//
//                                else -> {}
//                            }
//                        }
//                    })

                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                this.player = player
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                controllerAutoShow = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(9f / 21f)
                            .clickable(
                                enabled = !pagerState.isScrollInProgress,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (player.isPlaying) {
                                    player.pause()
                                } else {
                                    player.play()
                                }
                            }
                    )
//                    if (isPlayerLoading) {
//                        CircularProgressIndicator(
//                            modifier = Modifier
//                                .size(25.dp)
//                                .align(Alignment.Center),
//                            color = Color.White,
//                            trackColor = Color.LightGray,
//                        )
//                    }
                }
            }
            val userIndex = page % sampleUsers.size
            val (username, caption, likeCount) = sampleUsers[userIndex]
            val commentCount = "${(1000..5000).random()}"

            ReelsVideoPlayerOverlay(
                username = username,
                caption = caption,
                likeCount = likeCount,
                commentCount = commentCount
            )
        }
    }
}