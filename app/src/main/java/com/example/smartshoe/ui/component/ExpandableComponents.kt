package com.example.smartshoe.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.util.AnimationDefaults

/**
 * 展开组件集合
 * 合并：Expandable.kt + OptimizedExpandable.kt + ExpandableArrow.kt
 */

// ==================== 展开箭头组件 ====================

/**
 * 可复用的展开/收起箭头图标
 */
@Composable
fun ExpandableArrowIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = AppColors.DarkGray,
    size: Dp = 20.dp,
    rotationDegrees: Float = 90f,
    useGraphicsLayer: Boolean = false
) {
    val actualContentDescription = contentDescription
        ?: if (isExpanded) "收起" else "展开"

    if (useGraphicsLayer) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = actualContentDescription,
            tint = tint,
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    rotationZ = if (isExpanded) rotationDegrees else 0f
                }
        )
    } else {
        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) rotationDegrees else 0f,
            animationSpec = tween(
                durationMillis = AnimationDefaults.DURATION_SHORT,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            label = "arrow_rotation"
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = actualContentDescription,
            tint = tint,
            modifier = modifier
                .size(size)
                .rotate(rotation)
        )
    }
}

// ==================== 通用展开组件 ====================

/**
 * 优化的展开头部组件
 */
@OptIn(ExperimentalTransitionApi::class)
@Composable
fun ExpandableHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val transitionState = remember {
        MutableTransitionState(isExpanded).apply { targetState = isExpanded }
    }
    transitionState.targetState = isExpanded

    val transition = rememberTransition(transitionState, label = "expand_transition")
    val arrowRotation by transition.animateFloat(
        label = "arrow_rotation",
        transitionSpec = {
            tween(
                durationMillis = AnimationDefaults.DURATION_SHORT,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        }
    ) { expanded -> if (expanded) 90f else 0f }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = AppColors.DarkGray
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (isExpanded) "收起" else "展开",
            tint = AppColors.DarkGray,
            modifier = Modifier
                .size(20.dp)
                .rotate(arrowRotation)
        )
    }
}

/**
 * 展开内容区域
 */
@Composable
fun ExpandableContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(
            animationSpec = AnimationDefaults.getOptimizedExpandSpec()
        ) + fadeIn(
            animationSpec = tween(AnimationDefaults.currentFadeDuration)
        ),
        exit = shrinkVertically(
            animationSpec = AnimationDefaults.getOptimizedShrinkSpec()
        ) + fadeOut(
            animationSpec = tween(AnimationDefaults.currentFadeDuration)
        )
    ) {
        Column {
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

/**
 * 受控展开项
 */
@Composable
fun ControlledExpandableItem(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ExpandableHeader(
            title = title,
            subtitle = subtitle,
            isExpanded = isExpanded,
            onClick = { onExpandedChange(!isExpanded) },
            leadingIcon = leadingIcon
        )

        ExpandableContent(
            visible = isExpanded,
            content = content
        )
    }
}

// ==================== 具体展开项组件 ====================

/**
 * 可展开的版本信息项组件
 */
@Composable
fun ExpandableVersionItem(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 80.dp,
                max = if (isExpanded) 400.dp else 80.dp
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable { onExpandedChange(!isExpanded) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "版本信息",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "版本信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = "v1.0.0 (Build 2025)",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    ExpandableArrowIcon(
                        isExpanded = isExpanded,
                        size = 24.dp,
                        useGraphicsLayer = true
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = AnimationDefaults.expandTween) +
                        fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                exit = shrinkVertically(animationSpec = AnimationDefaults.shrinkTween) +
                       fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    VersionDetailItem("应用名称", "足底压力可视化")
                    Spacer(modifier = Modifier.height(8.dp))
                    VersionDetailItem("版本号", "v1.0.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    VersionDetailItem("构建日期", "2025年11月")
                    Spacer(modifier = Modifier.height(8.dp))
                    VersionDetailItem("开发者", "SmartShoe Team")

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "功能特性:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.OnSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FeatureListItem("蓝牙压力传感器连接")
                    FeatureListItem("实时压力数据可视化")
                    FeatureListItem("历史数据记录分析")
                    FeatureListItem("多传感器协同监测")
                }
            }
        }
    }
}

/**
 * 可展开的使用帮助项组件
 */
@Composable
fun ExpandableHelpItem(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 80.dp,
                max = if (isExpanded) 250.dp else 80.dp
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable { onExpandedChange(!isExpanded) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.help),
                        contentDescription = "使用帮助",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "使用帮助",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = "操作指南与常见问题",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    ExpandableArrowIcon(
                        isExpanded = isExpanded,
                        size = 24.dp,
                        useGraphicsLayer = true
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = AnimationDefaults.expandTween) +
                        fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                exit = shrinkVertically(animationSpec = AnimationDefaults.shrinkTween) +
                       fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "使用指南:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.OnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 点击主页\"连接\"按钮扫描设备\n" +
                                "2. 选择要连接的蓝牙设备\n" +
                                "3. 查看实时压力数据和图表\n" +
                                "4. 在设置中管理设备连接",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * 可展开的隐私政策项组件
 */
@Composable
fun ExpandablePrivacyItem(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 80.dp,
                max = if (isExpanded) 450.dp else 80.dp
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clickable { onExpandedChange(!isExpanded) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.security),
                        contentDescription = "隐私政策",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "隐私政策",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnSurface
                        )
                        Text(
                            text = "查看隐私保护条款",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    ExpandableArrowIcon(
                        isExpanded = isExpanded,
                        size = 24.dp,
                        useGraphicsLayer = true
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = AnimationDefaults.expandTween) +
                        fadeIn(animationSpec = AnimationDefaults.fadeInTween),
                exit = shrinkVertically(animationSpec = AnimationDefaults.shrinkTween) +
                       fadeOut(animationSpec = AnimationDefaults.fadeOutTween)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "隐私保护政策:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.OnSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "最后更新日期: 2025年11月",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "本应用尊重并保护您的隐私权。我们仅在必要时收集和使用您的个人信息，以提供和改进我们的服务。\n\n" +
                                "我们收集的信息包括蓝牙设备名称、传感器数据和连接状态，这些信息仅用于实时显示压力分布和生成历史数据图表。所有数据仅在设备本地存储，不会上传到任何远程服务器。\n\n" +
                                "我们承诺不会与第三方共享您的个人数据，除非获得您的明确同意或法律要求。您有权随时查看、修改或删除您的数据。",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onExpandedChange(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Text("我已阅读并同意", fontSize = 14.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "继续使用应用即表示您同意上述隐私政策",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ==================== 辅助组件 ====================

/**
 * 版本详情项组件
 * 公共组件，可在其他模块使用
 */
@Composable
fun VersionDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = AppColors.OnSurface,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun FeatureListItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(AppColors.Primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.DarkGray,
            lineHeight = 16.sp
        )
    }
}
