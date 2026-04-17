package com.example.smartshoe.ui.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.ui.component.chart.ChartConfigUtils
import com.example.smartshoe.ui.component.getDeviceDisplayName
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

object DataRecordScreen {
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
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "实时数据记录",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
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
                                        color = if (connectedDevice != null) AppColors.Primary else AppColors.DarkGray,
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
                                color = if (connectedDevice != null) AppColors.Primary else AppColors.DarkGray,
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
                point.getValue(sensorIndex)
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

        // 计算实时图表的基准时间戳（最近2分钟数据的第一个时间戳）
        val realtimeBaseTimestamp by remember(data) {
            derivedStateOf {
                val last = data.lastOrNull()?.timestamp ?: 0L
                val twoMinutesAgo = last - 120000L // 2分钟 = 120000毫秒
                data.firstOrNull { it.timestamp >= twoMinutesAgo }?.timestamp ?: firstTimestamp
            }
        }

        // 修复：使用 data 作为 key，确保数据变化时触发
        LaunchedEffect(data) {
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
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
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
                        color = AppColors.Primary
                    )

                    Text(
                        text = if (isUserInteracting && selectedValue != null) "选中: $displayValue" else "当前: $displayValue",
                        fontSize = 12.sp,
                        color = if (isUserInteracting && selectedValue != null) AppColors.Primary else Color.Gray,
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
                    AndroidView(
                        factory = { context ->
                            LineChart(context).apply {
                                // 使用 ChartConfigUtils 统一配置图表样式
                                ChartConfigUtils.setupChartStyle(
                                    this,
                                    firstTimestamp,
                                    lastTimestamp,
                                    ChartConfigUtils.ChartType.REALTIME
                                )
                                // 设置选中监听器
                                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                                    override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                                        e?.let { entry ->
                                            isUserInteracting = true
                                            lastInteractionTime = System.currentTimeMillis()
                                            // 使用实时图表的基准时间戳来查找数据点
                                            val closestPoint = ChartConfigUtils.findClosestDataPoint(entry, data, realtimeBaseTimestamp)
                                            selectedValue = closestPoint?.let { sensorValueExtractor(it) }
                                        }
                                    }
                                    override fun onNothingSelected() {
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                })
                                if (data.isEmpty()) {
                                    ChartConfigUtils.setupEmptyChart(this)
                                }
                            }
                        },
                        update = { chart ->
                            // 使用 ChartConfigUtils 更新图表数据
                            if (data.isNotEmpty()) {
                                ChartConfigUtils.updateChartData(
                                    chart,
                                    data,
                                    sensorIndex,
                                    firstTimestamp,
                                    ChartConfigUtils.SensorColors.getColor(sensorIndex),
                                    ChartConfigUtils.ChartType.REALTIME
                                )
                                // 如果用户未交互，自动移动到最新数据
                                if (!isUserInteracting) {
                                    chart.moveViewToX(chart.data.getDataSetByIndex(0).xMax)
                                }
                            } else {
                                ChartConfigUtils.setupEmptyChart(chart)
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
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
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
                    color = AppColors.Primary
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
        // 修复：使用 data 本身作为 remember 的 key，确保数据变化时重新计算
        // 使用 derivedStateOf 优化，只在数据内容变化时重新计算
        val currentValue by remember(data) {
            derivedStateOf {
                data.lastOrNull()?.getValue(sensorIndex) ?: 0
            }
        }

        // 修复：同样使用 data 作为 key
        val timestamps by remember(data) {
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
                    color = AppColors.Primary
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
                // 修复：使用 data 作为 key，确保图表数据实时更新
                // 使用 remember 缓存，避免不必要的对象创建
                val chartData = remember(data) { data }

                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            // 使用 ChartConfigUtils 统一配置图表样式
                            ChartConfigUtils.setupChartStyle(
                                this,
                                firstTimestamp,
                                lastTimestamp,
                                ChartConfigUtils.ChartType.REALTIME
                            )
                            if (data.isEmpty()) {
                                ChartConfigUtils.setupEmptyChart(this)
                            }
                        }
                    },
                    update = { chart ->
                        // 修复：每次 update 都重新获取最新数据
                        if (chartData.isNotEmpty()) {
                            ChartConfigUtils.updateChartData(
                                chart,
                                chartData,
                                sensorIndex,
                                firstTimestamp,
                                ChartConfigUtils.SensorColors.getColor(sensorIndex),
                                ChartConfigUtils.ChartType.REALTIME
                            )
                        } else {
                            ChartConfigUtils.setupEmptyChart(chart)
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

    /**
     * 数据记录页面主入口
     * 组合所有数据记录相关的组件
     */
    @Composable
    fun DataRecordScreen(
        modifier: Modifier = Modifier,
        historicalData: List<SensorDataPoint>,
        connectedDevice: BluetoothDevice?,
        userWeight: Float
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态信息栏
            RecordStatusHeader(
                connectedDevice = connectedDevice,
                dataPointCount = historicalData.size,
                userWeight = userWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // 三个传感器图表整合在一个卡片中
            CombinedChartsCard(
                historicalData = historicalData,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
