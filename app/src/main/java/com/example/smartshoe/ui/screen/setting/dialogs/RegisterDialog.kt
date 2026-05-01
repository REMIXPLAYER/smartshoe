package com.example.smartshoe.ui.screen.setting.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshoe.R
import com.example.smartshoe.ui.component.SmartShoeTextField
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.viewmodel.SettingViewModel

/**
 * 注册对话框
 */
@Composable
fun RegisterDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String, String) -> Unit,
    onSwitchToLogin: () -> Unit,
    viewModel: SettingViewModel? = null
) {
    val username = viewModel?.registerUsername?.collectAsStateWithLifecycle()?.value ?: ""
    val email = viewModel?.registerEmail?.collectAsStateWithLifecycle()?.value ?: ""
    val password = viewModel?.registerPassword?.collectAsStateWithLifecycle()?.value ?: ""
    val confirmPassword = viewModel?.registerConfirmPassword?.collectAsStateWithLifecycle()?.value ?: ""
    val passwordVisible = viewModel?.registerPasswordVisible?.collectAsStateWithLifecycle()?.value ?: false
    val isLoading = viewModel?.isRegisterLoading?.collectAsStateWithLifecycle()?.value ?: false
    val passwordsMatch = password == confirmPassword || password.isEmpty()
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Background),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "用户注册",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
                Spacer(modifier = Modifier.height(20.dp))
                SmartShoeTextField.Username(
                    value = username,
                    onValueChange = { viewModel?.onRegisterUsernameChange(it) },
                    label = "用户名",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartShoeTextField.Email(
                    value = email,
                    onValueChange = { viewModel?.onRegisterEmailChange(it) },
                    label = "邮箱地址",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Email
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartShoeTextField.Password(
                    value = password,
                    onValueChange = { viewModel?.onRegisterPasswordChange(it) },
                    label = "密码",
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { viewModel?.onRegisterPasswordVisibilityChange(!passwordVisible) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Lock,
                    visibilityIcon = painterResource(R.drawable.visibility),
                    visibilityOffIcon = painterResource(R.drawable.visibility_off)
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartShoeTextField.Password(
                    value = confirmPassword,
                    onValueChange = { viewModel?.onRegisterConfirmPasswordChange(it) },
                    label = "确认密码",
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { viewModel?.onRegisterPasswordVisibilityChange(!passwordVisible) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Lock,
                    isError = !passwordsMatch && confirmPassword.isNotBlank(),
                    errorMessage = if (!passwordsMatch && confirmPassword.isNotBlank()) "密码不匹配" else null,
                    visibilityIcon = painterResource(R.drawable.visibility),
                    visibilityOffIcon = painterResource(R.drawable.visibility_off)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onRegister(username, email, password) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.Background,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("注册", fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("已有账户？", fontSize = 14.sp, color = AppColors.PlaceholderText)
                    TextButton(onClick = onSwitchToLogin) {
                        Text("立即登录", color = AppColors.Primary, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消", color = AppColors.PlaceholderText)
                }
            }
        }
    }
}
