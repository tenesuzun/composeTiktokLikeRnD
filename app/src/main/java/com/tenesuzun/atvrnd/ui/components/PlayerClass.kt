package com.tenesuzun.atvrnd.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@UnstableApi
class PlayerClass(
    applicationContext: Context,
    videoUrl: String,
    previewDurationMs: Long = 3000,
    page: Int
) {
    var isPlayingState by mutableStateOf(false)
    var playerState by mutableIntStateOf(Player.STATE_IDLE)
    var thumbnailShownState by mutableStateOf(true)

    // buffer ayarları
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            previewDurationMs.toInt(), // Minimum buffer of 1 second
            previewDurationMs.toInt(), // Maximum buffer matches preview duration (3000ms)
            500,  // Start playback after 500ms of buffering
            1000  // Restart after rebuffering once we have 1 second
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    // bitrate ve çözünürlük ayarları
    private val trackSelector = DefaultTrackSelector(applicationContext).apply {
        setParameters(
            buildUponParameters()
                .setMaxVideoSize(720, 1280)
                .setMinVideoSize(180, 320)
                .setMaxVideoBitrate(BitrateTypes.p720.bitrate)
                .setMinVideoBitrate(BitrateTypes.p144.bitrate)
                .setForceHighestSupportedBitrate(false)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setAllowVideoMixedDecoderSupportAdaptiveness(true)
                .build()
        )
    }

    // codec slotu dolduğunda yedek codec (software) kullanması için, aksi halde codec slotu dolu olduğunda thumbnail'da kalıyor ve player tepki vermiyor
    private val renderersFactory = DefaultRenderersFactory(applicationContext).setEnableDecoderFallback(true)

    var player: ExoPlayer? = ExoPlayer.Builder(applicationContext, renderersFactory)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setUseLazyPreparation(false)
        .build()

    init {
        preparePlayer(page, videoUrl)
    }

    private fun preparePlayer(page: Int, videoUrl: String) {
        player?.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player?.playWhenReady = false

        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                // TODO: core v2'ye taşınınca hatalar firebase'e log olarak atılacak
                super.onPlayerError(error)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingState = isPlaying
                if (isPlaying)
                    thumbnailShownState = false
                super.onIsPlayingChanged(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        Log.e("playerState", "$page ready")
                        playerState = Player.STATE_READY
                        thumbnailShownState = false
                    }

                    Player.STATE_BUFFERING -> {
                        Log.e("playerState", "$page buffering")
                        playerState = Player.STATE_BUFFERING
                    }

                    Player.STATE_IDLE -> {
                        Log.e("playerState", "$page idle")
                        playerState = Player.STATE_IDLE
                    }

                    else -> {
                        playerState = playbackState
                        Log.e("playerState", "$page - else $playbackState")
                    }
                }
            }
        })

        player?.setMediaItem(
            MediaItem.Builder()
                .setUri(videoUrl)
                .setCustomCacheKey("player_$page")
                .build()
        )

        player?.prepare()
    }

    fun destroy() {
        player?.stop()
        player?.release()
        player = null
    }

    fun updatePlayerQuality(bitrate: Int) {
        (player?.trackSelector as? DefaultTrackSelector)?.let { selector ->
            selector.parameters = selector.buildUponParameters()
                .setMaxVideoSize(720, 1280)
                .setMinVideoSize(180, 320)
                .setMaxVideoBitrate(bitrate)
                .setMinVideoBitrate(BitrateTypes.p144.bitrate)
                .setForceHighestSupportedBitrate(false)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setAllowVideoMixedDecoderSupportAdaptiveness(true)
                .build()
        }
    }
}