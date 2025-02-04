package com.tenesuzun.atvrnd.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.tenesuzun.atvrnd.domain.m3u8List
import com.tenesuzun.atvrnd.domain.mp4List

@OptIn(UnstableApi::class)
@Composable
fun VideoPager() {
    val pagerState = rememberPagerState(pageCount = { mp4List.size })

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        TTVideoEngine(m3u8List[page])
    }
}