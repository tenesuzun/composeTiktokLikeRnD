package com.tenesuzun.atvrnd.ui.screens

import android.graphics.SurfaceTexture
import android.view.SurfaceView
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ss.ttvideoengine.TTVideoEngine
import com.ss.ttvideoengine.source.DirectUrlSource

@Composable
fun TTVideoEngine(videoUrl: String) {
    val context = LocalContext.current
    val ttVideoEngine = TTVideoEngine(context, TTVideoEngine.PLAYER_TYPE_OWN)

    Surface(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { context ->
            TextureView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                background = null
                surfaceTextureListener = object : SurfaceTextureListener{
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int)
                    { ttVideoEngine.surface = android.view.Surface(surface) }
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean
                    { return true }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }, modifier = Modifier.fillMaxSize())

        AndroidView(factory = {
            SurfaceView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                ttVideoEngine.setSurfaceHolder(holder)
            }
        }, modifier = Modifier.fillMaxSize())

        val cacheKey = TTVideoEngine.computeMD5(videoUrl)

        val directUrlSource = DirectUrlSource.Builder()
            .setVid(videoUrl.substringAfter(".com/"))
            .addItem(
                DirectUrlSource.UrlItem.Builder()
                    .setUrl(videoUrl)
                    .setCacheKey(cacheKey)
                    .build()
            )
            .build()

        ttVideoEngine.strategySource = directUrlSource

        ttVideoEngine.play()

        DisposableEffect(key1 = Unit) {
            onDispose {
                ttVideoEngine.releaseAsync()
            }
        }
    }
}