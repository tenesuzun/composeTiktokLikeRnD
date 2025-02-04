package com.tenesuzun.atvrnd.ui.components

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.tenesuzun.atvrnd.domain.mp4List

fun List<String>.toMediaItems(): List<MediaItem> {
    return map {
        MediaItem.fromUri(it)
    }
}

fun prepareVideoPlayers(startPage: Int, endPage: Int, context: Context, players: MutableMap<Int, ExoPlayer>) {
    (startPage..endPage).forEach { page ->
        if (!players.containsKey(page)) {
            val player = ExoPlayer.Builder(context).build().apply {
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
                playWhenReady = true
                setMediaItem(MediaItem.fromUri(mp4List[page]))
//                    setMediaItem(MediaItem.fromUri(m3u8List[page]))
                prepare()
            }
            players[page] = player
        }
    }
}

//suspend fun extractVideoThumbnail(videoUri: Uri, context: Context): Bitmap? {
//    return withContext(Dispatchers.IO) {
//        try {
//        val retriever = MediaMetadataRetriever()
//            retriever.setDataSource(context, videoUri)
////        retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
//            val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
//            retriever.release()
//            bitmap
//        } catch (e: Exception) {
//            Log.e("ThumbnailExtraction", "Failed to extract thumbnail: ${e.message}" )
//            null
//        }
//    }
//}