/*
 * TỔNG QUAN FILE: File build cấp gốc: chỉ khai báo các plugin dùng trong project, chưa áp dụng (apply false).
 *
 * Thành phần chính:
 * - plugins: Android, Compose, serialization, Hilt, KSP, google-services.
 *
 * Lưu ý:
 * - Plugin được áp dụng thật trong app/build.gradle.kts; ở đây chỉ để Gradle tải và chốt phiên bản một lần.
 */
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
}