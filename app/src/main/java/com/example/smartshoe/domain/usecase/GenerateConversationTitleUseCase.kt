package com.example.smartshoe.domain.usecase

import javax.inject.Inject

/**
 * 生成对话标题用例
 * 基于用户输入内容进行智能摘要，零AI调用
 *
 * 规则：
 * 1. 去除无意义前缀（帮我、请、一下等）
 * 2. 提取核心语义，限制在15字以内
 * 3. 保留关键信息：动作 + 对象
 */
class GenerateConversationTitleUseCase @Inject constructor() {

    companion object {
        private const val MAX_TITLE_LENGTH = 15

        // 需要去除的无意义前缀词
        private val PREFIX_WORDS = listOf(
            "帮我", "请", "麻烦", "能不能", "能不能帮我", "可以", "可以帮我",
            "我想", "我想要", "我要", "我需要", "麻烦你", "请帮我"
        )

        // 需要去除的语气词/后缀
        private val SUFFIX_WORDS = listOf(
            "一下", "好吗", "可以吗", "行吗", "吗", "呢", "吧", "啊", "哦", "嗯",
            "谢谢", "感谢", "麻烦了", "辛苦了"
        )

        // 常见连接词，在摘要时可省略
        private val FILLER_WORDS = listOf(
            "的", "了", "是", "有", "在", "和", "与", "或", "这个", "那个",
            "一个", "一下", "看看", "看看我的"
        )
    }

    /**
     * 生成对话标题
     *
     * @param content 用户输入内容
     * @return 摘要后的标题
     */
    operator fun invoke(content: String): String {
        if (content.isBlank()) return "新对话"

        // 步骤1：清理内容
        var cleaned = cleanContent(content.trim())

        // 步骤2：如果清理后过短，直接返回
        if (cleaned.length <= MAX_TITLE_LENGTH) return cleaned

        // 步骤3：智能截断，优先保留语义完整的短语
        return smartTruncate(cleaned)
    }

    /**
     * 清理内容：去除前缀、后缀、多余空格
     */
    private fun cleanContent(content: String): String {
        var result = content

        // 去除前缀
        for (prefix in PREFIX_WORDS) {
            if (result.startsWith(prefix)) {
                result = result.removePrefix(prefix)
                break
            }
        }

        // 去除后缀
        for (suffix in SUFFIX_WORDS) {
            if (result.endsWith(suffix)) {
                result = result.removeSuffix(suffix)
                break
            }
        }

        // 去除填充词（只去除开头的）
        var changed = true
        while (changed && result.isNotEmpty()) {
            changed = false
            for (filler in FILLER_WORDS) {
                if (result.startsWith(filler)) {
                    result = result.removePrefix(filler)
                    changed = true
                    break
                }
            }
        }

        return result.trim()
    }

    /**
     * 智能截断：在语义边界处截断，避免切断词语
     */
    private fun smartTruncate(content: String): String {
        // 优先在标点符号处截断
        val punctuationMarks = listOf('，', '。', '；', '！', '？', ',', '.', ';', '!', '?')

        // 查找最后一个在限制长度内的标点符号
        var lastPunctuationIndex = -1
        for (i in 0 until minOf(content.length, MAX_TITLE_LENGTH)) {
            if (content[i] in punctuationMarks) {
                lastPunctuationIndex = i
            }
        }

        // 如果在合理位置找到标点，在那里截断
        if (lastPunctuationIndex > 5) {
            return content.substring(0, lastPunctuationIndex)
        }

        // 否则在字数限制处截断，但避免切断词语
        // 从限制位置向前找，找到第一个可截断的位置（空格或标点）
        var truncateIndex = MAX_TITLE_LENGTH
        while (truncateIndex > 5) {
            val char = content[truncateIndex - 1]
            // 避免在英文单词中间切断
            if (char == ' ' || char in punctuationMarks || !char.isLetterOrDigit()) {
                break
            }
            truncateIndex--
        }

        val truncated = content.substring(0, truncateIndex).trim()
        return if (truncated.length < content.length) "$truncated..." else truncated
    }
}
