package com.example.smartshoe.domain.usecase.ai

import com.example.smartshoe.domain.model.AiStatusResult
import com.example.smartshoe.domain.repository.AiAssistantRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 检查 AI 服务状态
 * 
 * 职责：封装检查 AI 服务可用性的业务逻辑
 * 
 * 注意：使用领域层类型，符合 Clean Architecture
 */
@Singleton
class CheckAiStatusUseCase @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository
) {

    /**
     * 检查 AI 服务是否可用
     * 
     * @return AI 服务状态结果
     */
    suspend operator fun invoke(): AiStatusResult {
        return aiAssistantRepository.checkAiStatus()
    }
}
