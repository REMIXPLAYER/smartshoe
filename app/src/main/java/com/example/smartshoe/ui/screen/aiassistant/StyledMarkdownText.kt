package com.example.smartshoe.ui.screen.aiassistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.example.smartshoe.ui.theme.AppColors
import dev.jeziellago.compose.markdowntext.MarkdownText

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
 */
@Composable
fun StyledMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = AppColors.DarkGray,
    style: TextStyle = androidx.compose.ui.text.TextStyle.Default
) {
    val processedMarkdown = remember(markdown) { preprocessMarkdown(markdown) }

    MarkdownText(
        markdown = processedMarkdown,
        modifier = modifier,
        style = style.copy(
            color = color,
            lineHeight = 22.sp
        ),
        // 自定义链接颜色
        linkColor = AppColors.Primary
    )
}
