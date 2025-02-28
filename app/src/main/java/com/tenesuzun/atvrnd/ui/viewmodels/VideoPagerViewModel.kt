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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.tenesuzun.atvrnd.ui.components.NetworkQuality
import com.tenesuzun.atvrnd.ui.components.VideoPerformanceMonitor
import com.tenesuzun.atvrnd.ui.components.VideoRepository
import com.tenesuzun.atvrnd.ui.components.VideoState
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

    private val _networkQuality = MutableStateFlow(NetworkQuality.HIGH)
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()

    private val _videos = MutableStateFlow<List<VideoState>>(emptyList())
    val videos: StateFlow<List<VideoState>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _performanceReport = MutableStateFlow("")
    val performanceReport: StateFlow<String> = _performanceReport.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val bandwidthMeter = DefaultBandwidthMeter.Builder(applicationContext).build()
    val estimatedBandwidth = bandwidthMeter.bitrateEstimate // bits per second


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
                _networkQuality.value = NetworkQuality.LOW
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
    }

    private fun updateNetworkQuality(networkCapabilities: NetworkCapabilities) {
        val downloadSpeed = networkCapabilities.linkDownstreamBandwidthKbps
        val newQuality = when {
            downloadSpeed >= 2000 -> NetworkQuality.HIGH
            downloadSpeed >= 1000 -> NetworkQuality.MEDIUM
            else -> NetworkQuality.LOW
        }

        // Only update if quality changed to avoid unnecessary player reconfigurations
        if (_networkQuality.value != newQuality) {
            _networkQuality.value = newQuality
            Log.d("NetworkQuality", "Network quality changed to: ${newQuality.name}, Bandwidth: ${downloadSpeed}kbps")
        }
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val videoUrls = videoRepository.getVideoUrls()
                _videos.value = videoUrls.map { VideoState.Playing(it) }
            } catch (e: Exception) {
                _videos.value = listOf(VideoState.Error("Failed to load videos"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getPerformanceReport() { // UI üstünde göstermek için konsola ayriyeten zaten log basılıyor
        viewModelScope.launch {
            _performanceReport.value = performanceMonitor.generateReport()
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
        previewMode: Boolean = false,
        previewDurationMs: Long = 3000
    ): ExoPlayer {
        // Create appropriate LoadControl based on mode
        val loadControl = if (previewMode) {
            // For preview mode, use a small buffer size and don't buffer beyond preview duration
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    1000, // Minimum buffer of 1 second
                    previewDurationMs.toInt(), // Maximum buffer matches preview duration (3000ms)
                    500,  // Start playback after 500ms of buffering
                    1000  // Restart after rebuffering once we have 1 second
                )
                .setTargetBufferBytes(3 * 1024 * 1024) // Limit buffer size (3MB) for preview
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        } else {
            // More aggressive buffering for full playback
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 2, // Double the default max buffer
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }

        // Create track selector with appropriate parameters
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoBitrate(if (previewMode) networkQuality.bitrate / 2 else networkQuality.bitrate)
                    .setMinVideoBitrate(if (previewMode) networkQuality.bitrate / 4 else networkQuality.bitrate / 2)
                    .setForceHighestSupportedBitrate(!previewMode) // Only force highest quality for non-preview
                    .setExceedRendererCapabilitiesIfNecessary(true)
            )
        }

        // Create player with prepared LoadControl and TrackSelector
        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()

        // Configure player
        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.playWhenReady = false // Don't play until explicitly told to

        // Add listeners for performance monitoring
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
                }
            }
        })

        // For preview mode, we'll add a listener to limit buffering rather than clipping the media
        if (previewMode) {
            player.addListener(object : Player.Listener {
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    // Check if we've gone beyond our preview duration
                    if (newPosition.positionMs > previewDurationMs) {
                        // Pause buffering but don't reset position
                        player.pause()
                    }
                }
            })
        }

        // Set media item WITHOUT clipping to allow continuous playback later
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(videoUrl)
                .setCustomCacheKey("${videoUrl}_${networkQuality.name}")
                .build()
        )

        // Prepare player
        player.prepare()

        return player
