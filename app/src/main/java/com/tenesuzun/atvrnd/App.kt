package com.tenesuzun.atvrnd

import android.app.Application
import com.pandora.common.env.Env
import com.pandora.common.env.config.Config
import com.pandora.common.env.config.VodConfig
import java.io.File

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        val videoCacheDir = File(this.cacheDir, "video_cache").apply {
            if (!exists()) mkdirs()
        }

        val vodConfig = VodConfig.Builder(this)
            .setCacheDirPath(videoCacheDir.absolutePath)
            .setMaxCacheSize(300 * 1024 * 1024)
            .build()

        val config = Config.Builder()
            .setApplicationContext(this)
            .setAppID("709009")
            .setAppName("atvrnd")
            .setAppVersion("1.0")
            .setAppChannel("googleplay")
            .setLicenseUri("file:///android_asset/license/l-1176-ch-vod-a-709009.lic")
            .setVodConfig(vodConfig)
            .build()

        Env.init(config)

    }
}