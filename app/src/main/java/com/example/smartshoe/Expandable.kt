package com.example.smartshoe

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Expandable {
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
                    min = 80.dp, // 最小高度（收起状态）
                    max = if (isExpanded) 400.dp else 80.dp // 展开时增加高度
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 标题栏（始终显示）- 固定高度避免图标移动
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp) // 固定标题栏高度
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
                            tint = UIConstants.PrimaryColor,
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
                                color = UIConstants.OnSurface
                            )
                            Text(
                                text = "v1.0.0 (Build 2025)",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }

                        // 旋转箭头指示展开状态
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = if (isExpanded) 90f else 0f
                                }
                        )
                    }
                }

                // 版本详细信息（展开时显示）
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 250)
                    ) + fadeIn(animationSpec = tween(durationMillis = 250)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 版本详情信息
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

                        // 功能特性标题
                        Text(
                            "功能特性:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = UIConstants.OnSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 功能特性列表
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
     * 版本详情项组件
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
                color = UIConstants.OnSurface,
                fontWeight = FontWeight.Normal
            )
        }
    }

    /**
     * 功能特性列表项组件
     */
    @Composable
     fun FeatureListItem(text: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(UIConstants.PrimaryColor, CircleShape)
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
                // 标题栏
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
                            tint = UIConstants.PrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "使用帮助",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = UIConstants.OnSurface
                            )
                            Text(
                                text = "操作指南与常见问题",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = if (isExpanded) 90f else 0f
                                }
                        )
                    }
                }

                // 帮助内容
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 250)
                    ) + fadeIn(animationSpec = tween(durationMillis = 250)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
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
                            color = UIConstants.OnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "1. 点击主页\"连接\"按钮扫描设备\n" +
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
    /**
     * 可展开的隐私政策项组件（无滚动版本）
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
                    min = 80.dp, // 最小高度（收起状态）
                    max = if (isExpanded) 450.dp else 80.dp // 展开时增加高度
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 标题栏（始终显示）- 固定高度避免图标移动
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp) // 固定标题栏高度
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
                            tint = UIConstants.PrimaryColor,
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
                                color = UIConstants.OnSurface
                            )
                            Text(
                                text = "查看隐私保护条款",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }

                        // 旋转箭头指示展开状态
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (isExpanded) "收起" else "展开",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = if (isExpanded) 90f else 0f
                                }
                        )
                    }
                }

                // 隐私政策详细信息（展开时显示）
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 250)
                    ) + fadeIn(animationSpec = tween(durationMillis = 250)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 200)
                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 隐私政策标题
                        Text(
                            "隐私保护政策:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = UIConstants.OnSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 最后更新日期
                        Text(
                            "最后更新日期: 2025年11月",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 隐私政策内容 - 使用普通Text而不是滚动
                        Text(
                            text = "本应用尊重并保护您的隐私权。我们仅在必要时收集和使用您的个人信息，以提供和改进我们的服务。\n\n" +
                                    "我们收集的信息包括蓝牙设备名称、传感器数据和连接状态，这些信息仅用于实时显示压力分布和生成历史数据图表。所有数据仅在设备本地存储，不会上传到任何远程服务器。\n\n" +
                                    "我们承诺不会与第三方共享您的个人数据，除非获得您的明确同意或法律要求。您有权随时查看、修改或删除您的数据。",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 同意按钮
                        Button(
                            onClick = {
                                // 处理隐私政策同意
                                onExpandedChange(false) // 收起面板
                                // 可以添加同意记录逻辑
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = UIConstants.PrimaryColor)
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
}