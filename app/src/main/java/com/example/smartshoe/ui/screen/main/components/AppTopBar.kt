package com.example.smartshoe.ui.screen.main.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors

/**
 * 顶部应用栏组件
 * 显示应用标题和品牌颜色
 * 在所有页面显示左侧菜单和右侧新建对话按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    onMenuClick: () -> Unit,
    onNewConversation: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "举足凝健",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            // 左侧菜单按钮 - 所有页面都显示
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "历史会话",
                    tint = AppColors.OnPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        actions = {
            // 右侧新建对话按钮 - 气泡用OnPrimary，+号用Primary
            IconButton(onClick = onNewConversation) {
                Box(
                    modifier = Modifier.size(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 气泡轮廓 - OnPrimary颜色
                    Icon(
                        painter = painterResource(id = R.drawable.add_conversation_bubble),
                        contentDescription = null,
                        tint = AppColors.OnPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    // +号 - Primary颜色
                    Icon(
                        painter = painterResource(id = R.drawable.add_conversation_plus),
                        contentDescription = "新建对话",
                        tint = AppColors.OnPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppColors.TopAppBar,
            titleContentColor = AppColors.OnPrimary,
            navigationIconContentColor = AppColors.OnPrimary,
            actionIconContentColor = AppColors.OnPrimary
        )
    )
}
