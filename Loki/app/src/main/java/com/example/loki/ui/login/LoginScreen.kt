/*
 * TỔNG QUAN FILE: Màn đăng nhập và đăng ký (chung một màn, đăng ký chỉ thêm ô tên hiển thị).
 *
 * Thành phần chính:
 * - LoginScreen: các ô nhập giữ bằng rememberSaveable, canSubmit tính từ state, nút đăng nhập Google, nút
 *   chuyển chế độ.
 * - LoginField: ô nhập dùng lại cho tên, email và mật khẩu (state hoisting).
 * - AskLocalNetworkPermission: xin quyền ACCESS_LOCAL_NETWORK trên Android 17 trở lên.
 *
 * Lưu ý:
 * - Đăng nhập thành công không điều hướng ở đây: token được lưu, isLoggedIn đổi và NavHost tự chuyển sang
 *   Home.
 * - Shortcut: quyền mạng nội bộ chỉ cần khi backend chạy ở địa chỉ riêng (10.0.2.2, IP LAN); bỏ khi có URL
 *   công khai.
 * - imePadding giúp ô mật khẩu không bị bàn phím che.
 * - Bộ chọn tài khoản Google cần Activity nên được mở ngay tại đây (rememberCoroutineScope); ViewModel chỉ nhận
 *   idToken, không giữ Context.
 */
package com.example.loki.ui.login

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loki.data.remote.auth.googleIdToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Login and Register share one screen: register only adds a display name
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AskLocalNetworkPermission()
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val canSubmit = !uiState.isLoading && email.isNotBlank() && password.isNotBlank() &&
        (!isRegister || displayName.isNotBlank())

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Text("💛", fontSize = 56.sp)
            Text("Loki", fontSize = 48.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text(
                "Ảnh trực tiếp từ bạn bè,\nngay trên màn hình chính",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            if (isRegister) {
                LoginField(displayName, { displayName = it }, "Tên hiển thị")
            }
            LoginField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
            LoginField(password, { password = it }, "Mật khẩu", keyboardType = KeyboardType.Password, isPassword = true)

            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Button(
                onClick = {
                    if (isRegister) viewModel.register(email, password, displayName)
                    else viewModel.login(email, password)
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isRegister) "Tạo tài khoản" else "Đăng nhập", fontSize = 17.sp)
                }
            }
            Button(
                onClick = {
                    // The picker lives as long as this screen; backing out of it returns null and changes nothing
                    scope.launch {
                        try {
                            googleIdToken(context)?.let(viewModel::loginWithGoogle)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            viewModel.googleSignInFailed(e)
                        }
                    }
                },
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 20.sp)
                Spacer(Modifier.size(10.dp))
                Text("Tiếp tục với Google", fontSize = 17.sp)
            }
            TextButton(onClick = { isRegister = !isRegister }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isRegister) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký")
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = CircleShape,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}

// Android 17+ blocks connections to private addresses (our dev backend) without this permission.
// shortcut: drop it once the backend has a public URL
private const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"

@Composable
private fun AskLocalNetworkPermission() {
    if (Build.VERSION.SDK_INT < 37) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, ACCESS_LOCAL_NETWORK) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(ACCESS_LOCAL_NETWORK)
    }
}
