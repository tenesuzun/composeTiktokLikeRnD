package com.tenesuzun.atvrnd.ui.screens

import android.app.Application
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.tenesuzun.atvrnd.ui.components.PlayerClass
import com.tenesuzun.atvrnd.ui.components.VideoRepository
import com.tenesuzun.atvrnd.ui.viewmodels.OperationViewModel
import kotlin.math.abs

@OptIn(UnstableApi::class)
@Composable
fun VideoPager(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val viewModel: OperationViewModel = viewModel { OperationViewModel(application, VideoRepository()) }
    val videos by viewModel.videos.collectAsState()

    val pagerState = rememberPagerState(pageCount = { videos.size })

    val playerClassList = remember { mutableStateMapOf<Int, PlayerClass>() }

    val sampleUsers = remember {
        listOf(
            Triple("johndoe", "Check out this amazing view! 🏞️ #travel #adventure #vacation", "45.2K"),
            Triple("travel_lover", "Beautiful sunset at the beach today! #sunset #beachlife #relax", "22.8K"),
            Triple("tech_geek", "Testing out the new camera features #technology #smartphone #photography", "33.1K"),
            Triple("fitness_guru", "Morning workout routine - join me! #fitness #workout #motivation", "51.7K"),
            Triple("foodie_delights", "Homemade pasta recipe - so delicious! #food #cooking #recipe", "18.3K")
        )
    }

    DisposableEffect(lifecycleOwner) { // uygulamanın arkaplanda videoları oynatmaması için
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.isAppInForeground = false
                    playerClassList[pagerState.currentPage]?.player?.pause()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.isAppInForeground = false
                    playerClassList[pagerState.currentPage]?.destroy()
                    playerClassList.clear()
                }

                Lifecycle.Event.ON_RESUME -> {
                    viewModel.isAppInForeground = true
                    playerClassList[pagerState.currentPage]?.player?.play()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // vertical pager üstünde scroll yönüne göre players map içinde instance oluşturulacak playerların hesaplanması
    LaunchedEffect(pagerState.currentPage, viewModel.downloadSpeedState) {
        if (viewModel.isAppInForeground){
            val currentPage = pagerState.currentPage
            val pagesToPreload = 5 // mevcut indeksin önünde ve gerisinde hazır tutulacak player sayısı
            val cleanupThreshold = pagesToPreload * 2

            val startPage = (currentPage - pagesToPreload).coerceAtLeast(0)
            val endPage = (currentPage + pagesToPreload).coerceAtMost(videos.size - 1)

            //cleanup
            if (playerClassList.size > cleanupThreshold) {
                playerClassList.keys.toList()
                    .filter { it !in (startPage..endPage) }
                    .sortedBy { abs(it - currentPage) }
                    .take(playerClassList.size - cleanupThreshold)
                    .forEach { page ->
                        playerClassList[page]?.destroy()
                        playerClassList.remove(page)
                    }
            }

            (startPage..endPage).forEach { page ->
                if (!playerClassList.containsKey(page)) {
                    val videoUrl = videos[page]
                    playerClassList[page] = PlayerClass(application, videoUrl, 3000, page)
                } else {
                    playerClassList[page]?.updatePlayerQuality(viewModel.calculateBitrateByDownloadSpeed())
                }
            }

            playerClassList.forEach { (page, vm) ->
                if (page == currentPage) {
                    vm.player?.play()
                } else {
                    vm.player?.pause()
                }
            }
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        Box(modifier = Modifier.fillMaxSize()) {
            playerClassList[page]?.player?.let { player ->
                AndroidView(
                    factory = {
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
                        .align(Alignment.Center)
                )
            }

            val userIndex = page % sampleUsers.size
            val (username, caption, likeCount) = sampleUsers[userIndex]
            val commentCount = "${(1000..5000).random()}"

            ReelsVideoPlayerOverlay(
                username = username,
                caption = caption,
                likeCount = likeCount,
                commentCount = commentCount,
            )

            playerClassList[page]?.player?.let { player ->
                VideoProgressBar(
                    player = player,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                )
            }

            Surface(color = Color.Transparent) {
                playerClassList[page]?.let {
                    if (it.thumbnailShownState) {
                        AsyncImage(
                            //model = "https://picsum.photos/1080/1920",
                            model = "https://fastly.picsum.photos/id/483/1080/1920.jpg?hmac=LNLgDQ4_MQtLPbAWO-YIST02WsMf7xXf6auFl9zwnO4",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (it.thumbnailShownState) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                color = Color.White,
                                trackColor = Color.LightGray,
                                modifier = Modifier
                                    .width(30.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                    /*Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { // hız ve sayfa no bilgisi
                        Text(
                            text = "Hız:${viewModel.downloadSpeedState}",
                            color = Color.Green,
                            modifier = Modifier
                                .padding(start = 15.dp, top = 30.dp)
                                .background(Color.Black)
                                .padding(5.dp),
                            fontSize = 21.sp
                        )
                        Text(
                            text = "Sayfa:$page",
                            color = Color.Green,
                            modifier = Modifier
                                .padding(end = 15.dp, top = 30.dp)
                                .background(Color.Black)
                                .padding(5.dp),
                            fontSize = 21.sp
                        )
                    }*/
                }
            }
        }
    }
}