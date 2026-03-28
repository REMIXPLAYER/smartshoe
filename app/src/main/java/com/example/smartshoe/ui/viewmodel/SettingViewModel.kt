package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.data.manager.SensorDataManager
import com.example.smartshoe.data.model.UserState
import com.example.smartshoe.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面视图模型
 * 管理设置页面的UI状态和业务逻辑
 *
 * 职责：
 * - 管理登录/注册/修改资料的表单验证
 * - 管理设置页面的加载状态
 * - 管理数据备份状态
 * - 管理对话框显示状态
 */
@HiltViewModel
class SettingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sensorDataManager: SensorDataManager
) : ViewModel() {

    // ==================== 登录对话框状态 ====================
    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _loginEmail = MutableStateFlow("")
    val loginEmail: StateFlow<String> = _loginEmail.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    private val _loginPasswordVisible = MutableStateFlow(false)
    val loginPasswordVisible: StateFlow<Boolean> = _loginPasswordVisible.asStateFlow()

    private val _isLoginLoading = MutableStateFlow(false)
    val isLoginLoading: StateFlow<Boolean> = _isLoginLoading.asStateFlow()

    // ==================== 注册对话框状态 ====================
    private val _showRegisterDialog = MutableStateFlow(false)
    val showRegisterDialog: StateFlow<Boolean> = _showRegisterDialog.asStateFlow()

    private val _registerUsername = MutableStateFlow("")
    val registerUsername: StateFlow<String> = _registerUsername.asStateFlow()

    private val _registerEmail = MutableStateFlow("")
    val registerEmail: StateFlow<String> = _registerEmail.asStateFlow()

    private val _registerPassword = MutableStateFlow("")
    val registerPassword: StateFlow<String> = _registerPassword.asStateFlow()

    private val _registerConfirmPassword = MutableStateFlow("")
    val registerConfirmPassword: StateFlow<String> = _registerConfirmPassword.asStateFlow()

    private val _registerPasswordVisible = MutableStateFlow(false)
    val registerPasswordVisible: StateFlow<Boolean> = _registerPasswordVisible.asStateFlow()

    private val _isRegisterLoading = MutableStateFlow(false)
    val isRegisterLoading: StateFlow<Boolean> = _isRegisterLoading.asStateFlow()

    // ==================== 编辑资料状态 ====================
    private val _isEditProfileExpanded = MutableStateFlow(false)
    val isEditProfileExpanded: StateFlow<Boolean> = _isEditProfileExpanded.asStateFlow()

    private val _editProfileUsername = MutableStateFlow("")
    val editProfileUsername: StateFlow<String> = _editProfileUsername.asStateFlow()

    private val _editProfileEmail = MutableStateFlow("")
    val editProfileEmail: StateFlow<String> = _editProfileEmail.asStateFlow()

    private val _editProfileCurrentPassword = MutableStateFlow("")
    val editProfileCurrentPassword: StateFlow<String> = _editProfileCurrentPassword.asStateFlow()

    private val _editProfileNewPassword = MutableStateFlow("")
    val editProfileNewPassword: StateFlow<String> = _editProfileNewPassword.asStateFlow()

    private val _editProfileConfirmPassword = MutableStateFlow("")
    val editProfileConfirmPassword: StateFlow<String> = _editProfileConfirmPassword.asStateFlow()

    private val _editProfilePasswordVisible = MutableStateFlow(false)
    val editProfilePasswordVisible: StateFlow<Boolean> = _editProfilePasswordVisible.asStateFlow()

    private val _isEditProfileLoading = MutableStateFlow(false)
    val isEditProfileLoading: StateFlow<Boolean> = _isEditProfileLoading.asStateFlow()

    // ==================== 体重编辑状态 ====================
    private val _isEditingWeight = MutableStateFlow(false)
    val isEditingWeight: StateFlow<Boolean> = _isEditingWeight.asStateFlow()

    private val _weightInput = MutableStateFlow("")
    val weightInput: StateFlow<String> = _weightInput.asStateFlow()

    // ==================== 数据备份状态 ====================
    private val _uploadStatus = MutableStateFlow(UploadStatus.IDLE)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

    // ==================== 错误信息 ====================
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== 清除缓存确认对话框 ====================
    private val _showClearCacheConfirm = MutableStateFlow(false)
    val showClearCacheConfirm: StateFlow<Boolean> = _showClearCacheConfirm.asStateFlow()

    // ==================== 登录相关方法 ====================

    fun showLoginDialog() {
        _showLoginDialog.value = true
        resetLoginForm()
    }

    fun hideLoginDialog() {
        _showLoginDialog.value = false
        resetLoginForm()
    }

    fun onLoginEmailChange(email: String) {
        _loginEmail.value = email
    }

    fun onLoginPasswordChange(password: String) {
        _loginPassword.value = password
    }

    fun onLoginPasswordVisibilityChange(visible: Boolean) {
        _loginPasswordVisible.value = visible
    }

    /**
     * 验证登录表单
     * @return 错误信息，如果验证通过返回 null
     */
    fun validateLoginForm(): String? {
        return when {
            _loginEmail.value.isBlank() || _loginPassword.value.isBlank() -> "请输入邮箱和密码"
            else -> null
        }
    }

    fun resetLoginForm() {
        _loginEmail.value = ""
        _loginPassword.value = ""
        _loginPasswordVisible.value = false
        _isLoginLoading.value = false
    }

    // ==================== 注册相关方法 ====================

    fun showRegisterDialog() {
        _showRegisterDialog.value = true
        resetRegisterForm()
    }

    fun hideRegisterDialog() {
        _showRegisterDialog.value = false
        resetRegisterForm()
    }

    fun switchToRegister() {
        hideLoginDialog()
        showRegisterDialog()
    }

    fun switchToLogin() {
        hideRegisterDialog()
        showLoginDialog()
    }

    fun onRegisterUsernameChange(username: String) {
        _registerUsername.value = username
    }

    fun onRegisterEmailChange(email: String) {
        _registerEmail.value = email
    }

    fun onRegisterPasswordChange(password: String) {
        _registerPassword.value = password
    }

    fun onRegisterConfirmPasswordChange(confirmPassword: String) {
        _registerConfirmPassword.value = confirmPassword
    }

    fun onRegisterPasswordVisibilityChange(visible: Boolean) {
        _registerPasswordVisible.value = visible
    }

    /**
     * 验证注册表单
     * @return 错误信息，如果验证通过返回 null
     */
    fun validateRegisterForm(): String? {
        return when {
            _registerUsername.value.isBlank() -> "请输入用户名"
            _registerEmail.value.isBlank() -> "请输入邮箱地址"
            _registerPassword.value.isBlank() -> "请输入密码"
            _registerPassword.value != _registerConfirmPassword.value -> "两次输入的密码不一致"
            else -> null
        }
    }

    fun resetRegisterForm() {
        _registerUsername.value = ""
        _registerEmail.value = ""
        _registerPassword.value = ""
        _registerConfirmPassword.value = ""
        _registerPasswordVisible.value = false
        _isRegisterLoading.value = false
    }

    // ==================== 编辑资料相关方法 ====================

    fun toggleEditProfileExpanded() {
        _isEditProfileExpanded.value = !_isEditProfileExpanded.value
        if (!_isEditProfileExpanded.value) {
            resetEditProfileForm()
        }
    }

    fun initEditProfileForm(userState: UserState) {
        _editProfileUsername.value = userState.username
        _editProfileEmail.value = userState.email
    }

    fun onEditProfileUsernameChange(username: String) {
        _editProfileUsername.value = username
    }

    fun onEditProfileEmailChange(email: String) {
        _editProfileEmail.value = email
    }

    fun onEditProfileCurrentPasswordChange(password: String) {
        _editProfileCurrentPassword.value = password
    }

    fun onEditProfileNewPasswordChange(password: String) {
        _editProfileNewPassword.value = password
    }

    fun onEditProfileConfirmPasswordChange(password: String) {
        _editProfileConfirmPassword.value = password
    }

    fun onEditProfilePasswordVisibilityChange(visible: Boolean) {
        _editProfilePasswordVisible.value = visible
    }

    /**
     * 验证编辑资料表单
     * @return 错误信息，如果验证通过返回 null
     */
    fun validateEditProfileForm(): String? {
        val passwordsMatch = _editProfileNewPassword.value == _editProfileConfirmPassword.value ||
                _editProfileNewPassword.value.isEmpty()

        return when {
            _editProfileUsername.value.isBlank() -> "用户名不能为空"
            _editProfileEmail.value.isBlank() -> "邮箱不能为空"
            _editProfileCurrentPassword.value.isBlank() -> "请输入当前密码进行身份验证"
            !passwordsMatch -> "新密码不匹配"
            else -> null
        }
    }

    /**
     * 检查编辑资料表单是否有效
     */
    fun isEditProfileFormValid(): Boolean {
        val passwordsMatch = _editProfileNewPassword.value == _editProfileConfirmPassword.value ||
                _editProfileNewPassword.value.isEmpty()
        return _editProfileUsername.value.isNotBlank() &&
                _editProfileEmail.value.isNotBlank() &&
                _editProfileCurrentPassword.value.isNotBlank() &&
                passwordsMatch
    }

    fun resetEditProfileForm() {
        _editProfileUsername.value = ""
        _editProfileEmail.value = ""
        _editProfileCurrentPassword.value = ""
        _editProfileNewPassword.value = ""
        _editProfileConfirmPassword.value = ""
        _editProfilePasswordVisible.value = false
        _isEditProfileLoading.value = false
    }

    // ==================== 体重编辑相关方法 ====================

    fun startEditingWeight(currentWeight: Float) {
        _isEditingWeight.value = true
        _weightInput.value = if (currentWeight > 0) currentWeight.toString() else ""
    }

    fun cancelEditingWeight() {
        _isEditingWeight.value = false
        _weightInput.value = ""
    }

    fun onWeightInputChange(input: String) {
        _weightInput.value = input
    }

    /**
     * 验证体重输入
     * @return 体重值，如果验证失败返回 null
     */
    fun validateWeightInput(): Float? {
        val weight = _weightInput.value.toFloatOrNull()
        return if (weight != null && weight > 0 && weight < 300) weight else null
    }

    // ==================== 数据备份相关方法 ====================

    /**
     * 执行数据备份（统一管理备份流程）
     * @param isLoggedIn 是否已登录
     * @param hasData 是否有数据
     * @param onUploadData 实际上传数据的回调（由MainActivity提供）
     */
    fun backupData(
        isLoggedIn: Boolean,
        hasData: Boolean,
        onUploadData: (onComplete: (Boolean) -> Unit) -> Unit
    ) {
        // 1. 检查是否可以备份
        if (!canBackup(isLoggedIn, hasData)) {
            return
        }

        // 2. 设置上传状态
        _uploadStatus.value = UploadStatus.UPLOADING

        // 3. 执行上传
        onUploadData { success ->
            if (success) {
                _uploadStatus.value = UploadStatus.SUCCESS
            } else {
                _uploadStatus.value = UploadStatus.FAILED
            }
            
            // 3秒后重置状态
            viewModelScope.launch {
                delay(3000)
                _uploadStatus.value = UploadStatus.IDLE
            }
        }
    }

    fun resetUploadStatus() {
        _uploadStatus.value = UploadStatus.IDLE
    }

    /**
     * 检查是否可以进行备份
     */
    fun canBackup(isLoggedIn: Boolean, hasData: Boolean): Boolean {
        return isLoggedIn && hasData && _uploadStatus.value != UploadStatus.UPLOADING
    }

    // ==================== 清除缓存相关方法 ====================

    fun showClearCacheConfirmDialog() {
        _showClearCacheConfirm.value = true
    }

    fun hideClearCacheConfirmDialog() {
        _showClearCacheConfirm.value = false
    }

    // ==================== 错误处理 ====================

    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ==================== 加载状态管理 ====================

    fun setLoginLoading(loading: Boolean) {
        _isLoginLoading.value = loading
    }

    fun setRegisterLoading(loading: Boolean) {
        _isRegisterLoading.value = loading
    }

    fun setEditProfileLoading(loading: Boolean) {
        _isEditProfileLoading.value = loading
    }
}

/**
 * 上传状态枚举
 */
enum class UploadStatus {
    IDLE,       // 空闲状态
    UPLOADING,  // 上传中
    SUCCESS,    // 上传成功
    FAILED      // 上传失败
}
