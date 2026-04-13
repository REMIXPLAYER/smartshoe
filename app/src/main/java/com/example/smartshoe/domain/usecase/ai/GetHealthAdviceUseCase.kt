package com.example.smartshoe.domain.usecase.ai

import com.example.smartshoe.domain.model.HealthAdviceResult
import com.example.smartshoe.domain.repository.AiAssistantRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 获取健康建议
 * 
 * 职责：封装获取健康建议的业务逻辑
 * 
 * 注意：使用领域层类型，符合 Clean Architecture
 */
@Singleton
class GetHealthAdviceUseCase @Inject constructor(
    private val aiAssistantRepository: AiAssistantRepository
) {

    /**
     * 根据传感器记录获取健康建议
     * 
     * @param recordId 传感器记录 ID
     * @param token 用户 Token
     * @param userAge 用户年龄（可选）
     * @return 健康建议结果
     */
    suspend operator fun invoke(
        recordId: String,
        token: String,
        userAge: Int? = null
    ): HealthAdviceResult {
        return aiAssistantRepository.getHealthAdvice(recordId, token, userAge)
    }
}
