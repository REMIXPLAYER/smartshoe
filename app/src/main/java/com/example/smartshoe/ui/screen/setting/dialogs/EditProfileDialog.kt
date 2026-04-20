package com.example.smartshoe.ui.screen.setting.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import com.example.smartshoe.domain.model.UserState
import com.example.smartshoe.ui.component.SmartShoeTextField
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.viewmodel.SettingViewModel

/**
 * 编辑资料对话框
 */
@Composable
fun EditProfileDialog(
    userState: UserState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingViewModel? = null
) {
    val username = viewModel?.editProfileUsername?.collectAsStateWithLifecycle()?.value ?: userState.username
    val currentPassword = viewModel?.editProfileCurrentPassword?.collectAsStateWithLifecycle()?.value ?: ""
    val newPassword = viewModel?.editProfileNewPassword?.collectAsStateWithLifecycle()?.value ?: ""
    val confirmPassword = viewModel?.editProfileConfirmPassword?.collectAsStateWithLifecycle()?.value ?: ""
    val passwordVisible = viewModel?.editProfilePasswordVisible?.collectAsStateWithLifecycle()?.value ?: false
    val isLoading = viewModel?.isEditProfileLoading?.collectAsStateWithLifecycle()?.value ?: false
    val passwordsMatch = newPassword == confirmPassword || newPassword.isEmpty()
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
                    text = "编辑资料",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
                Spacer(modifier = Modifier.height(20.dp))
                // 邮箱只显示，不可编辑
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = AppColors.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "邮箱地址",
                            fontSize = 12.sp,
                            color = AppColors.DarkGray
                        )
                        Text(
                            text = userState.email,
                            fontSize = 16.sp,
                            color = AppColors.OnSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SmartShoeTextField.Username(
                    value = username,
                    onValueChange = { viewModel?.onEditProfileUsernameChange(it) },
                    label = "用户名（可选）",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartShoeTextField.Password(
                    value = currentPassword,
                    onValueChange = { viewModel?.onEditProfileCurrentPasswordChange(it) },
                    label = "当前密码（验证身份）",
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { viewModel?.onEditProfilePasswordVisibilityChange(!passwordVisible) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Lock,
                    visibilityIcon = painterResource(R.drawable.visibility),
                    visibilityOffIcon = painterResource(R.drawable.visibility_off)
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartShoeTextField.Password(
                    value = newPassword,
                    onValueChange = { viewModel?.onEditProfileNewPasswordChange(it) },
                    label = "新密码（留空不修改）",
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { viewModel?.onEditProfilePasswordVisibilityChange(!passwordVisible) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Lock,
                    visibilityIcon = painterResource(R.drawable.visibility),
                    visibilityOffIcon = painterResource(R.drawable.visibility_off)
                )
                Spacer(modifier = Modifier.height(12.dp))
                SmartShoeTextField.Password(
                    value = confirmPassword,
                    onValueChange = { viewModel?.onEditProfileConfirmPasswordChange(it) },
                    label = "确认新密码",
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { viewModel?.onEditProfilePasswordVisibilityChange(!passwordVisible) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Default.Lock,
                    isError = !passwordsMatch && confirmPassword.isNotBlank(),
                    errorMessage = if (!passwordsMatch && confirmPassword.isNotBlank()) "密码不匹配" else null,
                    visibilityIcon = painterResource(R.drawable.visibility),
                    visibilityOffIcon = painterResource(R.drawable.visibility_off)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "提示：修改资料需要输入当前密码验证",
                    fontSize = 12.sp,
                    color = AppColors.DarkGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onSave(username, userState.email, newPassword, currentPassword) },
                    enabled = !isLoading && currentPassword.isNotBlank() && passwordsMatch,
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
                        Text("保存修改", fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("取消", color = AppColors.DarkGray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 退出登录按钮
                Button(
                    onClick = {
                        onLogout()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "退出登录",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("退出登录", color = AppColors.Background)
                }
            }
        }
    }
}
