package com.tenesuzun.atvrnd.ui.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.tenesuzun.atvrnd.ui.components.NetworkQuality
import com.tenesuzun.atvrnd.ui.components.VideoPerformanceMonitor
import com.tenesuzun.atvrnd.ui.components.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

@UnstableApi
class VideoPagerViewModel(
    private val applicationContext: Context,
    private val videoRepository: VideoRepository,
    private val performanceMonitor: VideoPerformanceMonitor
) : ViewModel() {
    private val _networkQuality = MutableStateFlow(NetworkQuality.WIFI)

    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()

    private val _videos = MutableStateFlow<List<String>>(emptyList())
    val videos: StateFlow<List<String>> = _videos.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        observeNetworkQuality()
        loadVideos()
    }

    private fun observeNetworkQuality() {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateNetworkQuality(networkCapabilities)
            }

            override fun onLost(network: Network) {
                _networkQuality.value = NetworkQuality.GPRS
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
    }

    private fun updateNetworkQuality(networkCapabilities: NetworkCapabilities) {
        val downloadSpeed = networkCapabilities.linkDownstreamBandwidthKbps
        val newQuality = when {
            downloadSpeed >= 2000 -> NetworkQuality.WIFI
            downloadSpeed >= 1500 -> NetworkQuality.FOUR_G
            downloadSpeed >= 1000 -> NetworkQuality.THREE_G
            downloadSpeed >= 500 -> NetworkQuality.EDGE
            else -> NetworkQuality.GPRS
        }

        if (_networkQuality.value != newQuality) {
            _networkQuality.value = newQuality
            Log.d("NetworkQuality", "Network quality changed to: ${newQuality.name}, Bandwidth: ${downloadSpeed}kbps")
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

    override fun onCleared() {
        super.onCleared()
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    @OptIn(UnstableApi::class)
    fun createPlayer(
        context: Context,
        videoUrl: String,
        networkQuality: NetworkQuality,
        performanceMonitor: VideoPerformanceMonitor,
        previewDurationMs: Long = 3000
    ): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                previewDurationMs.toInt(), // Minimum buffer of 1 second
                previewDurationMs.toInt(), // Maximum buffer matches preview duration (3000ms)
                500,  // Start playback after 500ms of buffering
                1000  // Restart after rebuffering once we have 1 second
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoBitrate(networkQuality.bitrate)
                    .setMinVideoBitrate(networkQuality.bitrate / 2)
                    .setForceHighestSupportedBitrate(false)
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .build()
            )
        }

        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setUseLazyPreparation(false)
            .build()

        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.playWhenReady = false

        var bufferingStartTime = 0L

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        performanceMonitor.recordPlaybackStart(videoUrl, networkQuality)
                    }

                    Player.STATE_BUFFERING -> {
                        bufferingStartTime = System.currentTimeMillis()
                    }

                    Player.STATE_IDLE -> {
                        if (bufferingStartTime > 0) {
                            val bufferingTime = System.currentTimeMillis() - bufferingStartTime
                            performanceMonitor.recordBuffering(videoUrl, bufferingTime)
                            bufferingStartTime = 0
                        }
                    }

                    else -> {}
                }
            }
        })

        player.setMediaItem(
            MediaItem.Builder()
                .setUri(videoUrl)
                .setCustomCacheKey("${videoUrl}_${networkQuality.name}")
                .build()
        )

        player.prepare()

        return player
    }

    @OptIn(UnstableApi::class)
    fun updatePlayerQuality(
        player: ExoPlayer,
        networkQuality: NetworkQuality
    ) {
        (player.trackSelector as? DefaultTrackSelector)?.let { selector ->
            selector.parameters = selector.buildUponParameters()
                .setMaxVideoBitrate(networkQuality.bitrate)
                .setMinVideoBitrate(networkQuality.bitrate / 2)
                .setForceHighestSupportedBitrate(false)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .build()
        }
    }

    fun cleanupPlayers(
        players: MutableMap<Int, ExoPlayer>,
        currentPage: Int,
        startPage: Int,
        endPage: Int,
    ) {
        val cleanupThreshold = 20
        if (players.size > cleanupThreshold) {
            players.keys.toList()
                .filter { it !in (startPage..endPage) }
                .sortedBy { abs(it - currentPage) }
                .take(players.size - cleanupThreshold)
                .forEach { page ->
                    players[page]?.release()
                    players.remove(page)
                }
        }
    }
}