package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 流式Markdown渲染的防抖间隔（毫秒）
 * 用于减少流式输出时Markdown解析的CPU开销
 */
private const val STREAMING_MARKDOWN_DEBOUNCE_MS = 200L

/**
 * 预处理Markdown文本，修复常见的格式问题
 *
 * 修复规则：
 * 1. 标题符号后添加空格：###1. -> ### 1.
 * 2. 移除多余的空行
 * 3. 统一列表项格式
 */
fun preprocessMarkdown(content: String): String = content
    // 修复标题：在行首匹配1-6个#，如果后面紧跟非空白字符，则添加空格
    .replace(Regex("^(#{1,6})([^#\\s])", RegexOption.MULTILINE), "$1 $2")
    // 修复多个#号后紧跟数字的情况：###1 -> ### 1
    .replace(Regex("(#{3,})(\\d)"), "$1 $2")
    // 移除多余的空行（3个及以上换行符合并为2个）
    .replace(Regex("\n{3,}"), "\n\n")
    // 统一列表项格式：移除前置空格，统一为 "- "
    .replace(Regex("^\\s*[-*]\\s*", RegexOption.MULTILINE), "- ")

/**
 * 自定义Markdown渲染组件，带预处理和样式优化
 *
 * @param markdown Markdown文本内容
 * @param isStreaming 是否为流式输出模式。流式模式下会防抖Markdown渲染以减少CPU开销
 * @param modifier 修饰符
 * @param color 文本颜色
 * @param style 文本样式
 */
@Composable
fun StyledMarkdownText(
    markdown: String,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier,
    color: Color = AppColors.DarkGray,
    style: TextStyle = androidx.compose.ui.text.TextStyle.Default
) {
    // 流式模式下使用防抖渲染，非流式模式下即时渲染
    val displayedMarkdown = if (isStreaming) {
        rememberDebouncedMarkdown(markdown)
    } else {
        remember(markdown) { preprocessMarkdown(markdown) }
    }

    MarkdownText(
        markdown = displayedMarkdown,
        modifier = modifier,
        style = style.copy(
            color = color,
            lineHeight = 22.sp
        ),
        // 自定义链接颜色
        linkColor = AppColors.Primary
    )
}

/**
 * 流式Markdown防抖记忆化
 * 使用debounce限制Markdown重新解析的频率，减少流式输出时的CPU开销
 * 文本内容会累积，但Markdown渲染在停止输入后按间隔更新
 */
@OptIn(FlowPreview::class)
@Composable
private fun rememberDebouncedMarkdown(markdown: String): String {
    var debouncedMarkdown by remember { mutableStateOf(preprocessMarkdown(markdown)) }

    LaunchedEffect(Unit) {
        snapshotFlow { markdown }
            .distinctUntilChanged()
            .debounce(STREAMING_MARKDOWN_DEBOUNCE_MS)
            .collect { latestMarkdown ->
                debouncedMarkdown = preprocessMarkdown(latestMarkdown)
            }
    }

    return debouncedMarkdown
}
