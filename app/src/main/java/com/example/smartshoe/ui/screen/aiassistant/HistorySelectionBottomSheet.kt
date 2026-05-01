package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.util.DateTimeUtils

/**
 * 历史记录选择底部Sheet
 * 设计：固定高度展开，内部独立滚动
 * - Sheet高度固定为屏幕高度的60%，不可拖动改变高度
 * - 内部LazyColumn独立处理滚动，不与Sheet手势冲突
 * - 使用ModalBottomSheet的skipPartiallyExpanded=true确保直接展开到固定高度
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySelectionBottomSheet(
    records: List<SensorDataRecord>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRecordSelected: (SensorDataRecord) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val sheetHeight = screenHeight * 0.6f

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // 固定高度容器，内部独立滚动
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
        ) {
            // 标题栏 - 固定顶部，不随列表滚动
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "选择历史记录",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    // 仅在非加载状态下显示记录数量
                    if (!isLoading) {
                        Text(
                            text = "${records.size} 条记录",
                            fontSize = 12.sp,
                            color = AppColors.PlaceholderText
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = AppColors.OnSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // 说明文字 - 固定顶部
            Text(
                text = "选择一个历史记录进行AI健康分析",
                fontSize = 14.sp,
                color = AppColors.OnSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            )

            // 内容区域 - 使用LazyColumn独立滚动
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppColors.Primary)
                        }
                    }
                    records.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = AppColors.OnSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "暂无历史记录",
                                    fontSize = 16.sp,
                                    color = AppColors.OnSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "请先采集一些数据",
                                    fontSize = 14.sp,
                                    color = AppColors.OnSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = records,
                                key = { it.recordId }
                            ) { record ->
                                HistoryRecordItem(
                                    record = record,
                                    onClick = { onRecordSelected(record) }
                                )
                            }

                            // 底部留白，确保最后一条记录可完全显示（考虑系统导航栏）
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 历史记录项 - 与HistoryScreen样式保持一致
 * 右侧添加"分析"按钮用于AI助手页面
 */
@Composable
fun HistoryRecordItem(
    record: SensorDataRecord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 与HistoryScreen使用相同的时间格式
                Text(
                    text = DateTimeUtils.formatMonthDayHourMinute(record.startTime),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
                // 与HistoryScreen使用相同的副标题格式：数据点 + 压缩率
                Text(
                    text = "${record.dataCount}个数据点 | 压缩率: ${String.format("%.1f", record.compressionRatio * 100)}%",
                    fontSize = 12.sp,
                    color = AppColors.PlaceholderText
                )
            }

            // 右侧"分析"按钮
            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AppColors.Primary.copy(alpha = 0.1f),
                    contentColor = AppColors.Primary
                )
            ) {
                Text("分析", fontSize = 13.sp)
            }
        }
    }
}
