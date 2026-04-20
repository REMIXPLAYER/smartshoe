package com.example.smartshoe.ui.screen.setting.components.quickactions

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.ui.theme.AppColors

/**
 * 体重设置内容
 */
@Composable
fun WeightSettingsContent(
    isEditingWeight: Boolean,
    weightInput: String,
    userWeight: Float,
    onWeightInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.LightGray.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        if (isEditingWeight) {
            // 编辑模式
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "体重：",
                    fontSize = 16.sp,
                    color = AppColors.OnSurface
                )
                BasicTextField(
                    value = weightInput,
                    onValueChange = onWeightInputChange,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 16.sp,
                        color = AppColors.OnSurface
                    ),
                    modifier = Modifier
                        .width(80.dp)
                        .height(40.dp)
                        .border(
                            width = 1.dp,
                            color = AppColors.Primary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    text = " kg",
                    fontSize = 16.sp,
                    color = AppColors.OnSurface
                )

                Spacer(modifier = Modifier.weight(1f))

                // 确认按钮
                IconButton(onClick = onConfirm) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = "确认",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 取消按钮
                IconButton(onClick = onCancel) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "取消",
                        tint = AppColors.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // 显示当前体重
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "当前体重：",
                    fontSize = 16.sp,
                    color = AppColors.OnSurface
                )
                Text(
                    text = if (userWeight > 0) "${userWeight} kg" else "未设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (userWeight > 0) AppColors.Primary else AppColors.DarkGray
                )
            }
        }
    }
}
