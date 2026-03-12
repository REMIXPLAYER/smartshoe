package com.example.smartshoe.section

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smartshoe.R
import com.example.smartshoe.UIConstants
import com.example.smartshoe.data.SensorDataPoint
import com.example.smartshoe.data.remote.SensorDataRecord
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat
import java.util.*

object HistoryScreenSection {

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
        modifier: Modifier = Modifier
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
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
                    }
                )

                // 查询加载指示器
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = UIConstants.PrimaryColor)
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
                                .background(UIConstants.SurfaceColor),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = UIConstants.PrimaryColor)
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
        onRecordToggle: (SensorDataRecord) -> Unit
    ) {
        val context = LocalContext.current
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "选择时间范围",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = UIConstants.PrimaryColor,
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
                            showDatePicker(context, startDate ?: Date()) { newDate ->
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
                                    text = startDate?.let { dateFormat.format(it) } ?: "未选择",
                                    fontSize = 13.sp,
                                    color = if (startDate != null) Color(0xFF666666) else Color.Gray
                                )
                            }
                        }
                    }

                    // 结束时间
                    OutlinedButton(
                        onClick = {
                            showDatePicker(context, endDate ?: Date()) { newDate ->
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
                                    text = endDate?.let { dateFormat.format(it) } ?: "未选择",
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
                            color = UIConstants.PrimaryColor
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
                        containerColor = UIConstants.PrimaryColor
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
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

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
                                text = dateFormat.format(Date(record.startTime)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = UIConstants.PrimaryColor
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
                                tint = UIConstants.PrimaryColor,
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor)
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
                            tint = UIConstants.PrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "记录详情",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = UIConstants.PrimaryColor
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
                                text = dateFormat.format(Date(displayStartTime)),
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
                                text = dateFormat.format(Date(displayEndTime)),
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
                                    color = UIConstants.PrimaryColor
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
                                    color = UIConstants.PrimaryColor
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
                                    color = UIConstants.PrimaryColor
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
                    color = UIConstants.PrimaryColor
                )

                // 时间范围信息 - 使用记录的原始startTime和endTime计算时长
                // 而不是根据数据点的时间戳，因为数据点时间戳是客户端根据interval重新计算的
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
                data.lastOrNull()?.let {
                    when (sensorIndex) {
                        0 -> it.sensor1
                        1 -> it.sensor2
                        2 -> it.sensor3
                        else -> 0
                    }
                } ?: 0
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
                    color = UIConstants.PrimaryColor
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
                            setupHistoryChartStyle(firstTimestamp, lastTimestamp)
                            // 添加选中监听器
                            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                override fun onValueSelected(e: Entry?, h: Highlight?) {
                                    e?.let { entry ->
                                        // 根据X轴时间找到对应的数据点
                                        val targetTime = firstTimestamp + (entry.x * 1000).toLong()
                                        
                                        // 找到最接近的数据点
                                        val closestPoint = data.minByOrNull { 
                                            kotlin.math.abs(it.timestamp - targetTime) 
                                        }
                                        
                                        closestPoint?.let { point ->
                                            selectedValue = when (sensorIndex) {
                                                0 -> point.sensor1
                                                1 -> point.sensor2
                                                2 -> point.sensor3
                                                else -> 0
                                            }
                                        }
                                    }
                                }

                                override fun onNothingSelected() {
                                    // 当没有选中时，恢复显示最新值
                                    selectedValue = data.lastOrNull()?.let {
                                        when (sensorIndex) {
                                            0 -> it.sensor1
                                            1 -> it.sensor2
                                            2 -> it.sensor3
                                            else -> 0
                                        }
                                    } ?: 0
                                }
                            })
                        }
                    },
                    update = { chart ->
                        updateHistoryChartData(chart, data, record, sensorIndex, color)
                        // 根据数据时长动态选择时间格式
                        val totalDurationSeconds = (lastTimestamp - firstTimestamp) / 1000f
                        val timeFormat = when {
                            totalDurationSeconds < 3600 -> SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            else -> SimpleDateFormat("HH:mm", Locale.getDefault())
                        }
                        // 更新X轴格式化器的起始时间
                        chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                val actualTimestamp = firstTimestamp + (value * 1000).toLong()
                                return timeFormat.format(Date(actualTimestamp))
                            }
                        }
                        chart.invalidate()
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

    /**
     * 设置历史图表样式 - 支持拖动和缩放
     */
    private fun LineChart.setupHistoryChartStyle(firstTimestamp: Long = 0L, lastTimestamp: Long = 0L) {
        // 启用触摸和拖动
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        isDoubleTapToZoomEnabled = true

        // 描述
        description.isEnabled = false

        // X轴配置 - 时间轴
        val xAxis: XAxis = xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.LightGray.copy(alpha = 0.3f).toArgb()
        xAxis.setDrawAxisLine(true)
        xAxis.textColor = Color.Gray.toArgb()
        xAxis.textSize = 8f
        xAxis.labelRotationAngle = -45f // 旋转标签避免重叠
        
        // 计算数据总时长（秒）
        val totalDurationSeconds = if (lastTimestamp > firstTimestamp) {
            (lastTimestamp - firstTimestamp) / 1000f
        } else {
            0f
        }
        
        // 根据数据时长动态调整标签数量和格式
        val (labelCount, timeFormat) = when {
            totalDurationSeconds < 60 -> { // 小于1分钟，显示秒
                Pair(5, SimpleDateFormat("HH:mm:ss", Locale.getDefault()))
            }
            totalDurationSeconds < 600 -> { // 小于10分钟，显示分钟和秒
                Pair(5, SimpleDateFormat("HH:mm:ss", Locale.getDefault()))
            }
            totalDurationSeconds < 3600 -> { // 小于1小时，显示分钟
                Pair(6, SimpleDateFormat("HH:mm", Locale.getDefault()))
            }
            else -> { // 大于1小时，显示小时和分钟
                Pair(6, SimpleDateFormat("HH:mm", Locale.getDefault()))
            }
        }
        
        xAxis.setLabelCount(labelCount, false)
        
        // 使用自定义格式化器显示时间
        xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                // value 是相对时间（秒），转换为实际时间戳
                val actualTimestamp = firstTimestamp + (value * 1000).toLong()
                return timeFormat.format(Date(actualTimestamp))
            }
        }

        // Y轴配置 - 压力数值
        val leftAxis: YAxis = axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.LightGray.copy(alpha = 0.3f).toArgb()
        leftAxis.setDrawAxisLine(true)
        leftAxis.textColor = Color.Gray.toArgb()
        leftAxis.textSize = 10f
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 4095f
        leftAxis.labelCount = 5

        axisRight.isEnabled = false

        // 图例
        legend.isEnabled = false
    }

    /**
     * 更新历史图表数据
     * 使用record.startTime作为基准时间，确保X轴显示正确的时间范围
     */
    private fun updateHistoryChartData(
        chart: LineChart,
        data: List<SensorDataPoint>,
        record: SensorDataRecord,
        sensorIndex: Int,
        lineColor: Color
    ) {
        if (data.isEmpty()) return

        val entries = ArrayList<Entry>()
        // 使用记录的原始startTime作为基准，而不是数据点的时间戳
        val baseTimestamp = record.startTime

        data.forEachIndexed { index, dataPoint ->
            val value = when (sensorIndex) {
                0 -> dataPoint.sensor1.toFloat()
                1 -> dataPoint.sensor2.toFloat()
                2 -> dataPoint.sensor3.toFloat()
                else -> 0f
            }
            // X轴为相对时间（秒），基于记录的startTime
            // 数据点的时间戳是重新计算的：startTime + (i * interval)
            val relativeTime = (dataPoint.timestamp - baseTimestamp) / 1000f
            entries.add(Entry(relativeTime, value))
        }

        val dataSet = LineDataSet(entries, "传感器数据").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            this.color = lineColor.toArgb()
            setDrawFilled(true)
            fillColor = lineColor.copy(alpha = 0.2f).toArgb()
            mode = LineDataSet.Mode.LINEAR
        }

        val lineData = LineData(dataSet)
        chart.data = lineData

        // 自动缩放以显示所有数据
        chart.fitScreen()
        chart.invalidate()
    }

    /**
     * 显示日期选择器（仅日期，无时间）
     */
    private fun showDatePicker(
        context: android.content.Context,
        initialDate: Date,
        onDateSelected: (Date) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.time = initialDate

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
