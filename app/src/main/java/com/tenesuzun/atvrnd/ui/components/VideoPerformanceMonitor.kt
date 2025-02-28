package com.tenesuzun.atvrnd.ui.components

import android.util.Log

class VideoPerformanceMonitor {
    private val loadTimers = mutableMapOf<String, Long>()
    private val videoStats = mutableMapOf<String, VideoPerformanceStats>()

    fun startLoadingTimer(videoUrl: String) {
        loadTimers[videoUrl] = System.currentTimeMillis()
    }

    fun recordPlaybackStart(videoUrl: String, quality: NetworkQuality) {
        val startTime = loadTimers[videoUrl] ?: return
        val loadTime = System.currentTimeMillis() - startTime

        val stats = VideoPerformanceStats(
            videoUrl = videoUrl,
            quality = quality,
            loadTimeMs = loadTime,
            bufferedTimeMs = 0,
            timestamp = System.currentTimeMillis()
        )

        videoStats[videoUrl] = stats

        // Log the stats
        Log.d("VideoPerformance", "Video loaded: $videoUrl, Quality: ${quality.name}, Load time: ${loadTime}ms")
    }

    fun recordBuffering(videoUrl: String, bufferingTimeMs: Long) {
        videoStats[videoUrl]?.let { stats ->
            val updatedStats = stats.copy(bufferedTimeMs = stats.bufferedTimeMs + bufferingTimeMs)
            videoStats[videoUrl] = updatedStats

            // Log the stats
            Log.d("VideoPerformance", "Video buffered: $videoUrl, Buffering time: ${bufferingTimeMs}ms, Total buffering: ${updatedStats.bufferedTimeMs}ms")
        }
    }

    private fun getAverageLoadTime(quality: NetworkQuality): Long {
        val relevantStats = videoStats.values.filter { it.quality == quality }
        if (relevantStats.isEmpty()) return 0
        return relevantStats.sumOf { it.loadTimeMs } / relevantStats.size
    }

    private fun getAverageBufferingTime(quality: NetworkQuality): Long {
        val relevantStats = videoStats.values.filter { it.quality == quality }
        if (relevantStats.isEmpty()) return 0
        return relevantStats.sumOf { it.bufferedTimeMs } / relevantStats.size
    }

    fun generateReport(): String {
        val sb = StringBuilder()
        sb.appendLine("=== Video Performance Report ===")

        NetworkQuality.entries.forEach { quality ->
            val avgLoadTime = getAverageLoadTime(quality)
            val avgBufferingTime = getAverageBufferingTime(quality)
            sb.appendLine("Quality: ${quality.name}")
            sb.appendLine("  Average load time: ${avgLoadTime}ms")
            sb.appendLine("  Average buffering time: ${avgBufferingTime}ms")
        }

        return sb.toString()
    }
}

data class VideoPerformanceStats(
    val videoUrl: String,
    val quality: NetworkQuality,
    val loadTimeMs: Long,
    val bufferedTimeMs: Long,
    val timestamp: Long
)