package com.example.smartshoe.ui.screen.setting.components.header

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.domain.model.UserState
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.viewmodel.UploadStatus

/**
 * 用户信息头部组件
 */
@Composable
fun ProfileHeader(
    userState: UserState,
    connectedDevice: BluetoothDevice?,
    userWeight: Float,
    uploadStatus: UploadStatus,
    onEditProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AppColors.Primary, AppColors.PrimaryDark)
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // 用户信息（可点击）
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEditProfileClick
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(AppColors.Background, CircleShape)
                    .border(3.dp, AppColors.Background.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userState.username.isNotEmpty())
                        userState.username.take(1).uppercase()
                    else "?",
                    color = AppColors.Primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (userState.isLoggedIn) userState.username else "未登录",
                    color = AppColors.Background,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (userState.isLoggedIn) userState.email else "点击登录账户",
                    color = AppColors.Background.copy(alpha = 0.85f),
                    fontSize = 14.sp
                )
            }

            // 编辑资料按钮（仅登录后显示）
            if (userState.isLoggedIn) {
                IconButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = AppColors.Background,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // 未登录时显示箭头提示
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "登录",
                    tint = AppColors.Background.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 悬浮统计卡片
        StatsCard(
            isConnected = connectedDevice != null,
            weight = userWeight,
            uploadStatus = uploadStatus,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .offset(y = 40.dp)
        )
    }
}
