/*
 * TỔNG QUAN FILE: Cấu hình build của module app: plugin, phiên bản Android, cấu hình theo máy, bản debug và
 * release, thư viện.
 *
 * Thành phần chính:
 * - plugins: Android, Compose, kotlinx.serialization, Hilt, KSP (bộ sinh code cho Hilt và Room).
 * - google-services chỉ được bật khi có app/google-services.json, thiếu file thì app vẫn build, chỉ không có
 *   push.
 * - config(): đọc local.properties (IP backend, Google Web Client ID, Cloudinary) và đưa vào BuildConfig.
 * - android: applicationId, minSdk 26, targetSdk 37, versionCode và versionName.
 * - buildTypes: debug gọi backend trên máy tính (10.0.2.2), release gọi loki.apiUrlRelease.
 * - dependencies: phiên bản lấy từ gradle/libs.versions.toml.
 *
 * Lưu ý:
 * - local.properties không được commit; giá trị mặc định ở đây chỉ dùng được với emulator.
 * - applicationId com.example.* không được Play Store chấp nhận, phải đổi trước khi phát hành (và khai báo lại
 *   OAuth client của Google, Firebase).
 * - versionCode phải tăng mỗi lần phát hành.
 * - Bản release bật R8 (rút gọn + làm rối code) với keep rule trong proguard-rules.pro. Thêm thư viện dùng
 *   reflection thì phải thử lại bản release.
 * - Room xuất schema vào app/schemas (cần commit) để viết Migration và kiểm tra thay đổi database.
 * - Thêm thư viện thì khai báo trong libs.versions.toml trước, tránh khai báo trùng với phiên bản khác.
 */
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Firebase push notifications turn on once you add app/google-services.json from the Firebase console
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Per-machine settings come from local.properties (not committed); the defaults work on the emulator
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun config(key: String, default: String) = "\"${localProps.getProperty(key, default)}\""

android {
    namespace = "com.example.loki"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.loki"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Google Cloud Console > Credentials > OAuth client of type "Web application" (same as the backend's GOOGLE_CLIENT_ID)
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", config("loki.googleWebClientId", "878739657664-li1t25i8kl0a12aikpaqv24effvnrij4.apps.googleusercontent.com"))
        // cloudinary.com: Dashboard shows the cloud name; the upload preset must be Unsigned
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", config("loki.cloudinaryCloudName", "tx2kluh2"))
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", config("loki.cloudinaryUploadPreset", "loki_unsigned"))
    }

    buildTypes {
        debug {
            // 10.0.2.2 = the computer running the emulator; set loki.apiUrl to your PC's LAN IP for a real phone
            buildConfigField("String", "API_BASE_URL", config("loki.apiUrl", "http://10.0.2.2:8080/"))
        }
        release {
            buildConfigField("String", "API_BASE_URL", config("loki.apiUrlRelease", "https://api.example.com/"))
            optimization {
                enable = true
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room writes each database version's schema here; Migrations are written and checked against these files
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}