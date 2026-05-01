package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors

/**
 * 滚动到底部按钮
 */
@Composable
fun ScrollToBottomButton(
    onClick: () -> Unit,
    isStreaming: Boolean
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .shadow(4.dp, CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = AppColors.Surface,
            contentColor = AppColors.Primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "滚动到底部",
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isStreaming) "新消息" else "回到底部",
            fontSize = 14.sp
        )
    }
}
