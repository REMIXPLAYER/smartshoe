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
 *
 * 重构后：
 * - AboutAppSection 展开状态从 rememberSaveable 迁移到 SettingViewModel
 * - HTTP 错误解析从 LaunchedEffect 下沉到 SettingViewModel.handleAuthError()
 * - UI 层只负责收集状态和触发事件
 */
object SettingScreen {
    /**
     * 设置页面内容
     */
    @Composable
    fun SettingsScreen(
        modifier: Modifier = Modifier,
        connectedDevice: BluetoothDevice?,
        userWeight: Float,
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
        // 对话框状态 - 来自 ViewModel
        val showLoginDialog = settingViewModel?.showLoginDialog?.collectAsStateWithLifecycle()?.value ?: false
        val showRegisterDialog = settingViewModel?.showRegisterDialog?.collectAsStateWithLifecycle()?.value ?: false
        val isEditProfileExpanded = settingViewModel?.isEditProfileExpanded?.collectAsStateWithLifecycle()?.value ?: false
        val uploadStatus = settingViewModel?.uploadStatus?.collectAsStateWithLifecycle()?.value ?: UploadStatus.IDLE
        val errorMessage = settingViewModel?.errorMessage?.collectAsStateWithLifecycle()?.value

        // AboutAppSection 展开状态 - 来自 ViewModel（统一状态管理）
        val isVersionExpanded = settingViewModel?.isVersionExpanded?.collectAsStateWithLifecycle()?.value ?: false
        val isHelpExpanded = settingViewModel?.isHelpExpanded?.collectAsStateWithLifecycle()?.value ?: false
        val isPrivacyExpanded = settingViewModel?.isPrivacyExpanded?.collectAsStateWithLifecycle()?.value ?: false
        val isClearCacheExpanded = settingViewModel?.isClearCacheExpanded?.collectAsStateWithLifecycle()?.value ?: false

        // 处理认证状态 - 错误解析已下沉到 ViewModel
        LaunchedEffect(authUiState) {
            when (authUiState) {
                is AuthUiState.Success -> {
                    settingViewModel?.handleAuthCompleted(success = true)
                }
                is AuthUiState.Error -> {
                    settingViewModel?.handleAuthError(authUiState.message)
                    settingViewModel?.handleAuthCompleted(success = false)
                }
                else -> {}
            }
        }

        // 显示全局错误
        errorMessage?.let { message ->
            onShowError?.invoke(message)
            settingViewModel?.clearError()
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
                    connectedDevice = connectedDevice,
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
                SettingsList(
                    isVersionExpanded = isVersionExpanded,
                    isHelpExpanded = isHelpExpanded,
                    isPrivacyExpanded = isPrivacyExpanded,
                    isClearCacheExpanded = isClearCacheExpanded,
                    onVersionExpandedChange = { settingViewModel?.expandOnly(if (it) "version" else "") },
                    onHelpExpandedChange = { settingViewModel?.expandOnly(if (it) "help" else "") },
                    onPrivacyExpandedChange = { settingViewModel?.expandOnly(if (it) "privacy" else "") },
                    onClearCacheExpandedChange = { settingViewModel?.expandOnly(if (it) "clearCache" else "") },
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
            LaunchedEffect(Unit) {
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
