package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smartshoe.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun BlinkingCursor() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            visible = !visible
            delay(500)
        }
    }
    if (visible) {
        Box(
            modifier = Modifier
                .padding(start = 2.dp, bottom = 2.dp)
                .size(width = 2.dp, height = 16.dp)
                .background(AppColors.Cursor)
        )
    }
}
