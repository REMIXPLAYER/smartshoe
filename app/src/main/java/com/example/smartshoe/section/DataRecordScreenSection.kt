package com.example.smartshoe.section

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smartshoe.UIConstants
import com.example.smartshoe.UIConstants.PrimaryColor
import com.example.smartshoe.data.SensorDataPoint
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

object DataRecordScreenSection {
    /**
     * 记录状态头部信息
     */
    @Composable
    fun RecordStatusHeader(
        connectedDevice: BluetoothDevice?,
        dataPointCount: Int,
        modifier: Modifier = Modifier,
        userWeight: Float // 新增体重参数
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "实时数据记录",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UIConstants.PrimaryColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 连接状态和数据信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // 连接状态
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (connectedDevice != null) Color(0xFF4CAF50) else Color.Gray,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = if (connectedDevice != null) {
                                    "${getDeviceDisplayName(connectedDevice)}已连接，自动记录中"
                                } else {
                                    "设备未连接"
                                },
                                fontSize = 14.sp,
                                color = if (connectedDevice != null) Color(0xFF4CAF50) else Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 显示体重信息
                        if (userWeight > 0f) {
                            Text(
                                text = "体重: ${userWeight}kg",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Text(
                            text = "数据点数: $dataPointCount",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                }
            }
        }
    }

