package com.tenesuzun.atvrnd.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.tenesuzun.atvrnd.ui.components.BitrateTypes
import com.tenesuzun.atvrnd.ui.components.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

@UnstableApi
class OperationViewModel(
    private val applicationContext: Context,
    private val videoRepository: VideoRepository
) : ViewModel() {
    private val _videos = MutableStateFlow<List<String>>(emptyList())
    val videos: StateFlow<List<String>> = _videos.asStateFlow()

    var isAppInForeground = true
    var downloadSpeedState by mutableIntStateOf(0)

    init {
        observeNetworkQuality()
        loadVideos()
    }

    private fun observeNetworkQuality() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                if (isAppInForeground) {
                    downloadSpeedState = measureActualDownloadSpeedKbps()
                    delay(3000)
                }
            }
        }
    }

    fun calculateBitrateByDownloadSpeed(): Int {
        return when {
            downloadSpeedState >= 8000 -> BitrateTypes.p1080.bitrate
            downloadSpeedState >= 5000 -> BitrateTypes.p720.bitrate
            downloadSpeedState >= 2500 -> BitrateTypes.p480.bitrate
            downloadSpeedState >= 1000 -> BitrateTypes.p360.bitrate
            downloadSpeedState >= 500 -> BitrateTypes.p240.bitrate
            downloadSpeedState >= 0 -> BitrateTypes.p144.bitrate
            else -> BitrateTypes.p720.bitrate
        }
    }

    private suspend fun measureActualDownloadSpeedKbps(): Int {
        try {
            //val url = URL("https://speed.hetzner.de/100MB.bin") // küçük dosya kullan
            val url = URL("https://nbg1-speed.hetzner.com/100MB.bin") // küçük dosya kullan
            val connection = withContext(Dispatchers.IO) {
                url.openConnection()
            } as HttpURLConnection
            withContext(Dispatchers.IO) {
                connection.connect()
            }

            val input = BufferedInputStream(connection.inputStream)
            val startTime = System.currentTimeMillis()
            val buffer = ByteArray(1024)
            var totalBytes = 0
            var count: Int

            while (System.currentTimeMillis() - startTime < 3000) {
                count = withContext(Dispatchers.IO) {
                    input.read(buffer)
                }
                if (count == -1) break
                totalBytes += count
            }

            val endTime = System.currentTimeMillis()
            val durationSeconds = (endTime - startTime) / 1000.0
            withContext(Dispatchers.IO) {
                input.close()
            }
            connection.disconnect()

            return ((totalBytes * 8) / 1000.0 / durationSeconds).toInt()
        } catch (_: Exception) {
            return 1000
        }
    }

    private fun loadVideos() {
        viewModelScope.launch {
            try {
                val videoUrls = videoRepository.getVideoUrls()
                _videos.value = videoUrls.map { it }
            } catch (e: Exception) {
                _videos.value = listOf("Failed to load videos")
            } finally {
            }
        }
    }
}