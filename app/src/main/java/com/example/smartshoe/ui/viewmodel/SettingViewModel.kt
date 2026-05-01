package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartshoe.domain.repository.SensorDataRemoteRepository
import com.example.smartshoe.domain.model.UserState
import com.example.smartshoe.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val sensorDataRemoteRepository: SensorDataRemoteRepository
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

    // 登录错误状态（统一处理所有登录错误）
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // 登录错误字段标记（用于高亮显示错误的输入框）
    private val _loginErrorField = MutableStateFlow<LoginErrorField?>(null)
    val loginErrorField: StateFlow<LoginErrorField?> = _loginErrorField.asStateFlow()

    /**
     * 登录错误字段类型
     */
    enum class LoginErrorField {
        EMAIL, PASSWORD, BOTH
    }

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

    // 用于管理自动重置上传状态的协程
    private var resetUploadStatusJob: Job? = null

    // ==================== 错误信息 ====================
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== AboutAppSection 展开状态 ====================
    private val _isVersionExpanded = MutableStateFlow(false)
    val isVersionExpanded: StateFlow<Boolean> = _isVersionExpanded.asStateFlow()

    private val _isHelpExpanded = MutableStateFlow(false)
    val isHelpExpanded: StateFlow<Boolean> = _isHelpExpanded.asStateFlow()

    private val _isPrivacyExpanded = MutableStateFlow(false)
    val isPrivacyExpanded: StateFlow<Boolean> = _isPrivacyExpanded.asStateFlow()

    private val _isClearCacheExpanded = MutableStateFlow(false)
    val isClearCacheExpanded: StateFlow<Boolean> = _isClearCacheExpanded.asStateFlow()

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
        // 输入时清除错误状态
        clearLoginError()
    }

    fun onLoginPasswordChange(password: String) {
        _loginPassword.value = password
        // 输入时清除错误状态
        clearLoginError()
    }

    fun onLoginPasswordVisibilityChange(visible: Boolean) {
        _loginPasswordVisible.value = visible
    }

    /**
     * 设置登录错误
     * @param message 错误消息，null 表示清除错误
     * @param field 错误关联的字段
     */
    fun setLoginError(message: String?, field: LoginErrorField? = null) {
        _loginError.value = message
        _loginErrorField.value = field
    }

    /**
     * 清除登录错误
     */
    fun clearLoginError() {
        _loginError.value = null
        _loginErrorField.value = null
    }

    fun resetLoginForm() {
        _loginEmail.value = ""
        _loginPassword.value = ""
        _loginPasswordVisible.value = false
        _isLoginLoading.value = false
        clearLoginError()  // 清除错误状态
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

    fun showEditProfileDialog() {
        _isEditProfileExpanded.value = true
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
        return _editProfileCurrentPassword.value.isNotBlank() &&
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
     * 设置上传状态
     * @param status 上传状态
     */
    fun setUploadStatus(status: UploadStatus) {
        _uploadStatus.value = status

        // 取消之前的重置协程（如果有）
        resetUploadStatusJob?.cancel()

        // 如果上传成功或失败，3秒后自动重置为空闲状态
        if (status == UploadStatus.SUCCESS || status == UploadStatus.FAILED) {
            resetUploadStatusJob = viewModelScope.launch {
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

    // ==================== AboutAppSection 展开状态管理 ====================

    /**
     * 展开指定项，自动收起其他项（互斥展开）
     * @param target 要展开的项："version" | "help" | "privacy" | "clearCache" | ""
     */
    fun expandOnly(target: String) {
        _isVersionExpanded.value = target == "version"
        _isHelpExpanded.value = target == "help"
        _isPrivacyExpanded.value = target == "privacy"
        _isClearCacheExpanded.value = target == "clearCache"
    }

    // ==================== 认证状态处理 ====================

    /**
     * 处理认证完成（成功或失败后的清理）
     * 关闭对话框、收起编辑表单、重置加载状态
     */
    fun handleAuthCompleted(success: Boolean) {
        if (success) {
            hideLoginDialog()
            hideRegisterDialog()
            if (_isEditProfileExpanded.value) {
                toggleEditProfileExpanded()
            }
        }
        _isLoginLoading.value = false
        _isRegisterLoading.value = false
        _isEditProfileLoading.value = false
    }

    /**
     * 处理认证错误，将 HTTP 状态码和错误消息映射为用户友好消息
     * @param authErrorMessage 原始错误消息（格式如 "[401] 登录失败" 或纯文本）
     */
    fun handleAuthError(authErrorMessage: String) {
        val httpCodeRegex = Regex("\\[(\\d+)]")
        val httpCode = httpCodeRegex.find(authErrorMessage)?.groupValues?.get(1)?.toIntOrNull()
        val cleanMessage = authErrorMessage.replace(httpCodeRegex, "").trim()

        val (message, field) = when {
            // HTTP 401 - 未授权（账号或密码错误）
            httpCode == 401 -> "邮箱或密码错误" to LoginErrorField.BOTH
            // HTTP 403 - 禁止访问（账户被禁用）
            httpCode == 403 -> "账户已被禁用" to LoginErrorField.BOTH
            // HTTP 4xx - 客户端错误
            httpCode != null && httpCode in 400..499 ->
                (cleanMessage.ifEmpty { "请求参数错误" }) to LoginErrorField.BOTH
            // HTTP 5xx - 服务器错误
            httpCode != null && httpCode in 500..599 ->
                "服务器错误，请稍后重试" to null
            // 网络连接错误
            cleanMessage.contains("连接", ignoreCase = true) ||
            cleanMessage.contains("网络", ignoreCase = true) ||
            cleanMessage.contains("timeout", ignoreCase = true) ||
            cleanMessage.contains("无法连接", ignoreCase = true) ->
                "网络连接失败，请检查网络设置" to null
            // 其他未知错误
            else -> (cleanMessage.ifEmpty { "登录失败，请稍后重试" }) to null
        }

        setLoginError(message, field)
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
