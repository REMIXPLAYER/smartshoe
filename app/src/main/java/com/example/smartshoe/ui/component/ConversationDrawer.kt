package com.example.smartshoe.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartshoe.R
import com.example.smartshoe.domain.model.AiConversation
import com.example.smartshoe.ui.theme.AppColors
import com.example.smartshoe.ui.theme.AppDimensions
import com.example.smartshoe.util.DateTimeUtils
import java.util.*

/**
 * 对话列表抽屉
 * 从左侧滑出的对话历史列表，支持搜索和时间分类
 */
@Composable
fun ConversationDrawer(
    isOpen: Boolean,
    conversations: List<AiConversation>,
    currentConversationId: String?,
    searchKeyword: String,
    onSearchChange: (String) -> Unit,
    onConversationClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onClose: () -> Unit
) {
    // 遮罩层
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.DarkGray.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        )
    }

    // 抽屉内容
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(tween(300)) { -it },
        exit = slideOutHorizontally(tween(300)) { -it }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .background(
                        AppColors.Background,
                        shape = RoundedCornerShape(
                            topEnd = AppDimensions.CardCornerRadius,
                            bottomEnd = AppDimensions.CardCornerRadius
                        )
                    )
                    .padding(16.dp)
            ) {
                // 顶部标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "历史会话",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Title
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = AppColors.OnSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 新建对话按钮
                Button(
                    onClick = {
                        onNewConversation()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("新建对话", fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 搜索框
                SearchBar(
                    keyword = searchKeyword,
                    onKeywordChange = onSearchChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 对话列表
                ConversationList(
                    conversations = conversations,
                    currentConversationId = currentConversationId,
                    onConversationClick = { id ->
                        onConversationClick(id)
                        onClose()
                    },
                    onDeleteConversation = onDeleteConversation
                )
            }
        }
    }
}

/**
 * 搜索栏
 */
@Composable
private fun SearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit
) {
    BasicTextField(
        value = keyword,
        onValueChange = { onKeywordChange(it.take(100)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = AppColors.OnSurface
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = AppColors.Surface,
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = AppColors.PlaceholderText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    innerTextField()
                    if (keyword.isEmpty()) {
                        Text(
                            text = "搜索对话...",
                            fontSize = 14.sp,
                            color = AppColors.PlaceholderText
                        )
                    }
                }
                if (keyword.isNotEmpty()) {
                    IconButton(
                        onClick = { onKeywordChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除",
                            tint = AppColors.PlaceholderText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

/**
 * 对话列表（带时间分类）
 */
@Composable
private fun ConversationList(
    conversations: List<AiConversation>,
    currentConversationId: String?,
    onConversationClick: (String) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    // 按时间分类
    val groupedConversations = remember(conversations) {
        groupConversationsByTime(conversations)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groupedConversations.forEach { (category, items) ->
            // 分类标题
            item(key = "header_$category") {
                Text(
                    text = category,
                    fontSize = 12.sp,
                    color = AppColors.PlaceholderText,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 对话项
            items(
                items = items,
                key = { it.id }
            ) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    isSelected = conversation.id == currentConversationId,
                    onClick = { onConversationClick(conversation.id) },
                    onDelete = { onDeleteConversation(conversation.id) }
                )
            }
        }
    }
}

/**
 * 单个对话项
 * 删除交互与体重设置保持一致：点击删除图标后显示勾选/取消按钮
 */
@Composable
private fun ConversationItem(
    conversation: AiConversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(isSelected) {
        if (!isSelected) {
            isDeleting = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                enabled = !isDeleting
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                AppColors.Primary.copy(alpha = 0.1f)
            } else {
                AppColors.CardBackground
            }
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    text = conversation.title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) AppColors.Primary else AppColors.Title,
                    maxLines = 1
                )
                Text(
                    text = DateTimeUtils.formatMonthDayHourMinute(conversation.updatedAt),
                    fontSize = 11.sp,
                    color = AppColors.PlaceholderText
                )
            }

            // 删除/确认/取消按钮区域
            if (isDeleting) {
                // 确认删除按钮
                IconButton(
                    onClick = {
                        onDelete()
                        isDeleting = false
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = "确认删除",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 取消删除按钮
                IconButton(
                    onClick = { isDeleting = false },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "取消删除",
                        tint = AppColors.PlaceholderText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // 删除按钮
                IconButton(
                    onClick = { isDeleting = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "删除",
                        tint = AppColors.PlaceholderText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 按时间分类对话
 */
private fun groupConversationsByTime(
    conversations: List<AiConversation>
): Map<String, List<AiConversation>> {
    val now = System.currentTimeMillis()
    val oneDay = 24 * 60 * 60 * 1000L
    val sevenDays = 7 * oneDay
    val thirtyDays = 30 * oneDay

    return conversations.groupBy { conversation ->
        val diff = now - conversation.updatedAt
        when {
            diff < oneDay -> "今天"
            diff < sevenDays -> "前7天"
            diff < thirtyDays -> "前30天"
            else -> "更早"
        }
    }
}
