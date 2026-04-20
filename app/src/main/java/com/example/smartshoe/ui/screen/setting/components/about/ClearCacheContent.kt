package com.example.smartshoe.ui.screen.setting.components.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors

/**
 * 清除缓存内容 - 图标化列表展示
 */
@Composable
fun ClearCacheContent(onClearCache: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 警告卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.Error.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AppColors.Error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "清除缓存将删除以下数据，此操作不可恢复",
                    fontSize = 12.sp,
                    color = AppColors.Error.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }

        // 数据列表
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.LightGray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ClearCacheItem(icon = Icons.Default.Person, text = "用户登录信息")
                ClearCacheItem(icon = R.drawable.bluetooth, text = "蓝牙设备列表")
                ClearCacheItem(icon = R.drawable.history, text = "传感器历史数据")
                ClearCacheItem(icon = R.drawable.man, text = "体重设置")
                ClearCacheItem(icon = R.drawable.analytics, text = "临时文件")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 清除按钮
        Button(
            onClick = onClearCache,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "清除",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("确认清除缓存", color = AppColors.Background, fontSize = 14.sp)
        }
    }
}

/**
 * 清除缓存项组件
 */
@Composable
fun ClearCacheItem(icon: Any, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (icon) {
            is Int -> {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = AppColors.DarkGray.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                    contentDescription = null,
                    tint = AppColors.DarkGray.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = AppColors.DarkGray
        )
    }
}
