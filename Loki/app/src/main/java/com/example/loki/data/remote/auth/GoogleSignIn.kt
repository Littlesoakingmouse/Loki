/*
 * TỔNG QUAN FILE: Đăng nhập Google bằng Credential Manager, trả về idToken để gửi cho backend.
 *
 * Thành phần chính:
 * - WEB_CLIENT_ID: lấy từ BuildConfig (loki.googleWebClientId trong local.properties).
 * - googleIdToken(context): mở bộ chọn tài khoản; trả null khi người dùng huỷ, ném lỗi khi cấu hình sai.
 *
 * Lưu ý:
 * - context phải là Activity vì bộ chọn tài khoản hiện lên trên Activity; không inject Context của Application
 *   vào đây.
 * - NoCredentialException thường do thiếu OAuth client loại Android (package name + SHA-1). Bản debug và
 *   release có SHA-1 khác nhau, phải khai báo cả hai trên Google Cloud Console.
 * - Web Client ID phải trùng với GOOGLE_CLIENT_ID mà backend dùng để kiểm tra token.
 */
package com.example.loki.data.remote.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.loki.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// Set in app/build.gradle.kts (override with loki.googleWebClientId in local.properties)
private const val WEB_CLIENT_ID = BuildConfig.GOOGLE_WEB_CLIENT_ID

// Shows Google's account picker; returns the idToken for the backend, or null if the user backed out.
// context must be an Activity: the picker is shown on top of it
suspend fun googleIdToken(context: Context): String? {
    check(WEB_CLIENT_ID.isNotBlank() && WEB_CLIENT_ID != "your-web-client-id") {
        "Google sign-in isn't set up: set loki.googleWebClientId in local.properties"
    }
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build())
        .build()
    return try {
        val result = CredentialManager.create(context).getCredential(context, request)
        GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    } catch (e: GetCredentialCancellationException) {
        null
    } catch (e: NoCredentialException) {
        // Also what Google returns when this app's package + SHA-1 has no Android OAuth client
        throw IllegalStateException("No Google account available. Check the Android OAuth client (SHA-1).", e)
    }
}
