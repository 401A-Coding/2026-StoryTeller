plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.storyteller"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.storyteller"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.security.crypto)  // 新增：用于加密SharedPreferences
    implementation(libs.jsoup)  // 用于网页爬取
    implementation(libs.glide)  // 图片加载
    implementation(libs.markwon)  // Markdown渲染
    implementation(libs.localbroadcastmanager)  // 本地广播管理器
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}