    /**
     * 使用MPAndroidChart的图表组件 - 支持拖动查看和点击显示选中值
     * 用户点击/拖动时显示选中值，点击其他地方或2秒后自动归位到最新值
     *
     * 性能优化：
     * 1. 使用derivedStateOf缓存图表数据，避免每次重组重新计算
     * 2. 使用remember缓存entries和lineData，减少对象创建
     * 3. 优化AndroidView update调用频率，仅在数据变化时更新
     */
    @Composable
    fun MPLineChart(
        data: List<SensorDataPoint>,
        sensorIndex: Int,
        sensorName: String,
        modifier: Modifier = Modifier
    ) {
        var selectedValue by remember { mutableStateOf<Int?>(null) }
        var isUserInteracting by remember { mutableStateOf(false) }
        var lastInteractionTime by remember { mutableStateOf(0L) }

        // 使用remember缓存传感器数据提取函数，避免每次重组创建lambda
        val sensorValueExtractor = remember(sensorIndex) {
            { point: SensorDataPoint ->
                when (sensorIndex) {
                    0 -> point.sensor1
                    1 -> point.sensor2
                    2 -> point.sensor3
                    else -> 0
                }
            }
        }

        // 使用derivedStateOf缓存最新值，避免每次重组重新计算
        val latestValue by remember(data) {
            derivedStateOf {
                data.lastOrNull()?.let(sensorValueExtractor) ?: 0
            }
        }

        // 使用derivedStateOf缓存图表显示值，仅在data或selectedValue变化时重新计算
        val displayValue by remember(data, selectedValue) {
            derivedStateOf {
                selectedValue ?: latestValue
            }
        }

        // 缓存时间戳计算
        val firstTimestamp by remember(data) {
            derivedStateOf { data.firstOrNull()?.timestamp ?: 0L }
        }
        val lastTimestamp by remember(data) {
            derivedStateOf { data.lastOrNull()?.timestamp ?: firstTimestamp }
        }

        // 当数据更新且用户未交互时，自动更新选中值为最新值
        LaunchedEffect(data.size) {
            if (!isUserInteracting && data.isNotEmpty()) {
                selectedValue = sensorValueExtractor(data.last())
            }
        }

        // 检查是否需要自动归位（2秒无交互）
        LaunchedEffect(isUserInteracting, lastInteractionTime) {
            if (isUserInteracting) {
                // 启动一个延迟任务，2秒后自动归位
                kotlinx.coroutines.delay(2000)
                // 检查是否仍然是同一个交互会话
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastInteractionTime >= 2000) {
                    isUserInteracting = false
                    // 归位到最新值
                    if (data.isNotEmpty()) {
                        selectedValue = sensorValueExtractor(data.last())
                    }
                }
            }
        }

        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // 标题和当前值/选中值
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sensorName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = UIConstants.PrimaryColor
                    )

                    Text(
                        text = if (isUserInteracting && selectedValue != null) "选中: $displayValue" else "当前: $displayValue",
                        fontSize = 12.sp,
                        color = if (isUserInteracting && selectedValue != null) UIConstants.PrimaryColor else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 使用remember缓存图表数据转换结果，避免每次update重新计算
                    val chartDataState = rememberChartData(data, sensorIndex, isUserInteracting)

                    AndroidView(
                        factory = { context ->
                            LineChart(context).apply {
                                setupChartStyle(firstTimestamp, lastTimestamp)
                                setupInteractionListeners(
                                    onUserInteractionStart = {
                                        isUserInteracting = true
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    onUserInteractionEnd = {
                                        // 不立即结束交互，让LaunchedEffect处理延迟归位
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    onValueSelected = { entry ->
                                        isUserInteracting = true
                                        lastInteractionTime = System.currentTimeMillis()
                                        val targetTime = firstTimestamp + (entry.x * 1000).toLong()
                                        val closestPoint = data.minByOrNull {
                                            kotlin.math.abs(it.timestamp - targetTime)
                                        }
                                        selectedValue = closestPoint?.let(sensorValueExtractor)
                                    }
                                )
                                if (data.isEmpty()) {
                                    setupEmptyChart()
                                }
                            }
                        },
                        update = { chart ->
                            // 使用缓存的图表数据状态，避免重复计算
                            if (chartDataState.entries.isNotEmpty()) {
                                updateChartDataOptimized(chart, chartDataState, isUserInteracting)
                            } else {
                                chart.setupEmptyChart()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

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

    /**
     * 图表数据状态类 - 缓存转换后的图表数据
     */
    data class ChartDataState(
        val entries: List<Entry>,
        val firstTimestamp: Long,
        val lineColor: Color,
        val sensorIndex: Int
    )

    /**
     * 使用remember缓存图表数据转换结果
     * 只在data或sensorIndex变化时重新计算
     */
    @Composable
    private fun rememberChartData(
        data: List<SensorDataPoint>,
        sensorIndex: Int,
        isUserInteracting: Boolean
    ): ChartDataState {
        return remember(data.size, sensorIndex) {
            val entries = ArrayList<Entry>()

            // 只取最近2分钟的数据（120秒 = 120000毫秒）
            val lastTimestamp = data.lastOrNull()?.timestamp ?: 0L
            val twoMinutesAgo = lastTimestamp - 120000
            val recentData = data.filter { it.timestamp >= twoMinutesAgo }

            if (recentData.isEmpty()) {
                return@remember ChartDataState(emptyList(), 0L, Color.Gray, sensorIndex)
            }

            // 使用最近2分钟数据的第一个时间戳作为基准
            val firstTimestamp = recentData.first().timestamp

            recentData.forEach { dataPoint ->
                val value = when (sensorIndex) {
                    0 -> dataPoint.sensor1.toFloat()
                    1 -> dataPoint.sensor2.toFloat()
                    2 -> dataPoint.sensor3.toFloat()
                    else -> 0f
                }
                // X轴为相对时间（秒），相对于2分钟窗口的开始
                val relativeTime = (dataPoint.timestamp - firstTimestamp) / 1000f
                entries.add(Entry(relativeTime, value))
            }

            val lineColor = when (sensorIndex) {
                0 -> Color(0xFFFF6B6B)
                1 -> Color(0xFF4ECDC4)
                2 -> Color(0xFF45B7D1)
                else -> Color(0xFF3949AB)
            }

            ChartDataState(entries, firstTimestamp, lineColor, sensorIndex)
        }
    }


    /**
     * 设置图表样式 - 与历史记录详情页面保持一致
     */
    fun LineChart.setupChartStyle(firstTimestamp: Long = 0L, lastTimestamp: Long = 0L) {
        // 基本配置
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        description.isEnabled = false

        // X轴配置 - 时间轴，与历史记录详情保持一致
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

        // Y轴配置 - 确保网格线始终显示
        val leftAxis: YAxis = axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.LightGray.copy(alpha = 0.3f).toArgb()
        leftAxis.setDrawAxisLine(true)
        leftAxis.textColor = Color.Gray.toArgb()
        leftAxis.textSize = 10f
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 4095f
        leftAxis.granularity = 1000f

        axisRight.isEnabled = false

        // 图例
        legend.isEnabled = false
    }

    /**
     * 设置交互监听器 - 支持拖动查看和自动归位
     */
    fun LineChart.setupInteractionListeners(
        onUserInteractionStart: () -> Unit,
        onUserInteractionEnd: () -> Unit,
        onValueSelected: (Entry) -> Unit
    ) {
        // 添加选中监听器
        setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let { entry ->
                    onUserInteractionStart()
                    onValueSelected(entry)
                }
            }

            override fun onNothingSelected() {
                onUserInteractionEnd()
            }
        })

        // 添加手势监听器
        onChartGestureListener = object : OnChartGestureListener {
            override fun onChartGestureStart(
                me: android.view.MotionEvent?,
                lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?
            ) {
                onUserInteractionStart()
            }

            override fun onChartGestureEnd(
                me: android.view.MotionEvent?,
                lastPerformedGesture: com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture?
            ) {
                // 手势结束时通知，延迟归位逻辑在Compose层处理
                onUserInteractionEnd()
            }

            override fun onChartLongPressed(me: android.view.MotionEvent?) {}
            override fun onChartDoubleTapped(me: android.view.MotionEvent?) {}
            override fun onChartSingleTapped(me: android.view.MotionEvent?) {}
            override fun onChartFling(
                me1: android.view.MotionEvent?,
                me2: android.view.MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ) {
            }

            override fun onChartScale(
                me: android.view.MotionEvent?,
                scaleX: Float,
                scaleY: Float
            ) {
            }

            override fun onChartTranslate(me: android.view.MotionEvent?, dX: Float, dY: Float) {}
        }
    }

    /**
     * 设置空图表样式 - 确保网格线显示
     */
    fun LineChart.setupEmptyChart() {
        // 创建一个空的数据集，但保持图表框架和网格线
        val emptyEntries = ArrayList<Entry>()
        val emptyDataSet = LineDataSet(emptyEntries, "").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 0f  // 设置为0，不显示线条
            color = Color.Gray.toArgb()
        }

        val emptyData = LineData(emptyDataSet)
        data = emptyData

        // 确保坐标轴范围设置正确，使网格线可见
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 10f

        axisLeft.axisMinimum = 0f
        axisLeft.axisMaximum = 4095f

        invalidate()
    }


    /**
     * 优化后的图表数据更新函数 - 使用缓存的ChartDataState
     * 避免在update回调中进行数据转换计算
     */
    fun updateChartDataOptimized(
        chart: LineChart,
        chartDataState: ChartDataState,
        isUserInteracting: Boolean = false
    ) {
        val entries = chartDataState.entries as ArrayList<Entry>
        if (entries.isEmpty()) {
            chart.invalidate()
            return
        }

        val dataSet = LineDataSet(entries, "传感器数据").apply {
            setDrawCircles(false)
            setDrawValues(false)
            lineWidth = 2f
            color = chartDataState.lineColor.toArgb()
            setDrawFilled(true)
            fillColor = chartDataState.lineColor.copy(alpha = 0.2f).toArgb()
            mode = LineDataSet.Mode.LINEAR
        }

        val lineData = LineData(dataSet)
        chart.data = lineData

        // 设置X轴范围为0-120秒（2分钟）
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.axisMaximum = 120f

        // 使用HH:mm:ss格式显示时间
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val firstTimestamp = chartDataState.firstTimestamp

        // 更新X轴时间格式化器
        chart.xAxis.valueFormatter =
            object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val actualTimestamp = firstTimestamp + (value * 1000).toLong()
                    return timeFormat.format(Date(actualTimestamp))
                }
            }

        // 设置标签数量
        chart.xAxis.setLabelCount(5, false)

        // 确保网格线设置正确
        chart.xAxis.setDrawGridLines(true)
        chart.axisLeft.setDrawGridLines(true)

        // 如果用户未交互，自动移动到最新数据
        if (!isUserInteracting) {
            chart.moveViewToX(entries.last().x)
        }

        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    /**
     * 更新图表数据 - 只显示最近2分钟的数据，支持用户交互时不自动滚动
     * 保留此函数供其他调用方使用
     */
    fun updateChartData(
        chart: LineChart,
        data: List<SensorDataPoint>,
        sensorIndex: Int,
        isUserInteracting: Boolean = false
    ) {
        val entries = ArrayList<Entry>()

        // 只取最近2分钟的数据（120秒 = 120000毫秒）
        val lastTimestamp = data.lastOrNull()?.timestamp ?: 0L
        val twoMinutesAgo = lastTimestamp - 120000
        val recentData = data.filter { it.timestamp >= twoMinutesAgo }

        if (recentData.isEmpty()) {
            chart.invalidate()
            return
        }

        // 使用最近2分钟数据的第一个时间戳作为基准
        val firstTimestamp = recentData.first().timestamp

        recentData.forEach { dataPoint ->
            val value = when (sensorIndex) {
                0 -> dataPoint.sensor1.toFloat()
                1 -> dataPoint.sensor2.toFloat()
                2 -> dataPoint.sensor3.toFloat()
                else -> 0f
            }
            // X轴为相对时间（秒），相对于2分钟窗口的开始
            val relativeTime = (dataPoint.timestamp - firstTimestamp) / 1000f
            entries.add(Entry(relativeTime, value))
        }

        val lineColor = when (sensorIndex) {
            0 -> Color(0xFFFF6B6B)
            1 -> Color(0xFF4ECDC4)
            2 -> Color(0xFF45B7D1)
            else -> Color(0xFF3949AB)
        }

        val chartDataState = ChartDataState(entries, firstTimestamp, lineColor, sensorIndex)
        updateChartDataOptimized(chart, chartDataState, isUserInteracting)
    }

    /**
     * 整合的传感器图表卡片
     * 三个图表在一个Card中，使用分隔线分隔，可滚动查看
     */
    @Composable
    fun CombinedChartsCard(
        historicalData: List<SensorDataPoint>,
        modifier: Modifier = Modifier
    ) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = UIConstants.SurfaceColor),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 卡片标题
                Text(
                    text = "实时数据图表",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = UIConstants.PrimaryColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 传感器1图表
                SensorChartItem(
                    data = historicalData,
                    sensorIndex = 0,
                    sensorName = "传感器 1 - 脚掌前部"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 分隔线
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 传感器2图表
                SensorChartItem(
                    data = historicalData,
                    sensorIndex = 1,
                    sensorName = "传感器 2 - 脚弓部"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 分隔线
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 传感器3图表
                SensorChartItem(
                    data = historicalData,
                    sensorIndex = 2,
                    sensorName = "传感器 3 - 脚跟部"
                )
            }
        }
    }

    /**
     * 单个传感器图表项（无Card包装）- 优化版本
     * 使用remember缓存计算结果，避免不必要的重组
     */
    @Composable
    fun SensorChartItem(
        data: List<SensorDataPoint>,
        sensorIndex: Int,
        sensorName: String
    ) {
        // 使用derivedStateOf优化当前值计算，只在数据变化时重新计算
        val currentValue by remember(data.size, sensorIndex) {
            derivedStateOf {
                data.lastOrNull()?.let {
                    when (sensorIndex) {
                        0 -> it.sensor1
                        1 -> it.sensor2
                        2 -> it.sensor3
                        else -> 0
                    }
                } ?: 0
            }
        }

        // 缓存时间戳计算
        val timestamps by remember(data.size) {
            derivedStateOf {
                val first = data.firstOrNull()?.timestamp ?: 0L
                val last = data.lastOrNull()?.timestamp ?: first
                first to last
            }
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = UIConstants.PrimaryColor
                )

                Text(
                    text = "当前: $currentValue",
                    fontSize = 12.sp,
                    color = if (data.isEmpty()) Color.LightGray else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 图表区域 - 使用key优化AndroidView的重建
            val (firstTimestamp, lastTimestamp) = timestamps

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // 使用remember缓存图表数据，避免不必要的update调用
                val chartData by remember(data.size, sensorIndex) {
                    derivedStateOf { data }
                }

                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            setupChartStyle(firstTimestamp, lastTimestamp)
                            if (data.isEmpty()) {
                                setupEmptyChart()
                            }
                        }
                    },
                    update = { chart ->
                        // 只在数据真正变化时更新图表
                        if (chartData.isNotEmpty()) {
                            updateChartData(chart, chartData, sensorIndex)
                        } else {
                            chart.setupEmptyChart()
                        }
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