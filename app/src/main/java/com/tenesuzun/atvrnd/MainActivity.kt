package com.tenesuzun.atvrnd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pandora.common.applog.AppLogWrapper
import com.tenesuzun.atvrnd.ui.screens.VideoPager
import com.tenesuzun.atvrnd.ui.theme.AtvRnDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            onUserLogin("testUniqueID")

            AtvRnDTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        VideoPager()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onUserLogout()
    }

    private fun onUserLogout() {
        AppLogWrapper.getAppLogInstance()?.setUserUniqueID(null)
    }

    private fun onUserLogin(userUniqueId: String) {
        AppLogWrapper.getAppLogInstance()?.setUserUniqueID(userUniqueId)
    }
}