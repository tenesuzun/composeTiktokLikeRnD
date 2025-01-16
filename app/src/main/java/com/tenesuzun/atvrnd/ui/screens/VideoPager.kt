package com.tenesuzun.atvrnd.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tenesuzun.atvrnd.domain.m3u8List
import com.tenesuzun.atvrnd.ui.components.VideoPlayer
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun VideoPager() {
    val pagerState = rememberPagerState(pageCount = { m3u8List.size })
    val scope = rememberCoroutineScope()

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 5
    ) { page ->
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            VideoPlayer(
                videoUri = m3u8List[page],
            ) {
                if (page != m3u8List.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(page + 1)
                    }
                }
            }
        }
    }
}