package com.tenesuzun.atvrnd.ui.components

import com.tenesuzun.atvrnd.domain.m3u8List

class VideoRepository {
    fun getVideoUrls(): List<String> {
        return m3u8List
    }
}