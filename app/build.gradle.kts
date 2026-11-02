plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.dsha.dswidget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dsha.dswidget"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.3"
    }

    // CI 每次构建都会现场生成一个随机 debug 签名，导致版本之间签名不同、无法覆盖安装。
    // 这里固定用仓库自带的 debug.p12（调试用密钥，公开无妨），保证以后升级能直接装。
    signingConfigs {
        create("stableDebug") {
            storeFile = file("keystore/debug.p12")
            storePassword = "dswidget"
            keyAlias = "dswidget"
            keyPassword = "dswidget"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("stableDebug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("stableDebug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
