package com.tenesuzun.atvrnd

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tenesuzun.atvrnd.ui.screens.VideoPager
import com.tenesuzun.atvrnd.ui.theme.AtvRnDTheme

class MainActivity : ComponentActivity() {
    private val iconOld = "com.tenesuzun.atvrnd.MainActivityDefault"
    private val iconNew = "com.tenesuzun.atvrnd.MainActivityNew"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
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
        changeIcon(iconOld = iconOld, iconNew = iconNew)
    }

    private fun changeIcon(iconOld : String, iconNew : String) {
        // disable old icon
        packageManager.setComponentEnabledSetting(
            ComponentName(this@MainActivity, iconOld),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // enable new icon
        packageManager.setComponentEnabledSetting(
            ComponentName(this@MainActivity, iconNew),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}