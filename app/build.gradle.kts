plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt) apply false
    kotlin("kapt")
}

android {
    namespace = "com.tenesuzun.atvrnd"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tenesuzun.atvrnd"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    packaging {
        resources {
            pickFirsts += "**/*.so"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        // byteplus için 1_8 kullanılması gerekebilir
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // exoplayer
    implementation(libs.exoplayer)
    implementation(libs.media.ui)
    implementation(libs.media.common.ktx)
    implementation(libs.media.common)
    implementation(libs.exoplayer.dash)
    implementation(libs.exoplayer.hls)

    // dagger
    implementation(libs.hilt.android)

    //byteplus
    implementation(libs.byteplus.sdk.player.standard)
    implementation(libs.okhttp)
//    implementation ("com.bytedanceapi:ttsdk-player_standard:1.45.100.4")
//    implementation ("com.bytedance.applog:RangersAppLog-Lite-global:6.14.3")

    implementation(libs.hilt.navigation.compose)
}