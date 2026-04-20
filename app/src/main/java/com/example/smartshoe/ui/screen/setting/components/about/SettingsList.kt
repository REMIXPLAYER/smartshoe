package com.example.smartshoe.ui.screen.setting.components.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartshoe.ui.screen.setting.components.common.GradientDivider
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppIcons

/**
 * 设置列表组件
 */
@Composable
fun SettingsList(
    isVersionExpanded: Boolean,
    isHelpExpanded: Boolean,
    isPrivacyExpanded: Boolean,
    isClearCacheExpanded: Boolean,
    onVersionExpandedChange: (Boolean) -> Unit,
    onHelpExpandedChange: (Boolean) -> Unit,
    onPrivacyExpandedChange: (Boolean) -> Unit,
    onClearCacheExpandedChange: (Boolean) -> Unit,
    onClearCache: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Background),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 版本信息
            AboutAppItem(
                appIcon = AppIcons.Info,
                title = "版本信息",
                subtitle = "v1.2.0",
                isExpanded = isVersionExpanded,
                onExpandedChange = onVersionExpandedChange
            ) {
                VersionInfoContent()
            }

            GradientDivider()

            // 使用帮助
            AboutAppItem(
                appIcon = AppIcons.Help,
                title = "使用帮助",
                subtitle = "操作指南与常见问题",
                isExpanded = isHelpExpanded,
                onExpandedChange = onHelpExpandedChange
            ) {
                HelpGuideContent()
            }

            GradientDivider()

            // 隐私政策
            AboutAppItem(
                appIcon = AppIcons.Security,
                title = "隐私政策",
                subtitle = "查看隐私保护条款",
                isExpanded = isPrivacyExpanded,
                onExpandedChange = onPrivacyExpandedChange
            ) {
                PrivacyPolicyContent()
            }

            GradientDivider()

            // 清除缓存（红色主题）
            AboutAppItem(
                appIcon = AppIcons.Delete,
                title = "清除缓存",
                subtitle = "清除应用本地数据",
                isExpanded = isClearCacheExpanded,
                onExpandedChange = onClearCacheExpandedChange,
                iconBackground = AppColors.Error.copy(alpha = 0.1f),
                iconTint = AppColors.Error
            ) {
                ClearCacheContent(onClearCache = onClearCache)
            }
        }
    }
}
