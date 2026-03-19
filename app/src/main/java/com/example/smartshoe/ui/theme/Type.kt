package com.example.smartshoe.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 应用排版配置
 * 集中管理所有尺寸、文字大小相关的配置参数
 */
object AppTypography {
    // 文字大小
    val TitleTextSize = 20.sp                    // 主标题文字大小
    val SubtitleTextSize = 18.sp                 // 副标题文字大小
    val BodyTextSize = 16.sp                     // 正文文字大小
    val CaptionTextSize = 14.sp                  // 说明文字大小
    val SmallTextSize = 12.sp                    // 小文字大小
    val SensorValueTextSize = 16.sp              // 传感器数值文字大小

    // 行高
    val LineHeightNormal = 18.sp                 // 标准行高
    val LineHeightLarge = 22.sp                  // 大行高
}

/**
 * 应用尺寸配置
 */
object AppDimensions {
    // 间距
    val DefaultPadding = 16.dp                   // 默认内边距
    val SmallPadding = 8.dp                      // 小内边距
    val LargePadding = 24.dp                     // 大内边距

    // 按钮尺寸
    val ButtonWidth = 120.dp                     // 按钮宽度
    val ButtonHeight = 48.dp                     // 按钮高度

    // 图表尺寸
    val ChartHeight = 200.dp                     // 图表高度
    val ChartItemHeight = 180.dp                 // 图表项高度

    // 卡片圆角
    val CardCornerRadius = 12.dp                 // 卡片圆角
    val ButtonCornerRadius = 8.dp                // 按钮圆角

    // 传感器可视化
    val ImageScale = 1.8f                        // 鞋垫图片缩放比例
    val SensorBorderWidth = 2f                   // 传感器区域边框宽度
    val SensorBorderAlpha = 0.5f                 // 传感器区域边框透明度
}
