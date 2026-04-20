package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartshoe.domain.model.UserState
import com.example.smartshoe.ui.screen.setting.components.about.SettingsList
import com.example.smartshoe.ui.screen.setting.components.header.ProfileHeader
import com.example.smartshoe.ui.screen.setting.components.quickactions.QuickActionsSection
import com.example.smartshoe.ui.screen.setting.dialogs.EditProfileDialog
import com.example.smartshoe.ui.screen.setting.dialogs.LoginDialog
import com.example.smartshoe.ui.screen.setting.dialogs.RegisterDialog
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.viewmodel.AuthUiState
import com.example.smartshoe.ui.viewmodel.SettingViewModel
import com.example.smartshoe.ui.viewmodel.UploadStatus

/**
 * 设置页面主入口
 */
object SettingScreen {
    /**
     * 设置页面内容
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
        // 调试功能：生成模拟数据（当前未在设置页面使用，保留供外部调用）
        onGenerateMockData: () -> Unit = {},
        settingViewModel: SettingViewModel? = null,
        onShowError: ((String) -> Unit)? = null,
        authUiState: AuthUiState = AuthUiState.Idle
    ) {
        // 对话框状态
        val showLoginDialog = settingViewModel?.showLoginDialog?.collectAsStateWithLifecycle()?.value ?: false
        val showRegisterDialog = settingViewModel?.showRegisterDialog?.collectAsStateWithLifecycle()?.value ?: false
        val isEditProfileExpanded = settingViewModel?.isEditProfileExpanded?.collectAsStateWithLifecycle()?.value ?: false
        val uploadStatus = settingViewModel?.uploadStatus?.collectAsStateWithLifecycle()?.value ?: UploadStatus.IDLE
        val errorMessage = settingViewModel?.errorMessage?.collectAsStateWithLifecycle()?.value

        // AboutAppSection 展开状态
        var isVersionExpanded by rememberSaveable { mutableStateOf(false) }
        var isHelpExpanded by rememberSaveable { mutableStateOf(false) }
        var isPrivacyExpanded by rememberSaveable { mutableStateOf(false) }
        var isClearCacheExpanded by rememberSaveable { mutableStateOf(false) }

        // 处理认证状态
        LaunchedEffect(authUiState) {
            when (authUiState) {
                is AuthUiState.Success -> {
                    settingViewModel?.handleAuthCompleted(success = true)
                }
                is AuthUiState.Error -> {
                    val authErrorMessage = authUiState.message
                    // 根据 HTTP 状态码和错误消息统一处理登录错误
                    // 错误格式可能是 "[401] 登录失败" 或纯文本消息
                    val httpCodeRegex = Regex("\\[(\\d+)]")
                    val httpCode = httpCodeRegex.find(authErrorMessage)?.groupValues?.get(1)?.toIntOrNull()
                    val cleanMessage = authErrorMessage.replace(httpCodeRegex, "").trim()

                    when {
                        // HTTP 401 - 未授权（账号或密码错误）
                        httpCode == 401 -> {
                            settingViewModel?.setLoginError(
                                message = "邮箱或密码错误",
                                field = com.example.smartshoe.ui.viewmodel.SettingViewModel.LoginErrorField.BOTH
                            )
                        }
                        // HTTP 403 - 禁止访问（账户被禁用）
                        httpCode == 403 -> {
                            settingViewModel?.setLoginError(
                                message = "账户已被禁用",
                                field = com.example.smartshoe.ui.viewmodel.SettingViewModel.LoginErrorField.BOTH
                            )
                        }
                        // HTTP 4xx - 客户端错误（请求格式问题等）
                        httpCode != null && httpCode in 400..499 -> {
                            settingViewModel?.setLoginError(
                                message = cleanMessage.ifEmpty { "请求参数错误" },
                                field = com.example.smartshoe.ui.viewmodel.SettingViewModel.LoginErrorField.BOTH
                            )
                        }
                        // HTTP 5xx - 服务器错误
                        httpCode != null && httpCode in 500..599 -> {
                            settingViewModel?.setLoginError(
                                message = "服务器错误，请稍后重试",
                                field = null
                            )
                        }
                        // 网络连接错误
                        cleanMessage.contains("连接", ignoreCase = true) ||
                        cleanMessage.contains("网络", ignoreCase = true) ||
                        cleanMessage.contains("timeout", ignoreCase = true) ||
                        cleanMessage.contains("无法连接", ignoreCase = true) -> {
                            settingViewModel?.setLoginError(
                                message = "网络连接失败，请检查网络设置",
                                field = null
                            )
                        }
                        // 其他未知错误
                        else -> {
                            settingViewModel?.setLoginError(
                                message = cleanMessage.ifEmpty { "登录失败，请稍后重试" },
                                field = null
                            )
                        }
                    }
                    settingViewModel?.handleAuthCompleted(success = false)
                }
                else -> {}
            }
        }

        // 显示全局错误
        errorMessage?.let { message ->
            onShowError?.invoke(message)
            settingViewModel.clearError()
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.Background),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            // 用户信息头部
            item {
                ProfileHeader(
                    userState = userState,
                    connectedDevice = connectedDevice,
                    userWeight = userWeight,
                    uploadStatus = uploadStatus,
                    onEditProfileClick = {
                        if (userState.isLoggedIn) {
                            settingViewModel?.showEditProfileDialog()
                        } else {
                            settingViewModel?.showLoginDialog()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(60.dp))
            }

            // 快捷功能区
            item {
                QuickActionsSection(
                    scannedDevices = scannedDevices,
                    connectedDevice = connectedDevice,
                    onConnectDevice = onConnectDevice,
                    onDisconnectDevice = onDisconnectDevice,
                    userWeight = userWeight,
                    onEditWeight = onEditWeight,
                    pressureAlertsEnabled = pressureAlertsEnabled,
                    onPressureAlertsChange = onPressureAlertsChange,
                    onBackupClick = {
                        if (settingViewModel?.canBackup(isLoggedIn, hasData) == true) {
                            settingViewModel.setUploadStatus(UploadStatus.UPLOADING)
                            onBackupData(isLoggedIn, "手动备份") { success ->
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 关于应用区
            item {
                // 互斥展开：展开一项时自动收起其他项
                fun expandOnly(target: String) {
                    isVersionExpanded = target == "version"
                    isHelpExpanded = target == "help"
                    isPrivacyExpanded = target == "privacy"
                    isClearCacheExpanded = target == "clearCache"
                }

                SettingsList(
                    isVersionExpanded = isVersionExpanded,
                    isHelpExpanded = isHelpExpanded,
                    isPrivacyExpanded = isPrivacyExpanded,
                    isClearCacheExpanded = isClearCacheExpanded,
                    onVersionExpandedChange = { expandOnly(if (it) "version" else "") },
                    onHelpExpandedChange = { expandOnly(if (it) "help" else "") },
                    onPrivacyExpandedChange = { expandOnly(if (it) "privacy" else "") },
                    onClearCacheExpandedChange = { expandOnly(if (it) "clearCache" else "") },
                    onClearCache = onClearCache
                )
            }
        }

        // 登录对话框
        if (showLoginDialog) {
            LoginDialog(
                onDismiss = { settingViewModel?.hideLoginDialog() },
                onLogin = { email, password ->
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
                    settingViewModel?.validateRegisterForm()?.let { error ->
                        settingViewModel.showError(error)
                        return@RegisterDialog
                    }
                    settingViewModel?.setRegisterLoading(true)
                    onRegister(username, email, password)
                },
                onSwitchToLogin = { settingViewModel?.switchToLogin() },
                viewModel = settingViewModel
            )
        }

        // 编辑资料对话框
        if (isEditProfileExpanded && userState.isLoggedIn) {
            // 初始化编辑表单
            androidx.compose.runtime.LaunchedEffect(Unit) {
                settingViewModel?.initEditProfileForm(userState)
            }

            EditProfileDialog(
                userState = userState,
                onDismiss = { settingViewModel?.toggleEditProfileExpanded() },
                onSave = { username, email, newPassword, currentPassword ->
                    onEditProfile(username, email, newPassword, currentPassword)
                },
                onLogout = onLogout,
                viewModel = settingViewModel
            )
        }
    }
}
