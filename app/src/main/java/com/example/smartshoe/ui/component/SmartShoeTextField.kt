package com.example.smartshoe.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors

/**
 * SmartShoe 统一文本输入框组件
 * 用于替代 SettingScreen 中重复的 OutlinedTextField
 *
 * 遵循 Clean Architecture 原则，将 UI 组件提取到独立的可复用组件中
 */
object SmartShoeTextField {

    /**
     * 基础输入框
     *
     * @param value 输入值
     * @param onValueChange 值变化回调
     * @param label 标签文本
     * @param modifier 修饰符
     * @param leadingIcon 左侧图标（ImageVector）
     * @param leadingIconPainter 左侧图标（Painter）
     * @param trailingIcon 右侧图标按钮
     * @param keyboardType 键盘类型
     * @param isError 是否显示错误状态
     * @param errorMessage 错误提示文本
     * @param singleLine 是否单行
     * @param visualTransformation 视觉转换（用于密码显示/隐藏）
     */
    @Composable
    fun Basic(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        modifier: Modifier = Modifier,
        leadingIcon: ImageVector? = null,
        leadingIconPainter: Painter? = null,
        leadingIconContentDescription: String? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        keyboardType: KeyboardType = KeyboardType.Text,
        isError: Boolean = false,
        errorMessage: String? = null,
        singleLine: Boolean = true,
        visualTransformation: VisualTransformation = VisualTransformation.None
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = when {
                leadingIcon != null -> {
                    {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = leadingIconContentDescription ?: label,
                            tint = AppColors.Primary
                        )
                    }
                }
                leadingIconPainter != null -> {
                    {
                        Icon(
                            painter = leadingIconPainter,
                            contentDescription = leadingIconContentDescription ?: label,
                            tint = AppColors.Primary
                        )
                    }
                }
                else -> null
            },
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            supportingText = if (isError && errorMessage != null) {
                { Text(errorMessage, color = AppColors.Error) }
            } else null,
            modifier = modifier,
            singleLine = singleLine,
            shape = RoundedCornerShape(8.dp),
            visualTransformation = visualTransformation
        )
    }

    /**
     * 邮箱输入框
     */
    @Composable
    fun Email(
        value: String,
        onValueChange: (String) -> Unit,
        label: String = "邮箱地址",
        modifier: Modifier = Modifier,
        leadingIcon: ImageVector? = null,
        isError: Boolean = false,
        errorMessage: String? = null
    ) {
        Basic(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            leadingIcon = leadingIcon,
            keyboardType = KeyboardType.Email,
            isError = isError,
            errorMessage = errorMessage
        )
    }

    /**
     * 密码输入框
     *
     * @param value 密码值
     * @param onValueChange 值变化回调
     * @param label 标签文本
     * @param passwordVisible 密码是否可见
     * @param onPasswordVisibilityChange 密码可见性变化回调
     * @param modifier 修饰符
     * @param isError 是否显示错误状态
     * @param errorMessage 错误提示文本
     * @param visibilityIconVisible 是否显示可见性切换图标
     * @param visibilityIcon 可见图标
     * @param visibilityOffIcon 不可见图标
     */
    @Composable
    fun Password(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        passwordVisible: Boolean,
        onPasswordVisibilityChange: () -> Unit,
        modifier: Modifier = Modifier,
        leadingIcon: ImageVector? = null,
        isError: Boolean = false,
        errorMessage: String? = null,
        visibilityIconVisible: Boolean = true,
        visibilityIcon: Painter? = null,
        visibilityOffIcon: Painter? = null
    ) {
        Basic(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            leadingIcon = leadingIcon,
            trailingIcon = if (visibilityIconVisible && visibilityIcon != null && visibilityOffIcon != null) {
                {
                    IconButton(onClick = onPasswordVisibilityChange) {
                        Icon(
                            painter = if (passwordVisible) visibilityIcon else visibilityOffIcon,
                            contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                            tint = AppColors.DarkGray
                        )
                    }
                }
            } else null,
            keyboardType = KeyboardType.Password,
            isError = isError,
            errorMessage = errorMessage,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            }
        )
    }

    /**
     * 用户名输入框
     */
    @Composable
    fun Username(
        value: String,
        onValueChange: (String) -> Unit,
        label: String = "用户名",
        modifier: Modifier = Modifier,
        leadingIcon: ImageVector? = null,
        isError: Boolean = false,
        errorMessage: String? = null
    ) {
        Basic(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            leadingIcon = leadingIcon,
            isError = isError,
            errorMessage = errorMessage
        )
    }

    /**
     * 数字输入框（用于体重等）
     */
    @Composable
    fun Number(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        modifier: Modifier = Modifier,
        suffix: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
        errorMessage: String? = null
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = isError,
            supportingText = if (isError && errorMessage != null) {
                { Text(errorMessage, color = AppColors.Error) }
            } else null,
            modifier = modifier,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            suffix = suffix,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp
            )
        )
    }
}
