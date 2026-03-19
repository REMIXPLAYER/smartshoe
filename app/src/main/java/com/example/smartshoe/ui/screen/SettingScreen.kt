package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle

import androidx.compose.ui.window.Dialog
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppIcon
import com.example.smartshoe.ui.theme.AppIcons
import com.example.smartshoe.data.model.UserState
import com.example.smartshoe.ui.component.SmartShoeTextField

object SettingScreen {


    @Composable
    fun AccountSettingsSection(
        userState: UserState,
        onLogin: (String, String) -> Unit,
        onRegister: (String, String, String) -> Unit,
        onLogout: () -> Unit,
        onEditProfile: (String, String, String, String) -> Unit,
    ) {
        // 使用rememberSaveable保存弹窗状态，避免配置变化后状态丢失
        var showLoginDialog by rememberSaveable { mutableStateOf(false) }
        var showRegisterDialog by rememberSaveable { mutableStateOf(false) }

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
                        onLogout = onLogout
                    )
                } else {
                    LoggedOutView(
                        onLoginClick = { showLoginDialog = true },
                        onRegisterClick = { showRegisterDialog = true }
                    )
                }
            }
        }

        if (showLoginDialog) {
            LoginDialog(
                onDismiss = { showLoginDialog = false },
                onLogin = { email, password ->
                    onLogin(email, password)
                    showLoginDialog = false
                },
                onSwitchToRegister = {
                    showLoginDialog = false
                    showRegisterDialog = true
                }
            )
        }

        if (showRegisterDialog) {
            RegisterDialog(
                onDismiss = { showRegisterDialog = false },
                onRegister = { username, email, password ->
                    onRegister(username, email, password)
                    showRegisterDialog = false
                },
                onSwitchToLogin = {
                    showRegisterDialog = false
                    showLoginDialog = true
                }
            )
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
        onLogout: () -> Unit
    ) {
        // 使用rememberSaveable保存编辑状态，避免配置变化后丢失
        var isEditExpanded by rememberSaveable { mutableStateOf(false) }
        var username by rememberSaveable { mutableStateOf(userState.username) }
        var email by rememberSaveable { mutableStateOf(userState.email) }
        var currentPassword by rememberSaveable { mutableStateOf("") }
        var newPassword by rememberSaveable { mutableStateOf("") }
        var confirmPassword by rememberSaveable { mutableStateOf("") }
        var passwordVisible by rememberSaveable { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }

        val passwordsMatch = newPassword == confirmPassword || newPassword.isEmpty()
        val isFormValid = username.isNotBlank() &&
                email.isNotBlank() &&
                currentPassword.isNotBlank() &&
                passwordsMatch

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
                    .clickable { isEditExpanded = !isEditExpanded }
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

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isEditExpanded) "收起" else "展开",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            rotationZ = if (isEditExpanded) 90f else 0f
                        }
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
                    onUsernameChange = { username = it },
                    email = email,
                    onEmailChange = { email = it },
                    currentPassword = currentPassword,
                    onCurrentPasswordChange = { currentPassword = it },
                    newPassword = newPassword,
                    onNewPasswordChange = { newPassword = it },
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = { confirmPassword = it },
                    passwordVisible = passwordVisible,
                    onPasswordVisibleChange = { passwordVisible = it },
                    passwordsMatch = passwordsMatch,
                    isFormValid = isFormValid,
                    isLoading = isLoading,
                    onSave = {
                        isLoading = true
                        onEditProfile(username, email, newPassword, currentPassword)
                        isLoading = false
                        isEditExpanded = false
                    },
                    onCancel = { isEditExpanded = false }
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
        onSwitchToRegister: () -> Unit
    ) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        val context = LocalContext.current
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
                        onValueChange = { email = it },
                        label = "邮箱地址",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Email
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 密码输入
                    SmartShoeTextField.Password(
                        value = password,
                        onValueChange = { password = it },
                        label = "密码",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 登录按钮
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "请输入邮箱和密码", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            onLogin(email, password)
                            isLoading = false
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
        onSwitchToLogin: () -> Unit
    ) {
        var username by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var passwordsMatch by remember { mutableStateOf(true) }
        val context = LocalContext.current
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
                        onValueChange = { username = it },
                        label = "用户名",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Person
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 邮箱输入
                    SmartShoeTextField.Email(
                        value = email,
                        onValueChange = { email = it },
                        label = "邮箱地址",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Email
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 密码输入
                    SmartShoeTextField.Password(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordsMatch = it == confirmPassword
                        },
                        label = "密码",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Default.Lock,
                        visibilityIcon = painterResource(R.drawable.visibility),
                        visibilityOffIcon = painterResource(R.drawable.visibility_off)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 确认密码输入
                    SmartShoeTextField.Password(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordsMatch = password == it
                        },
                        label = "确认密码",
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
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
                            when {
                                username.isBlank() -> {
                                    Toast.makeText(context, "请输入用户名", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                email.isBlank() -> {
                                    Toast.makeText(context, "请输入邮箱地址", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                password.isBlank() -> {
                                    Toast.makeText(context, "请输入密码", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                password != confirmPassword -> {
                                    Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }
                            isLoading = true
                            onRegister(username, email, password)
                            isLoading = false
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
     * 设备设置项组件
     */
    @Composable
     fun DeviceSettingItem(
        device: BluetoothDevice,
        isConnected: Boolean,
        onConnect: () -> Unit,
        onDisconnect: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) Color(0xFFE8F5E8) else Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 设备图标和名称
                Icon(
                    painter = painterResource(R.drawable.bluetooth),
                    contentDescription = "蓝牙设备",
                    tint = if (isConnected) Color(0xFF4CAF50) else AppColors.Primary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getDeviceDisplayName(device),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isConnected) Color(0xFF2E7D32) else AppColors.OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = device.address ?: "未知地址",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                // 连接/断开按钮
                Button(
                    onClick = {
                        if (isConnected) {
                            onDisconnect()
                        } else {
                            onConnect()
                        }
                    },
                    modifier = Modifier
                        .height(32.dp)
                        .width(if (isConnected) 70.dp else 60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFFF44336) else AppColors.Primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = if (isConnected) "断开" else "连接",
                        fontSize = 12.sp,
                        color = Color.White
                    )
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
        onBackupData: (Boolean, String, (Boolean) -> Unit) -> Unit,
        isLoggedIn: Boolean,
        hasData: Boolean
    ) {
        // 使用rememberSaveable保存状态，避免配置变化后状态丢失
        var isDeviceListExpanded by rememberSaveable { mutableStateOf(false) }
        var isEditingWeight by rememberSaveable { mutableStateOf(false) }
        var weightInput by rememberSaveable { mutableStateOf("") }
        var uploadStatus by remember { mutableStateOf("idle") }

        // 优化图标旋转动画
        val arrowRotation by animateFloatAsState(
            targetValue = if (isDeviceListExpanded) 90f else 0f,
            animationSpec = tween(durationMillis = AnimationDefaults.DURATION_LONG),
            label = "arrow_rotation"
        )

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. 蓝牙设备管理（可展开）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDeviceListExpanded = !isDeviceListExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = "蓝牙设备",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "蓝牙设备管理",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Text(
                            if (connectedDevice != null) "已连接：${getDeviceDisplayName(connectedDevice)}"
                            else "未连接设备",
                            fontSize = 14.sp,
                            color = if (connectedDevice != null) Color(0xFF4CAF50) else Color.Gray
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isDeviceListExpanded) "收起" else "展开",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = arrowRotation
                            }
                    )
                }

                // 展开的设备列表
                AnimatedVisibility(
                    visible = isDeviceListExpanded,
                    enter = expandVertically(
                        animationSpec = AnimationDefaults.expandTween
                    ) + fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                    exit = shrinkVertically(
                        animationSpec = AnimationDefaults.shrinkTween
                    ) + fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        if (scannedDevices.isEmpty()) {
                            Text(
                                "未找到设备，请在主页点击扫描",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // 使用 LazyColumn 优化大量设备列表的性能
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(
                                    items = scannedDevices,
                                    key = { it.address ?: it.hashCode() }
                                ) { device ->
                                    DeviceSettingItem(
                                        device = device,
                                        isConnected = connectedDevice?.address == device.address,
                                        onConnect = { onConnectDevice(device) },
                                        onDisconnect = onDisconnectDevice
                                    )
                                }
                            }
                        }

                        if (connectedDevice != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.Green, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "已连接",
                                    fontSize = 12.sp,
                                    color = Color.Green,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
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
                            onValueChange = { weightInput = it },
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
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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
                            val context = LocalContext.current
                            IconButton(
                                onClick = {
                                    val weight = weightInput.toFloatOrNull()
                                    if (weight != null && weight > 0 && weight < 300) {
                                        onEditWeight(weight)
                                        isEditingWeight = false
                                    } else {
                                        Toast.makeText(context, "请输入有效的体重（1-300kg）", Toast.LENGTH_SHORT).show()
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
                                onClick = {
                                    isEditingWeight = false
                                    weightInput = ""
                                },
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
                            .clickable {
                                weightInput = if (userWeight > 0) userWeight.toString() else ""
                                isEditingWeight = true
                            }
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
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when {
                                !isLoggedIn -> {
                                }
                                !hasData -> {
                                }
                                uploadStatus == "uploading" -> {
                                }
                                else -> {
                                    uploadStatus = "uploading"
                                    onBackupData(true, "") { success ->
                                        uploadStatus = if (success) "success" else "failed"
                                    }
                                }
                            }
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
                                    uploadStatus == "uploading" -> "正在上传..."
                                    uploadStatus == "success" -> "上传成功"
                                    uploadStatus == "failed" -> "上传失败"
                                    else -> "备份到云端"
                                },
                                fontSize = 14.sp,
                                color = when (uploadStatus) {
                                    "success" -> Color(0xFF4CAF50)
                                    "failed" -> Color(0xFFF44336)
                                    else -> Color.Gray
                                }
                            )
                        }
                    }

                    when (uploadStatus) {
                        "uploading" -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AppColors.Primary,
                                strokeWidth = 2.dp
                            )
                        }
                        "success" -> {
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
                        "failed" -> {
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
                var showClearCacheConfirm by remember { mutableStateOf(false) }
                Button(
                    onClick = { showClearCacheConfirm = true },
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
                        onDismissRequest = { showClearCacheConfirm = false },
                        title = { Text("清除缓存确认") },
                        text = {
                            Text("确定要清除所有缓存数据吗？这将包括：\n• 用户数据\n• 蓝牙设备列表\n• 传感器数据\n• 体重数据\n• 连接状态\n• 临时文件\n\n此操作不可撤销。")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    onClearCache()
                                    showClearCacheConfirm = false
                                }
                            ) {
                                Text("确认清除", color = Color(0xFFF44336))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showClearCacheConfirm = false }
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
                // 1. 版本信息（可展开）- 与设备与偏好卡片风格一致
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isVersionExpanded) {
                                isHelpExpanded = false
                                isPrivacyExpanded = false
                            }
                            isVersionExpanded = !isVersionExpanded
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "版本信息",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "版本信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Text(
                            "v1.0.0 (Build 2026)",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isVersionExpanded) "收起" else "展开",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = if (isVersionExpanded) 90f else 0f
                            }
                    )
                }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isHelpExpanded) {
                                isVersionExpanded = false
                                isPrivacyExpanded = false
                            }
                            isHelpExpanded = !isHelpExpanded
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.help),
                        contentDescription = "使用帮助",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "使用帮助",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Text(
                            "操作指南与常见问题",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isHelpExpanded) "收起" else "展开",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = if (isHelpExpanded) 90f else 0f
                            }
                    )
                }

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
                                    "4. 在设置中管理设备连接\n"+
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isPrivacyExpanded) {
                                isVersionExpanded = false
                                isHelpExpanded = false
                            }
                            isPrivacyExpanded = !isPrivacyExpanded
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.security),
                        contentDescription = "隐私政策",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "隐私政策",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Text(
                            "查看隐私保护条款",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isPrivacyExpanded) "收起" else "展开",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = if (isPrivacyExpanded) 90f else 0f
                            }
                    )
                }

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

    /**
     * 版本详情项组件
     */
    @Composable
    private fun VersionDetailItem(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = AppColors.OnSurface,
                fontWeight = FontWeight.Normal
            )
        }
    }

    @Composable
    private fun SettingItem(
        icon: AppIcon,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.OnSurface
                    )
                    Text(
                        subtitle,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "更多",
                    tint = Color.Gray
                )
            }
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
        onGenerateMockData: () -> Unit = {}
    ) {
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
                    onEditProfile = onEditProfile
                ) 
            }
            item {
                // 设备与偏好设置组合卡片
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
                    onBackupData = onBackupData,
                    isLoggedIn = isLoggedIn,
                    hasData = hasData
                )
            }
            item { AboutAppSection() }
        }
    }

}
