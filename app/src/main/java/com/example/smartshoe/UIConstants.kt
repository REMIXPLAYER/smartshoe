package com.example.smartshoe

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * UI配置常量对象，集中管理所有界面相关的配置参数
 * 便于统一修改和维护UI样式
 */
object UIConstants {
    // 颜色配置
    val TopAppBarColor = Color(0xFF3949AB)        // 顶部应用栏背景色（深蓝色）
    val TextColorDark = Color(0xFF3E2723)          // 深色文字颜色（深棕色）
    val TitleColor = Color(0xFF283593)              // 标题文字颜色（蓝色）
    val BackgroundColor = Color.White              // 应用背景色（白色）
    // 在UIConstants中添加新的颜色配置
    val PrimaryColor = Color(0xFF3949AB)        // 主色调
    val SurfaceColor = Color(0xFFFFFFFF)        // 表面色

    val OnPrimary = Color.White                 // 主色上的文字
    val OnSurface = Color(0xFF212121)           // 表面上的文字

    // 尺寸配置
    val ImageScale = 1.8f                          // 鞋垫图片缩放比例
    val SensorBorderWidth = 2f                     // 传感器区域边框宽度
    val SensorBorderAlpha = 0.5f                   // 传感器区域边框透明度

    // 间距配置
    val DefaultPadding = 16.dp                     // 默认内边距
    val SmallPadding = 8.dp                        // 小内边距

    // 文字大小
    val TitleTextSize = 20.sp                      // 主标题文字大小
    val SensorValueTextSize = 16.sp                // 传感器数值文字大小



    // 新增按钮尺寸配置
    val ButtonWidth = 120.dp                       // 按钮宽度
}