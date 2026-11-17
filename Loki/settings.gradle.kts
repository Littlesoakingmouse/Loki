/*
 * TỔNG QUAN FILE: Cài đặt của Gradle: nơi tải plugin và thư viện, tên project và danh sách module.
 *
 * Thành phần chính:
 * - pluginManagement: kho Google (chỉ các nhóm com.android, com.google, androidx), Maven Central, Gradle
 *   Plugin Portal.
 * - foojay-resolver: tự tải JDK phù hợp cho Gradle khi cần.
 * - dependencyResolutionManagement: kho thư viện google() và mavenCentral(); FAIL_ON_PROJECT_REPOS cấm module
 *   tự khai báo kho riêng.
 * - include(":app"): project chỉ có một module.
 *
 * Lưu ý:
 * - Thêm thư viện từ kho khác (ví dụ JitPack) thì phải khai báo kho ở đây.
 */
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Loki"
include(":app")
 