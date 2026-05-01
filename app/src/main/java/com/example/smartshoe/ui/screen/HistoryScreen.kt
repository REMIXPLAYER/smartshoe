package com.example.smartshoe.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartshoe.domain.model.SensorDataPoint
import com.example.smartshoe.domain.model.SensorDataRecord
import com.example.smartshoe.ui.screen.history.DateTimeSelector
import com.example.smartshoe.ui.screen.history.SelectedRecordDetail
import com.example.smartshoe.ui.theme.AppColors
import java.util.Date

/**
 * 历史记录主界面
 * 新增：支持AI分析功能
 *
 * 架构：主Screen文件只负责组合子组件，具体UI实现拆分到 screen/history/components/ 目录
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
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

        AnimatedContent(
            targetState = selectedRecord,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 300)
                ) togetherWith
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 300)
                )
            },
            modifier = Modifier.fillMaxSize(),
            label = "record_detail_animation"
        ) { targetRecord ->
            if (targetRecord != null) {
                if (isRecordDetailLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.Background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                } else {
                    SelectedRecordDetail(
                        record = targetRecord,
                        data = recordData,
                        onBackClick = { onRecordSelect(null) },
                        onAiAnalysisClick = onAiAnalysisClick
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
