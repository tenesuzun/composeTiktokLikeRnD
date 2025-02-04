package com.tenesuzun.atvrnd

import android.app.Application
 import android.content.Context
import com.pandora.common.applog.AppLogWrapper
import com.pandora.common.env.Env
import com.pandora.common.env.config.Config
import com.pandora.common.env.config.VodConfig
import com.pandora.vod.VodSDK
import java.io.File

class App: Application() {
    override fun onCreate() {
        super.onCreate()

        initVodSDK(context = this, userUniqueId = "testUniqueID")
    }

    private fun initVodSDK(context: Context, userUniqueId: String) {
        val videoCacheDir = File(this.cacheDir, "video_cache").apply {
            if (!exists()) mkdirs()
        }

        val vodConfig = VodConfig.Builder(this)
            .setCacheDirPath(videoCacheDir.absolutePath)
            .setMaxCacheSize(300 * 1024 * 1024)
            .build()

        val config = Config.Builder()
            .setApplicationContext(context)
            .setAppID("709009")
            .setAppName("atvrnd")
            .setAppVersion("1.0")
            .setAppChannel("googleplay")
            .setLicenseUri("file:///android_asset/license/l-1176-ch-vod-a-709009.lic")
            .setVodConfig(vodConfig)
            .build()

        Env.init(config)

        AppLogWrapper.getAppLogInstance()?.let { appLog ->
            if (userUniqueId.isNotEmpty()) {
                appLog.setUserUniqueID(userUniqueId)
            }
        }

        VodSDK.openAllVodLog()
    }
}