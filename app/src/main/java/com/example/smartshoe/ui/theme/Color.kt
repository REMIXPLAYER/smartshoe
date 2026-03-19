package com.example.smartshoe.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 应用颜色配置
 * 集中管理所有颜色相关的配置参数
 */
object AppColors {
    // 主色调
    val Primary = Color(0xFF3949AB)              // 主色调（深蓝色）
    val PrimaryDark = Color(0xFF283593)          // 深色主色调
    val OnPrimary = Color.White                  // 主色上的文字

    // 表面色
    val Surface = Color(0xFFFFFFFF)              // 表面色（白色）
    val OnSurface = Color(0xFF212121)            // 表面上的文字（深灰）

    // 顶部应用栏
    val TopAppBar = Color(0xFF3949AB)            // 顶部应用栏背景色

    // 文字颜色
    val TextDark = Color(0xFF3E2723)             // 深色文字（深棕色）
    val Title = Color(0xFF283593)                // 标题文字颜色

    // 背景色
    val Background = Color.White                 // 应用背景色

    // 状态颜色
    val Success = Color(0xFF4CAF50)              // 成功状态
    val Error = Color(0xFFF44336)                // 错误状态
    val Warning = Color(0xFFFFC107)              // 警告状态

    // 传感器图表颜色
    val Sensor1 = Color(0xFFFF6B6B)              // 传感器1颜色
    val Sensor2 = Color(0xFF4ECDC4)              // 传感器2颜色
    val Sensor3 = Color(0xFF45B7D1)              // 传感器3颜色

    // 图表网格线颜色
    val ChartGrid = Color(0xFFE0E0E0)            // 图表网格线
    val ChartText = Color(0xFF757575)            // 图表文字
}
