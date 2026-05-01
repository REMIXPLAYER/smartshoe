package com.example.smartshoe.ui.screen.setting.components.quickactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.viewmodel.UploadStatus

/**
 * 备份设置内容
 */
@Composable
fun BackupSettingsContent(
    isLoggedIn: Boolean,
    hasData: Boolean,
    uploadStatus: UploadStatus,
    onBackupClick: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        // 备份状态行（文字在左，按钮/状态在右）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧状态文字
            Column {
                Text(
                    text = "数据备份",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnSurface
                )
                Text(
                    text = when {
                        !isLoggedIn -> "请先登录"
                        !hasData -> "暂无数据"
                        uploadStatus == UploadStatus.UPLOADING -> "上传中..."
                        uploadStatus == UploadStatus.SUCCESS -> "上传成功"
                        uploadStatus == UploadStatus.FAILED -> "上传失败"
                        else -> "点击备份"
                    },
                    fontSize = 12.sp,
                    color = when (uploadStatus) {
                        UploadStatus.SUCCESS -> AppColors.Primary
                        UploadStatus.FAILED -> AppColors.Error
                        else -> AppColors.PlaceholderText
                    }
                )
            }

            // 右侧按钮或状态图标
            when (uploadStatus) {
                UploadStatus.UPLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AppColors.Primary,
                        strokeWidth = 2.dp
                    )
                }
                UploadStatus.SUCCESS -> {
                    // 绿色成功勾选图标
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(AppColors.Success, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = "备份成功",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                UploadStatus.FAILED -> {
                    // 失败重试按钮
                    TextButton(
                        onClick = { if (isLoggedIn && hasData) onBackupClick() },
                        enabled = isLoggedIn && hasData
                    ) {
                        Text(
                            "重试",
                            color = if (isLoggedIn && hasData) AppColors.Error else AppColors.PlaceholderText,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    // 未上传状态显示上传图标（无背景，只改变图标颜色）
                    Icon(
                        painter = painterResource(R.drawable.upload),
                        contentDescription = "备份",
                        tint = if (isLoggedIn && hasData) AppColors.Primary else AppColors.PlaceholderText.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(
                                enabled = isLoggedIn && hasData,
                                onClick = onBackupClick
                            )
                    )
                }
            }
        }
    }
}
