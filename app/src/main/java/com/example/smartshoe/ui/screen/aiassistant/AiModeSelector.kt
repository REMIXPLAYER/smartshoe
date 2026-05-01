package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.component.ExpandableChevron
import com.example.smartshoe.ui.theme.AppColors

/**
 * AI模式选择器 - 悬浮设计，展开时覆盖消息内容
 * 样式与蓝牙设备管理器保持一致（方案A：现代简洁风）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModeSelectorTopBar(
    enableThinking: Boolean,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onModeSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 60.dp,
                    max = if (isExpanded) 280.dp else 60.dp
                )
        ) {
            // 标题栏 - 统一60.dp高度，与蓝牙卡片保持一致
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onExpandToggle
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI图标 - 使用 minds 图标
                Icon(
                    painter = painterResource(R.drawable.minds),
                    contentDescription = "AI模式",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 主标题 - 统一16.sp SemiBold Primary色
                Text(
                    text = "AI 模式",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Primary,
                    modifier = Modifier.weight(1f)
                )

                // 右侧：当前模式标签 + 展开图标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 当前模式标签 - 使用方案B：蓝紫对比
                    val modeLabel = if (enableThinking) "深度思考" else "快速响应"
                    val modeColor = if (enableThinking)
                        AppColors.AiModeDeep      // 明亮紫 - 深度思考
                    else
                        AppColors.AiModeQuick     // 深蓝 - 快速响应

                    Surface(
                        color = modeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = modeLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = modeColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // 统一展开图标（使用ExpandableChevron）
                    ExpandableChevron(
                        isExpanded = isExpanded,
                        size = 24.dp,
                        tint = AppColors.OnSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // 展开列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    ModeOptionItem(
                        title = "快速响应",
                        subtitle = "快速生成回答，适合日常咨询",
                        isSelected = !enableThinking,
                        onClick = { onModeSelected(false) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ModeOptionItem(
                        title = "深度思考",
                        subtitle = "深入分析问题，适合复杂健康咨询",
                        isSelected = enableThinking,
                        onClick = { onModeSelected(true) }
                    )
                }
            }
        }
    }
}

/**
 * 模式选项项
 */
@Composable
fun ModeOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AppColors.Primary else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) AppColors.Primary else AppColors.OnSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AppColors.CardBackground)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) AppColors.Primary else AppColors.OnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.OnSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