//        // Create track selector first
//        val trackSelector = DefaultTrackSelector(context).apply {
//            setParameters(
//                buildUponParameters()
//                    .setMaxVideoBitrate(networkQuality.bitrate)
//                    .setMinVideoBitrate(networkQuality.bitrate / 2)
//                    .setForceHighestSupportedBitrate(false)
//                    .setExceedRendererCapabilitiesIfNecessary(true)
//            )
//        }
//
//        // Create player with track selector
//        val player = ExoPlayer.Builder(context)
//            .setTrackSelector(trackSelector)  // Set the track selector here
//            .setLoadControl(
//                DefaultLoadControl.Builder()
//                    .setBufferDurationsMs(
//                        if (previewMode) 1000 else DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
//                        if (previewMode) 3000 else DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
//                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
//                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
//                    )
//                    .setPrioritizeTimeOverSizeThresholds(true)
//                    .build()
//            )
//            .build()
//
//        // Configure player
//        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
//        player.playWhenReady = true
//
//        // Add listeners for performance monitoring
//        var bufferingStartTime = 0L
//
//        player.addListener(object : Player.Listener {
//            override fun onPlaybackStateChanged(playbackState: Int) {
//                when (playbackState) {
//                    Player.STATE_READY -> {
//                        performanceMonitor.recordPlaybackStart(videoUrl, networkQuality)
//                    }
//
//                    Player.STATE_BUFFERING -> {
//                        bufferingStartTime = System.currentTimeMillis()
//                    }
//
//                    Player.STATE_IDLE -> {
//                        if (bufferingStartTime > 0) {
//                            val bufferingTime = System.currentTimeMillis() - bufferingStartTime
//                            performanceMonitor.recordBuffering(videoUrl, bufferingTime)
//                            bufferingStartTime = 0
//                        }
//                    }
//                }
//            }
//        })
//
//        if (previewMode) {
//            player.setMediaItem(
//                MediaItem.Builder()
//                    .setUri(videoUrl)
//                    .setCustomCacheKey("${videoUrl}_${networkQuality.name}_preview")
//                    .setClippingConfiguration(
//                        MediaItem.ClippingConfiguration.Builder()
//                            .setEndPositionMs(previewDurationMs)
//                            .build()
//                    )
//                    .build()
//            )
//        } else {
//            // Set media item
//            player.setMediaItem(
//                MediaItem.Builder()
//                    .setUri(videoUrl)
//                    .setCustomCacheKey("${videoUrl}_${networkQuality.name}")
//                    .build()
//            )
//        }
//
//        // Prepare player
//        player.prepare()
//
//        return player
    }

    fun convertToFullPlayer(
        context: Context,
        player: ExoPlayer,
        videoUrl: String,
        networkQuality: NetworkQuality
    ) {
        val currentPosition = player.currentPosition
        val wasPlaying = player.isPlaying

        val fullMediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setCustomCacheKey("${videoUrl}_${networkQuality.name}")
            .build()

        player.replaceMediaItem(0, fullMediaItem)
//        player.addMediaItem(1, fullMediaItem)

        // Optimize track selector for full playback quality
        (player.trackSelector as? DefaultTrackSelector)?.let { selector ->
            selector.parameters = selector.buildUponParameters()
                .setMaxVideoBitrate(networkQuality.bitrate)
                .setMinVideoBitrate(networkQuality.bitrate / 2)
                .setForceHighestSupportedBitrate(true) // Prioritize quality for main view
                .build()
        }

        player.seekTo(currentPosition)

//        player.addListener(object : Player.Listener {
//            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
//                    player.removeListener(this)
//                    player.removeMediaItem(0)
//
//                    (player.trackSelector as? DefaultTrackSelector)?.let { selector ->
//                        selector.parameters = selector.buildUponParameters()
//                            .setMaxVideoBitrate(networkQuality.bitrate)
//                            .setMinVideoBitrate(networkQuality.bitrate / 2)
//                            .setForceHighestSupportedBitrate(true)
//                            .build()
//                    }
//                }
//            }
//        })

        if (wasPlaying) {
            player.play()
        }
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
        endPage: Int
    ) {
        val cleanupThreshold = 5
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