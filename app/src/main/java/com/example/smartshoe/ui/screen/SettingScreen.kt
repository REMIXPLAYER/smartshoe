package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import com.example.smartshoe.util.AnimationDefaults
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.window.Dialog
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppIcon
import com.example.smartshoe.ui.theme.AppIcons
import com.example.smartshoe.data.model.UserState
import com.example.smartshoe.debug.ui.DebugSection
import com.example.smartshoe.ui.component.DeviceListContent
import com.example.smartshoe.ui.component.DeviceListHeader
import com.example.smartshoe.ui.component.DeviceSettingItem
import com.example.smartshoe.ui.component.ExpandableArrowIcon
import com.example.smartshoe.ui.component.SmartShoeTextField
import com.example.smartshoe.ui.component.VersionDetailItem
import com.example.smartshoe.ui.viewmodel.AuthUiState
import com.example.smartshoe.ui.viewmodel.SettingViewModel
import com.example.smartshoe.ui.viewmodel.UploadStatus

object SettingScreen {


    @Composable
    fun AccountSettingsSection(
        userState: UserState,
        onLogin: (String, String) -> Unit,
        onRegister: (String, String, String) -> Unit,
        onLogout: () -> Unit,
        onEditProfile: (String, String, String, String) -> Unit,
        settingViewModel: SettingViewModel? = null
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (userState.isLoggedIn) {
                    LoggedInView(
                        userState = userState,
                        onEditProfile = onEditProfile,
                        onLogout = onLogout,
                        settingViewModel = settingViewModel
                    )
                } else {
                    LoggedOutView(
                        onLoginClick = { settingViewModel?.showLoginDialog() },
                        onRegisterClick = { settingViewModel?.showRegisterDialog() }
                    )
                }
            }
        }
    }

    @Composable
    private fun EditProfileForm(
        username: String,
        onUsernameChange: (String) -> Unit,
        email: String,
        onEmailChange: (String) -> Unit,
        currentPassword: String,
        onCurrentPasswordChange: (String) -> Unit,
        newPassword: String,
        onNewPasswordChange: (String) -> Unit,
        confirmPassword: String,
        onConfirmPasswordChange: (String) -> Unit,
        passwordVisible: Boolean,
        onPasswordVisibleChange: (Boolean) -> Unit,
        passwordsMatch: Boolean,
        isFormValid: Boolean,
        isLoading: Boolean,
        onSave: () -> Unit,
        onCancel: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            // 用户名输入
            SmartShoeTextField.Username(
                value = username,
                onValueChange = onUsernameChange,
                label = "用户名",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Person
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 邮箱输入
            SmartShoeTextField.Email(
                value = email,
                onValueChange = onEmailChange,
                label = "邮箱地址",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 当前密码输入
            SmartShoeTextField.Password(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = "当前密码（验证身份）",
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = { onPasswordVisibleChange(!passwordVisible) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Lock,
                visibilityIcon = painterResource(R.drawable.visibility),
                visibilityOffIcon = painterResource(R.drawable.visibility_off)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 新密码输入
            SmartShoeTextField.Password(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = "新密码（留空不修改）",
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = { onPasswordVisibleChange(!passwordVisible) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Lock,
                visibilityIcon = painterResource(R.drawable.visibility),
                visibilityOffIcon = painterResource(R.drawable.visibility_off)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 确认新密码
            SmartShoeTextField.Password(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "确认新密码",
                passwordVisible = passwordVisible,
                onPasswordVisibilityChange = { onPasswordVisibleChange(!passwordVisible) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Lock,
                isError = !passwordsMatch && confirmPassword.isNotBlank(),
                errorMessage = if (!passwordsMatch && confirmPassword.isNotBlank()) "新密码不匹配" else null,
                visibilityIcon = painterResource(R.drawable.visibility),
                visibilityOffIcon = painterResource(R.drawable.visibility_off)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "提示：修改资料需要输入当前密码进行身份验证",
                fontSize = 12.sp,
                color = Color.Gray,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = onSave,
                enabled = !isLoading && isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("保存修改", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 取消按钮
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("取消", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @Composable
    private fun LoggedInView(
        userState: UserState,
        onEditProfile: (String, String, String, String) -> Unit,
        onLogout: () -> Unit,
        settingViewModel: SettingViewModel? = null
    ) {
        // 使用 SettingViewModel 管理状态
        val isEditExpanded = settingViewModel?.isEditProfileExpanded?.collectAsStateWithLifecycle()?.value ?: false
        val username = settingViewModel?.editProfileUsername?.collectAsStateWithLifecycle()?.value ?: userState.username
        val email = settingViewModel?.editProfileEmail?.collectAsStateWithLifecycle()?.value ?: userState.email
        val currentPassword = settingViewModel?.editProfileCurrentPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val newPassword = settingViewModel?.editProfileNewPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val confirmPassword = settingViewModel?.editProfileConfirmPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val passwordVisible = settingViewModel?.editProfilePasswordVisible?.collectAsStateWithLifecycle()?.value ?: false
        val isLoading = settingViewModel?.isEditProfileLoading?.collectAsStateWithLifecycle()?.value ?: false

        // 初始化表单数据
        if (settingViewModel != null && !isEditExpanded) {
            settingViewModel.initEditProfileForm(userState)
        }

        val passwordsMatch = newPassword == confirmPassword || newPassword.isEmpty()
        val isFormValid = settingViewModel?.isEditProfileFormValid() ?: (username.isNotBlank() &&
                email.isNotBlank() &&
                currentPassword.isNotBlank() &&
                passwordsMatch)

        Column {
            // 用户信息摘要
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(AppColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userState.username.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userState.username,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = userState.email,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 编辑资料展开项
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingViewModel?.toggleEditProfileExpanded() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = "编辑资料",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "编辑用户资料",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = "修改个人信息",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                ExpandableArrowIcon(
                    isExpanded = isEditExpanded,
                    useGraphicsLayer = true
                )
            }

            // 展开的编辑资料表单
            AnimatedVisibility(
                visible = isEditExpanded,
                enter = expandVertically(
                    animationSpec = AnimationDefaults.expandTween
                ) + fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                exit = shrinkVertically(
                    animationSpec = AnimationDefaults.shrinkTween
                ) + fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
            ) {
                EditProfileForm(
                    username = username,
                    onUsernameChange = { settingViewModel?.onEditProfileUsernameChange(it) },
                    email = email,
                    onEmailChange = { settingViewModel?.onEditProfileEmailChange(it) },
                    currentPassword = currentPassword,
                    onCurrentPasswordChange = { settingViewModel?.onEditProfileCurrentPasswordChange(it) },
                    newPassword = newPassword,
                    onNewPasswordChange = { settingViewModel?.onEditProfileNewPasswordChange(it) },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { settingViewModel?.onEditProfileConfirmPasswordChange(it) },
                    passwordVisible = passwordVisible,
                    onPasswordVisibleChange = { settingViewModel?.onEditProfilePasswordVisibilityChange(it) },
                    passwordsMatch = passwordsMatch,
                    isFormValid = isFormValid,
                    isLoading = isLoading,
                    onSave = {
                        onEditProfile(username, email, newPassword, currentPassword)
                    },
                    onCancel = { settingViewModel?.toggleEditProfileExpanded() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 退出登录按钮
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("退出登录", color = Color.White)
            }

            // 添加底部间距，避免与下一个卡片重叠
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    @Composable
    private fun LoggedOutView(
        onLoginClick: () -> Unit,
        onRegisterClick: () -> Unit
    ) {
        Column {
            // 登录按钮
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("登录账户")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 注册按钮
            Button(
                onClick = onRegisterClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("注册新账户", color = AppColors.Primary)
            }

            // 添加底部间距，避免与下一个卡片重叠
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    @Composable
    private fun LoginDialog(
        onDismiss: () -> Unit,
        onLogin: (String, String) -> Unit,
        onSwitchToRegister: () -> Unit,
        onShowError: (String) -> Unit = {},
        viewModel: SettingViewModel? = null
    ) {
        // 使用 SettingViewModel 管理状态
        val email = viewModel?.loginEmail?.collectAsStateWithLifecycle()?.value ?: ""
        val password = viewModel?.loginPassword?.collectAsStateWithLifecycle()?.value ?: ""
        val passwordVisible = viewModel?.loginPasswordVisible?.collectAsStateWithLifecycle()?.value ?: false
        val isLoading = viewModel?.isLoginLoading?.collectAsStateWithLifecycle()?.value ?: false
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
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(scrollState)
                ) {
                    // 标题
                    Text(
                        text = "用户登录",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 邮箱输入
                    SmartShoeTextField.Email(
                        value = email,
                        onValueChange = { viewModel?.onLoginEmailChange(it) },
                        label = "邮箱地址",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Email
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 密码输入
                    SmartShoeTextField.Password(
                        value = password,
                        onValueChange = { viewModel?.onLoginPasswordChange(it) },
                        label = "密码",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { viewModel?.onLoginPasswordVisibilityChange(!passwordVisible) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 登录按钮
                    Button(
                        onClick = {
                            onLogin(email, password)
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("登录", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 注册链接
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "还没有账户？",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        TextButton(onClick = onSwitchToRegister) {
                            Text(
                                "立即注册",
                                color = AppColors.Primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("取消", color = Color.Gray)
                    }
                }
            }
        }
    }

    @Composable
    private fun RegisterDialog(
        onDismiss: () -> Unit,
        onRegister: (String, String, String) -> Unit,
        onSwitchToLogin: () -> Unit,
        onShowError: (String) -> Unit = {},
        viewModel: SettingViewModel? = null
    ) {
        // 使用 SettingViewModel 管理状态
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
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(scrollState)
                ) {
                    // 标题
                    Text(
                        text = "用户注册",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 用户名输入
                    SmartShoeTextField.Username(
                        value = username,
                        onValueChange = { viewModel?.onRegisterUsernameChange(it) },
                        label = "用户名",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Person
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 邮箱输入
                    SmartShoeTextField.Email(
                        value = email,
                        onValueChange = { viewModel?.onRegisterEmailChange(it) },
                        label = "邮箱地址",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Email
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 密码输入
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

                    // 确认密码输入
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

                    // 注册按钮
                    Button(
                        onClick = {
                            onRegister(username, email, password)
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("注册", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 登录链接
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "已有账户？",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        TextButton(onClick = onSwitchToLogin) {
                            Text(
                                "立即登录",
                                color = AppColors.Primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 取消按钮
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("取消", color = Color.Gray)
                    }
                }
            }
        }
    }


    /**
     * 设备与偏好设置组合卡片
     * 将蓝牙设备管理、体重设置和压力提醒整合在一个卡片中
     */
    @Composable
    fun DeviceAndPreferenceSection(
        scannedDevices: List<BluetoothDevice>,
        connectedDevice: BluetoothDevice?,
        onConnectDevice: (BluetoothDevice) -> Unit,
        onDisconnectDevice: () -> Unit,
        userWeight: Float,
        onEditWeight: (Float) -> Unit,
        pressureAlertsEnabled: Boolean,
        onPressureAlertsChange: (Boolean) -> Unit,
        onClearCache: () -> Unit,
        onBackupClick: () -> Unit,
        uploadStatus: UploadStatus,
        isLoggedIn: Boolean,
        hasData: Boolean,
        settingViewModel: SettingViewModel? = null
    ) {
        // 使用rememberSaveable保存状态，避免配置变化后状态丢失
        var isDeviceListExpanded by rememberSaveable { mutableStateOf(false) }

        // 从ViewModel获取状态
        val isEditingWeight = settingViewModel?.isEditingWeight?.collectAsStateWithLifecycle()?.value ?: false
        val weightInput = settingViewModel?.weightInput?.collectAsStateWithLifecycle()?.value ?: ""
        val showClearCacheConfirm = settingViewModel?.showClearCacheConfirm?.collectAsStateWithLifecycle()?.value ?: false

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. 蓝牙设备管理（可展开）- 使用高性能动画
                DeviceListHeader(
                    isExpanded = isDeviceListExpanded,
                    onExpandedChange = { isDeviceListExpanded = it },
                    connectedDevice = connectedDevice
                )

                // 展开的设备列表
                AnimatedVisibility(
                    visible = isDeviceListExpanded,
                    enter = expandVertically(animationSpec = AnimationDefaults.expandTween) +
                            fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                    exit = shrinkVertically(animationSpec = AnimationDefaults.shrinkTween) +
                           fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        DeviceListContent(
                            scannedDevices = scannedDevices,
                            connectedDevice = connectedDevice,
                            onConnectDevice = onConnectDevice,
                            onDisconnectDevice = onDisconnectDevice,
                            deviceSettingItem = { device, isConnected, onConnect, onDisconnect ->
                                DeviceSettingItem(
                                    device = device,
                                    isConnected = isConnected,
                                    onConnect = onConnect,
                                    onDisconnect = onDisconnect
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分隔线
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                // 2. 体重数据设置
                if (isEditingWeight) {
                    // 编辑模式
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.man),
                                contentDescription = "体重",
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    "体重数据",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.OnSurface
                                )
                                Text(
                                    "输入体重数值",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 使用 BasicTextField 替代 OutlinedTextField，完全控制内边距
                            androidx.compose.foundation.text.BasicTextField(
                                value = weightInput,
                                onValueChange = { settingViewModel?.onWeightInputChange(it) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    color = AppColors.OnSurface
                                ),
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(36.dp)
                                    .border(
                                        width = 1.dp,
                                        color = AppColors.Primary,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            8.dp
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )

                            Text(
                                text = " kg",
                                fontSize = 14.sp,
                                color = AppColors.OnSurface,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // 确认按钮
                            IconButton(
                                onClick = {
                                    val weight = settingViewModel?.validateWeightInput()
                                    if (weight != null) {
                                        onEditWeight(weight)
                                    } else {
                                        settingViewModel?.showError("请输入有效的体重值（1-300kg）")
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = "确认",
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 取消按钮
                            IconButton(
                                onClick = { settingViewModel?.cancelEditingWeight() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "取消",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    // 显示模式
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingViewModel?.startEditingWeight(userWeight) }
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.man),
                                contentDescription = "体重",
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    "体重数据",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.OnSurface
                                )
                                Text(
                                    "点击修改体重",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = if (userWeight > 0) "${userWeight} kg" else "未设置",
                            fontSize = 14.sp,
                            color = if (userWeight > 0) AppColors.Primary else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分隔线
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                // 3. 压力异常提醒
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.warning),
                            contentDescription = "压力提醒",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                "压力异常提醒",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.OnSurface
                            )
                            Text(
                                "检测到异常压力时通知",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Switch(
                        checked = pressureAlertsEnabled,
                        onCheckedChange = onPressureAlertsChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AppColors.Primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                            uncheckedTrackColor = AppColors.Surface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分隔线
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                // 4. 数据备份
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // UI层只负责触发事件，所有业务逻辑交给ViewModel
                            onBackupClick()
                        }
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.cloud),
                            contentDescription = "数据备份",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                "数据备份",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.OnSurface
                            )
                            Text(
                                when {
                                    !isLoggedIn -> "请先登录后再备份"
                                    !hasData -> "暂无数据可备份"
                                    uploadStatus == UploadStatus.UPLOADING -> "正在上传..."
                                    uploadStatus == UploadStatus.SUCCESS -> "上传成功"
                                    uploadStatus == UploadStatus.FAILED -> "上传失败"
                                    else -> "备份到云端"
                                },
                                fontSize = 14.sp,
                                color = when (uploadStatus) {
                                    UploadStatus.SUCCESS -> Color(0xFF4CAF50)
                                    UploadStatus.FAILED -> Color(0xFFF44336)
                                    else -> Color.Gray
                                }
                            )
                        }
                    }

                    when (uploadStatus) {
                        UploadStatus.UPLOADING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AppColors.Primary,
                                strokeWidth = 2.dp
                            )
                        }

                        UploadStatus.SUCCESS -> {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF4CAF50), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = "上传成功",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        UploadStatus.FAILED -> {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFF44336), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "上传失败",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        else -> {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "备份",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. 清除缓存按钮
                Button(
                    onClick = { settingViewModel?.showClearCacheConfirmDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "清除",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清除应用缓存", color = Color.White, fontSize = 14.sp)
                }

                // 清除缓存确认对话框
                if (showClearCacheConfirm) {
                    AlertDialog(
                        onDismissRequest = { settingViewModel?.hideClearCacheConfirmDialog() },
                        title = { Text("清除缓存确认") },
                        text = {
                            Text("确定要清除所有缓存数据吗？这将包括：\n• 用户数据\n• 蓝牙设备列表\n• 传感器数据\n• 体重数据\n• 连接状态\n• 临时文件\n\n此操作不可撤销。")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onClearCache()
                                    settingViewModel?.hideClearCacheConfirmDialog()
                                }
                            ) {
                                Text("确认清除", color = Color(0xFFF44336))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { settingViewModel?.hideClearCacheConfirmDialog() }
                            ) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun AboutAppSection() {
        // 使用rememberSaveable保存展开状态，避免配置变化后状态丢失
        var isVersionExpanded by rememberSaveable { mutableStateOf(false) }
        var isHelpExpanded by rememberSaveable { mutableStateOf(false) }
        var isPrivacyExpanded by rememberSaveable { mutableStateOf(false) }

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. 版本信息（可展开）
                SettingItem(
                    icon = AppIcons.Info,
                    title = "版本信息",
                    subtitle = "v1.0.0 (Build 2026)",
                    onClick = {
                        if (!isVersionExpanded) {
                            isHelpExpanded = false
                            isPrivacyExpanded = false
                        }
                        isVersionExpanded = !isVersionExpanded
                    },
                    trailingContent = {
                        ExpandableArrowIcon(
                            isExpanded = isVersionExpanded,
                            useGraphicsLayer = true
                        )
                    }
                )

                // 展开的版本详情
                AnimatedVisibility(
                    visible = isVersionExpanded,
                    enter = expandVertically(
                        animationSpec = AnimationDefaults.expandTween
                    ) + fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                    exit = shrinkVertically(
                        animationSpec = AnimationDefaults.shrinkTween
                    ) + fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        VersionDetailItem("应用名称", "足底压力可视化")
                        Spacer(modifier = Modifier.height(8.dp))
                        VersionDetailItem("版本号", "v1.0.0")
                        Spacer(modifier = Modifier.height(8.dp))
                        VersionDetailItem("构建日期", "2026年3月")
                        Spacer(modifier = Modifier.height(8.dp))
                        VersionDetailItem("开发者", "SmartShoe Team")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分隔线
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                // 2. 使用帮助（可展开）
                SettingItem(
                    icon = AppIcons.Help,
                    title = "使用帮助",
                    subtitle = "操作指南与常见问题",
                    onClick = {
                        if (!isHelpExpanded) {
                            isVersionExpanded = false
                            isPrivacyExpanded = false
                        }
                        isHelpExpanded = !isHelpExpanded
                    },
                    trailingContent = {
                        ExpandableArrowIcon(
                            isExpanded = isHelpExpanded,
                            useGraphicsLayer = true
                        )
                    }
                )

                // 展开的帮助内容
                AnimatedVisibility(
                    visible = isHelpExpanded,
                    enter = expandVertically(
                        animationSpec = AnimationDefaults.expandTween
                    ) + fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                    exit = shrinkVertically(
                        animationSpec = AnimationDefaults.shrinkTween
                    ) + fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            "使用指南:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "1. 点击主页\"扫描\"按钮扫描设备\n" +
                                    "2. 选择要连接的蓝牙设备\n" +
                                    "3. 查看实时压力数据和图表\n" +
                                    "4. 在设置中管理设备连接\n" +
                                    "5. 登录账户后进行数据云端备份\n",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分隔线
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                )

                // 3. 隐私政策（可展开）
                SettingItem(
                    icon = AppIcons.Security,
                    title = "隐私政策",
                    subtitle = "查看隐私保护条款",
                    onClick = {
                        if (!isPrivacyExpanded) {
                            isVersionExpanded = false
                            isHelpExpanded = false
                        }
                        isPrivacyExpanded = !isPrivacyExpanded
                    },
                    trailingContent = {
                        ExpandableArrowIcon(
                            isExpanded = isPrivacyExpanded,
                            useGraphicsLayer = true
                        )
                    }
                )

                // 展开的隐私政策内容
                AnimatedVisibility(
                    visible = isPrivacyExpanded,
                    enter = expandVertically(
                        animationSpec = AnimationDefaults.expandTween
                    ) + fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                    exit = shrinkVertically(
                        animationSpec = AnimationDefaults.shrinkTween
                    ) + fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            "最后更新日期: 2025年11月",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "本应用尊重并保护您的隐私权。我们仅在必要时收集和使用您的个人信息，以提供和改进我们的服务。\n\n" +
                                    "我们收集的信息包括蓝牙设备名称、传感器数据和连接状态，这些信息仅用于实时显示压力分布和生成历史数据图表。所有数据仅在设备本地存储，不会上传到任何远程服务器。\n\n" +
                                    "我们承诺不会与第三方共享您的个人数据，除非获得您的明确同意或法律要求。您有权随时查看、修改或删除您的数据。",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingItem(
        icon: AppIcon,
        title: String,
        subtitle: String,
        onClick: () -> Unit,
        trailingContent: @Composable (() -> Unit)? = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 根据图标类型显示不同的图标
            when (icon) {
                is AppIcon.MaterialIcon -> {
                    Icon(
                        imageVector = icon.icon,
                        contentDescription = title,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                is AppIcon.ResourceIcon -> {
                    Icon(
                        painter = painterResource(id = icon.resId),
                        contentDescription = title,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                else -> {}
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // 尾部内容：自定义或默认箭头
            trailingContent?.invoke() ?: Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "更多",
                tint = Color.Gray
            )
        }
    }

    /**
     * 设置页面主入口
     * 组合所有设置相关的Section
     */
    @Composable
    fun SettingsScreen(
        modifier: Modifier = Modifier,
        scannedDevices: List<BluetoothDevice>,
        connectedDevice: BluetoothDevice?,
        userWeight: Float,
        onConnectDevice: (BluetoothDevice) -> Unit,
        onDisconnectDevice: () -> Unit,
        onClearCache: () -> Unit,
        onEditWeight: (Float) -> Unit,
        userState: UserState,
        onLogin: (String, String) -> Unit,
        onRegister: (String, String, String) -> Unit,
        onLogout: () -> Unit,
        onEditProfile: (String, String, String, String) -> Unit,
        pressureAlertsEnabled: Boolean,
        onPressureAlertsChange: (Boolean) -> Unit,
        onBackupData: (Boolean, String, (Boolean) -> Unit) -> Unit,
        isLoggedIn: Boolean,
        hasData: Boolean,
        onGenerateMockData: () -> Unit = {},
        settingViewModel: SettingViewModel? = null,
        onShowError: ((String) -> Unit)? = null,
        authUiState: AuthUiState = AuthUiState.Idle
    ) {
        // 使用 SettingViewModel 管理状态
        val showLoginDialog = settingViewModel?.showLoginDialog?.collectAsStateWithLifecycle()?.value ?: false
        val showRegisterDialog = settingViewModel?.showRegisterDialog?.collectAsStateWithLifecycle()?.value ?: false
        val errorMessage = settingViewModel?.errorMessage?.collectAsStateWithLifecycle()?.value

        // 监听认证状态变化，转换为简单参数后委托给ViewModel处理
        // 避免SettingViewModel直接依赖AuthUiState类型
        LaunchedEffect(authUiState) {
            when (authUiState) {
                is AuthUiState.Success -> settingViewModel?.handleAuthCompleted(success = true)
                is AuthUiState.Error -> settingViewModel?.handleAuthCompleted(success = false)
                is AuthUiState.Idle -> settingViewModel?.handleAuthCompleted(success = false)
                else -> { /* Loading状态不处理 */ }
            }
        }

        // 显示错误提示 - 通过回调让 Activity 处理
        errorMessage?.let { message ->
            onShowError?.invoke(message)
            settingViewModel?.clearError()
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.Background),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AccountSettingsSection(
                    userState = userState,
                    onLogin = onLogin,
                    onRegister = onRegister,
                    onLogout = onLogout,
                    onEditProfile = onEditProfile,
                    settingViewModel = settingViewModel
                )
            }
            item {
                // 设备与偏好设置组合卡片
                val uploadStatus = settingViewModel?.uploadStatus?.collectAsStateWithLifecycle()?.value ?: UploadStatus.IDLE
                
                DeviceAndPreferenceSection(
                    scannedDevices = scannedDevices,
                    connectedDevice = connectedDevice,
                    onConnectDevice = onConnectDevice,
                    onDisconnectDevice = onDisconnectDevice,
                    userWeight = userWeight,
                    onEditWeight = onEditWeight,
                    pressureAlertsEnabled = pressureAlertsEnabled,
                    onPressureAlertsChange = onPressureAlertsChange,
                    onClearCache = onClearCache,
                    onBackupClick = {
                        // 检查是否可以备份
                        if (settingViewModel?.canBackup(isLoggedIn, hasData) == true) {
                            // 设置上传状态为上传中
                            settingViewModel.setUploadStatus(UploadStatus.UPLOADING)
                            // 调用MainActivity传入的上传回调
                            onBackupData(true, "") { success ->
                                // 根据上传结果更新状态
                                settingViewModel.setUploadStatus(
                                    if (success) UploadStatus.SUCCESS else UploadStatus.FAILED
                                )
                            }
                        }
                    },
                    uploadStatus = uploadStatus,
                    isLoggedIn = isLoggedIn,
                    hasData = hasData,
                    settingViewModel = settingViewModel
                )
            }
            item { AboutAppSection() }
            item { DebugSection(onGenerateMockData = onGenerateMockData) }
        }

        // 登录对话框
        if (showLoginDialog) {
            LoginDialog(
                onDismiss = { settingViewModel?.hideLoginDialog() },
                onLogin = { email, password ->
                    // 验证表单
                    settingViewModel?.validateLoginForm()?.let { error ->
                        settingViewModel.showError(error)
                        return@LoginDialog
                    }
                    // 执行登录
                    settingViewModel?.setLoginLoading(true)
                    onLogin(email, password)
                },
                onSwitchToRegister = { settingViewModel?.switchToRegister() },
                viewModel = settingViewModel
            )
        }

        // 注册对话框
        if (showRegisterDialog) {
            RegisterDialog(
                onDismiss = { settingViewModel?.hideRegisterDialog() },
                onRegister = { username, email, password ->
                    // 验证表单
                    settingViewModel?.validateRegisterForm()?.let { error ->
                        settingViewModel.showError(error)
                        return@RegisterDialog
                    }
                    // 执行注册
                    settingViewModel?.setRegisterLoading(true)
                    onRegister(username, email, password)
                },
                onSwitchToLogin = { settingViewModel?.switchToLogin() },
                viewModel = settingViewModel
            )
        }
    }
}


