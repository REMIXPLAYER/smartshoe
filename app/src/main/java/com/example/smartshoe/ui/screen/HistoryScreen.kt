package com.example.smartshoe.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.data.model.SensorDataPoint
import com.example.smartshoe.data.model.SensorDataRecord
import com.example.smartshoe.ui.component.chart.ChartConfigUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.example.smartshoe.util.DateTimeUtils
import java.util.Calendar
import java.util.Date

object HistoryScreen {

    /**
     * 历史记录主界面
     */
    @Composable
    fun HistoryScreen(
        records: List<SensorDataRecord>,
        selectedRecord: SensorDataRecord?,
        recordData: List<SensorDataPoint>,
        isLoading: Boolean,
        isRecordDetailLoading: Boolean = false,
        startDate: Date?,
        endDate: Date?,
        onStartDateChange: (Date?) -> Unit,
        onEndDateChange: (Date?) -> Unit,
        onQueryClick: () -> Unit,
        onRecordSelect: (SensorDataRecord?) -> Unit,
        queryExecuted: Boolean = false,
        modifier: Modifier = Modifier,
        onShowDatePicker: ((Date, (Date) -> Unit) -> Unit)? = null
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(16.dp)
        ) {
            // 主内容区域（时间选择器和记录列表）- 始终显示在底层
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DateTimeSelector(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateChange = onStartDateChange,
                    onEndDateChange = onEndDateChange,
                    onQueryClick = onQueryClick,
                    isLoading = isLoading,
                    records = records,
                    selectedRecord = selectedRecord,
                    queryExecuted = queryExecuted,
                    onRecordToggle = { record ->
                        if (selectedRecord?.recordId == record.recordId) {
                            onRecordSelect(null)
                        } else {
                            onRecordSelect(record)
                        }
                    },
                    onShowDatePicker = onShowDatePicker
                )

                // 查询加载指示器
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                }
            }

            // 记录详情页面 - 从右侧滑入滑出
            AnimatedVisibility(
                visible = selectedRecord != null,
                enter = slideInHorizontally(initialOffsetX = { it }), // 从右侧进入
                exit = slideOutHorizontally(targetOffsetX = { it }),   // 向右侧退出
                modifier = Modifier.fillMaxSize()
            ) {
                if (selectedRecord != null) {
                    if (isRecordDetailLoading) {
                        // 记录详情加载中
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppColors.Background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AppColors.Primary)
                        }
                    } else if (recordData.isNotEmpty()) {
                        SelectedRecordDetail(
                            record = selectedRecord,
                            data = recordData,
                            onBackClick = { onRecordSelect(null) }
                        )
                    }
                }
            }
        }
    }

    /**
     * 日期选择器 - 包含记录列表
     */
    @Composable
    private fun DateTimeSelector(
        startDate: Date?,
        endDate: Date?,
        onStartDateChange: (Date?) -> Unit,
        onEndDateChange: (Date?) -> Unit,
        onQueryClick: () -> Unit,
        isLoading: Boolean,
        records: List<SensorDataRecord>,
        selectedRecord: SensorDataRecord?,
        queryExecuted: Boolean = false,
        onRecordToggle: (SensorDataRecord) -> Unit,
        onShowDatePicker: ((Date, (Date) -> Unit) -> Unit)? = null
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "选择时间范围",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 开始和结束时间 - 水平排列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 开始时间
                    OutlinedButton(
                        onClick = {
                            onShowDatePicker?.invoke(startDate ?: Date()) { newDate ->
                                // 设置开始时间为当天的00:00:00
                                val calendar = Calendar.getInstance()
                                calendar.time = newDate
                                calendar.set(Calendar.HOUR_OF_DAY, 0)
                                calendar.set(Calendar.MINUTE, 0)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                                onStartDateChange(calendar.time)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "开始日期",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = startDate?.let { DateTimeUtils.formatDate(it.time) } ?: "未选择",
                                    fontSize = 13.sp,
                                    color = if (startDate != null) Color(0xFF666666) else Color.Gray
                                )
                            }
                        }
                    }

                    // 结束时间
                    OutlinedButton(
                        onClick = {
                            onShowDatePicker?.invoke(endDate ?: Date()) { newDate ->
                                // 设置结束时间为当天的23:59:59
                                val calendar = Calendar.getInstance()
                                calendar.time = newDate
                                calendar.set(Calendar.HOUR_OF_DAY, 23)
                                calendar.set(Calendar.MINUTE, 59)
                                calendar.set(Calendar.SECOND, 59)
                                calendar.set(Calendar.MILLISECOND, 999)
                                onEndDateChange(calendar.time)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "结束日期",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = endDate?.let { DateTimeUtils.formatDate(it.time) } ?: "未选择",
                                    fontSize = 13.sp,
                                    color = if (endDate != null) Color(0xFF666666) else Color.Gray
                                )
                            }
                        }
                    }
                }

                // 清除日期按钮（当选择了日期时显示）
                if (startDate != null || endDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            onStartDateChange(null)
                            onEndDateChange(null)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "清除日期筛选",
                            fontSize = 12.sp,
                            color = AppColors.Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 查询按钮
                Button(
                    onClick = onQueryClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isLoading) "查询中..." else if (startDate == null && endDate == null) "查询所有记录" else "查询历史记录")
                }

                // 记录列表 - 并入时间范围卡片
                if (records.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "查询结果 (${records.size}条记录)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    RecordList(
                        records = records,
                        selectedRecord = selectedRecord,
                        onRecordToggle = onRecordToggle
                    )
                } else if (!isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (queryExecuted) "该时间段内没有数据记录" else "暂无数据，请选择时间范围查询",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    /**
     * 记录列表 - 无外层Card，因为已在DateTimeSelector的Card中
     * 超过5个记录时支持滚动，整体卡片固定
     */
    @Composable
    private fun RecordList(
        records: List<SensorDataRecord>,
        selectedRecord: SensorDataRecord?,
        onRecordToggle: (SensorDataRecord) -> Unit
    ) {
        // 计算单个记录项的高度（约56dp：padding + 内容高度）
        val itemHeight = 56.dp
        // 最大显示5个记录项的高度
        val maxListHeight = itemHeight * 6 + 48.dp // 5个项 + 48dp间距

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxListHeight)
                .verticalScroll(rememberScrollState())
        ) {
            records.forEach { record ->
                val isSelected = selectedRecord?.recordId == record.recordId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { onRecordToggle(record) },
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
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
                            Text(
                                text = DateTimeUtils.formatMonthDayHourMinute(record.startTime),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary
                            )
                            Text(
                                text = "${record.dataCount}个数据点 | 压缩率: ${String.format("%.1f", record.compressionRatio * 100)}%",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        // 勾选图标，仅选中时显示
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已选中",
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 选中记录的详细图表展示
     */
    @Composable
    private fun SelectedRecordDetail(
        record: SensorDataRecord,
        data: List<SensorDataPoint>,
        onBackClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 记录信息标题栏，包含返回按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.leftarrow),
                            contentDescription = "返回",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "记录详情",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 记录信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // 确保开始时间早于结束时间，如果存储反了则交换显示
                        val (displayStartTime, displayEndTime) = if (record.startTime <= record.endTime) {
                            record.startTime to record.endTime
                        } else {
                            record.endTime to record.startTime
                        }

                        // 时间信息
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "开始时间",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = DateTimeUtils.formatDateTime(displayStartTime),
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "结束时间",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = DateTimeUtils.formatDateTime(displayEndTime),
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )

                        // 统计信息 - 与上方时间信息左对齐
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // 数据点
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "${record.dataCount}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary
                                )
                                Text(
                                    text = "数据点",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            // 采样间隔
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "${record.interval}ms",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary
                                )
                                Text(
                                    text = "采样间隔",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            // 压缩率
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "${String.format("%.1f", record.compressionRatio * 100)}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary
                                )
                                Text(
                                    text = "压缩率",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 图表标题
                Text(
                    text = "传感器数据图表",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )

                // 时间范围信息 - 使用记录的原始startTime和endTime计算时长
                // 而不是根据数据点的时间戳，因为数据点时间是客户端根据interval重新计算的
                val durationText = if (data.isNotEmpty()) {
                    // 使用记录的实际时间范围
                    val durationSeconds = (record.endTime - record.startTime) / 1000
                    val durationStr = when {
                        durationSeconds < 60 -> "${durationSeconds}秒"
                        durationSeconds < 3600 -> "${durationSeconds / 60}分${durationSeconds % 60}秒"
                        else -> "${durationSeconds / 3600}小时${(durationSeconds % 3600) / 60}分"
                    }
                    "数据时长: $durationStr | ${data.size}个数据点"
                } else ""

                if (durationText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = durationText,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 传感器1图表
                SensorChartItem(
                    data = data,
                    record = record,
                    sensorIndex = 0,
                    sensorName = "传感器 1 - 脚掌前部",
                    color = Color(0xFFFF6B6B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 传感器2图表
                SensorChartItem(
                    data = data,
                    record = record,
                    sensorIndex = 1,
                    sensorName = "传感器 2 - 脚弓部",
                    color = Color(0xFF4ECDC4)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 分隔线
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 传感器3图表
                SensorChartItem(
                    data = data,
                    record = record,
                    sensorIndex = 2,
                    sensorName = "传感器 3 - 脚跟部",
                    color = Color(0xFF45B7D1)
                )
            }
        }
    }

    /**
     * 单个传感器图表项（无Card包装）
     * 与数据记录页面保持相同设计风格
     * 支持拖动查看时显示选中点的值
     */
    @Composable
    private fun SensorChartItem(
        data: List<SensorDataPoint>,
        record: SensorDataRecord,
        sensorIndex: Int,
        sensorName: String,
        color: Color
    ) {
        // 使用remember保存当前选中的值，默认显示最新值
        var selectedValue by remember(data) {
            mutableIntStateOf(
                data.lastOrNull()?.getValue(sensorIndex) ?: 0
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 标题和当前值
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sensorName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.Primary
                )

                Text(
                    text = "数值: $selectedValue",
                    fontSize = 12.sp,
                    color = if (data.isEmpty()) Color.LightGray else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 图表区域（无Card包装）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // 使用记录的原始startTime和endTime作为时间轴范围
                // 而不是数据点重新计算的时间戳
                val firstTimestamp = record.startTime
                val lastTimestamp = record.endTime
                
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            // 使用 ChartConfigUtils 统一配置图表样式
                            ChartConfigUtils.setupChartStyle(
                                this,
                                firstTimestamp,
                                lastTimestamp,
                                ChartConfigUtils.ChartType.HISTORY
                            )
                            // 添加选中监听器
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                                    e?.let { entry ->
                                        // 使用 ChartConfigUtils 查找最接近的数据点
                                        val closestPoint = ChartConfigUtils.findClosestDataPoint(entry, data, firstTimestamp)
                                        closestPoint?.let { point ->
                                            selectedValue = ChartConfigUtils.getSensorValue(point, sensorIndex)
                                        }
                                    }
                                }

                                override fun onNothingSelected() {
                                    // 当没有选中时，恢复显示最新值
                                    selectedValue = data.lastOrNull()?.let {
                                        ChartConfigUtils.getSensorValue(it, sensorIndex)
                                    } ?: 0
                                }
                            })
                        }
                    },
                    update = { chart ->
                        // 使用 ChartConfigUtils 更新图表数据
                        ChartConfigUtils.updateChartData(
                            chart,
                            data,
                            sensorIndex,
                            firstTimestamp,
                            color,
                            ChartConfigUtils.ChartType.HISTORY
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // 当没有数据时显示提示文本
                if (data.isEmpty()) {
                    Text(
                        text = "暂无数据",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

}
