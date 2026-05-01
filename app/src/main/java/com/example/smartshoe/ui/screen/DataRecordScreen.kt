package com.example.smartshoe.ui.screen

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.ui.screen.datarecord.CombinedChartsCard
import com.example.smartshoe.ui.screen.datarecord.RecordStatusHeader
import com.example.smartshoe.ui.theme.AppColors

/**
 * 数据记录页面主入口
 * 组合所有数据记录相关的组件
 *
 * 架构：主Screen文件只负责组合子组件，具体UI实现拆分到 screen/datarecord/ 目录
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
        RecordStatusHeader(
            connectedDevice = connectedDevice,
            dataPointCount = historicalData.size,
            userWeight = userWeight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        CombinedChartsCard(
            historicalData = historicalData,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
