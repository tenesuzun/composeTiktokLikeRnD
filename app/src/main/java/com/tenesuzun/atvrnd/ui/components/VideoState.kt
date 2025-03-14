package com.tenesuzun.atvrnd.ui.components

sealed class VideoState {
    data class Loading(val videoUrl: String) : VideoState()
    data class Playing(val videoUrl: String) : VideoState()
    data class Error(val message: String) : VideoState()
}

enum class NetworkQuality(val bitrate: Int) {
//    LOW(500_000),    // 500kbps
//    MEDIUM(1_000_000), // 1Mbps
//    HIGH(2_000_000),    // 2Mbps

    WIFI(2_000_000),
    FOUR_G(1_000_000),
    THREE_G(500_000),
    EDGE(100_000),
    GPRS(25_000),
}