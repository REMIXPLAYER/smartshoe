package com.example.smartshoe.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.screen.history.DateTimeSelector
import com.example.smartshoe.ui.screen.history.SelectedRecordDetail
import com.example.smartshoe.ui.theme.AppColors
import java.util.Date

/**
 * 历史记录主界面
 * 支持AI分析功能
 *
 * 架构：主Screen文件只负责组合子组件，具体UI实现拆分到 screen/history/ 目录
 * 详情页覆盖在卡片上方（非全屏），从右侧滑入/滑出
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
    onAiAnalysisClick: ((String) -> Unit)? = null,
    queryExecuted: Boolean = false,
    modifier: Modifier = Modifier,
    onShowDatePicker: ((Date, (Date) -> Unit) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 卡片容器：详情页在此 Box 内覆盖展示，区域受卡片约束而非全屏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
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

                // 详情覆盖层：在卡片区域内从右侧滑入/滑出
                // 使用 remember + var 保存记录值，确保动画期间内容不消失
                var lastSelectedRecord by remember { mutableStateOf<SensorDataRecord?>(null) }
                if (selectedRecord != null) {
                    lastSelectedRecord = selectedRecord
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedRecord != null,
                    enter = slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.TopStart)
                ) {
                    lastSelectedRecord?.let { record ->
                        if (isRecordDetailLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AppColors.Surface),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AppColors.Primary)
                            }
                        } else {
                            SelectedRecordDetail(
                                record = record,
                                data = recordData,
                                onBackClick = { onRecordSelect(null) },
                                onAiAnalysisClick = onAiAnalysisClick
                            )
                        }
                    }
                }
            }

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
    }
}
