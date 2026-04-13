package com.example.smartshoe.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors

/**
 * 底部导航栏组件
 * 重构：从根包移动到 ui/component/ 目录
 */
object BottomNavigation {
    /**
     * 底部导航栏组件
     */
    @Composable
     fun BottomNavigationBar(
        selectedTab: Int,
        onTabSelected: (Int) -> Unit
    ) {
        NavigationBar(
            containerColor = AppColors.Background,
            contentColor = AppColors.Primary,
            modifier = Modifier.height(85.dp),
            tonalElevation = 8.dp
        ) {
            // 主页标签
            NavigationBarItem(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Home, // 主页图标
                        contentDescription = "主页",
                        modifier = Modifier.size(24.dp) // 设置图标大小
                    )
                },
                label = {
                    Text(
                        "主页",
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.OnPrimary, // 选中时图标白色
                    selectedTextColor = AppColors.MediumGray, // 选中时文字灰色
                    unselectedIconColor = AppColors.MediumGray, // 未选中时图标灰色
                    unselectedTextColor = AppColors.MediumGray, // 未选中时文字灰色
                    indicatorColor = AppColors.Primary // 选中指示器颜色（深蓝色）
                )
            )

            // 数据记录标签
            NavigationBarItem(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "数据记录"
                    )
                },
                label = {
                    Text(
                        "数据记录",
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.OnPrimary,
                    selectedTextColor = AppColors.MediumGray,
                    unselectedIconColor = AppColors.MediumGray,
                    unselectedTextColor = AppColors.MediumGray,
                    indicatorColor = AppColors.Primary
                )
            )

            // 历史记录标签
            NavigationBarItem(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.history),
                        contentDescription = "历史记录"
                    )
                },
                label = {
                    Text(
                        "历史记录",
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.OnPrimary,
                    selectedTextColor = AppColors.MediumGray,
                    unselectedIconColor = AppColors.MediumGray,
                    unselectedTextColor = AppColors.MediumGray,
                    indicatorColor = AppColors.Primary
                )
            )

            // AI助手标签
            NavigationBarItem(
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "AI助手"
                    )
                },
                label = {
                    Text(
                        "AI助手",
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.OnPrimary,
                    selectedTextColor = AppColors.MediumGray,
                    unselectedIconColor = AppColors.MediumGray,
                    unselectedTextColor = AppColors.MediumGray,
                    indicatorColor = AppColors.Primary
                )
            )

            // 设置标签
            NavigationBarItem(
                selected = selectedTab == 4,
                onClick = { onTabSelected(4) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "设置"
                    )
                },
                label = {
                    Text(
                        "设置",
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.OnPrimary,
                    selectedTextColor = AppColors.MediumGray,
                    unselectedIconColor = AppColors.MediumGray,
                    unselectedTextColor = AppColors.MediumGray,
                    indicatorColor = AppColors.Primary
                )
            )
        }
    }
}
