import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// 鍙戝竷绛惧悕瀵嗛挜涓庡瘑鐮佷粠 local.properties锛堜笉鍏紑锛夎鍙栵紱缂哄け鏃?release 鏃犳硶绛惧悕锛堢鍚堥鏈燂細绛惧悕鑳藉姏浠呴檺浣滆€呮湰鍦帮級
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.ideacompost.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ideacompost.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.3.2"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(localProps.getProperty("storeFile", "ideacompost-release.keystore"))
            storePassword = localProps.getProperty("storePassword", "")
            keyAlias = localProps.getProperty("keyAlias", "")
            keyPassword = localProps.getProperty("keyPassword", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

// 发布时注入收款码：private/donate_qr.png 仅存在于本地（不入库），
// release 构建前拷贝到生成目录并注册为 release 资源集；文件缺失时构建照常（App 内显示占位图）。
val donateQrSrc = rootProject.file("private/donate_qr.png")
val donateAssetsDir = layout.buildDirectory.dir("generated/donateQrAssets")

val injectDonateQr = tasks.register<Copy>("injectDonateQr") {
    onlyIf { donateQrSrc.exists() }
    from(donateQrSrc) { rename { "donate_qr.png" } }
    into(donateAssetsDir)
}

android {
    sourceSets.getByName("release") {
        assets.srcDir(donateAssetsDir)
    }
}

tasks.matching {
    it.name == "mergeReleaseAssets" || it.name.startsWith("lintVital") || it.name.startsWith("generateReleaseLintVital")
}.configureEach {
    dependsOn(injectDonateQr)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)

    debugImplementation(libs.androidx.ui.tooling)
}
