package com.example.smartshoe.di

import com.example.smartshoe.data.repository.AuthRepositoryImpl
import com.example.smartshoe.data.repository.AiAssistantRepositoryImpl
import com.example.smartshoe.data.repository.AiConversationRepositoryImpl
import com.example.smartshoe.data.repository.SensorDataRemoteRepositoryImpl
import com.example.smartshoe.data.repository.SensorDataRepositoryImpl
import com.example.smartshoe.data.repository.HistoryRecordRepositoryImpl
import com.example.smartshoe.data.repository.UserProfileRepositoryImpl
import com.example.smartshoe.domain.repository.AuthRepository
import com.example.smartshoe.domain.repository.AiAssistantRepository
import com.example.smartshoe.domain.repository.AiConversationRepository
import com.example.smartshoe.domain.repository.SensorDataRemoteRepository
import com.example.smartshoe.domain.repository.SensorDataRepository
import com.example.smartshoe.domain.repository.HistoryRecordRepository
import com.example.smartshoe.domain.repository.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 依赖注入模块
 * 使用构造函数注入，无需手动实例化
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 绑定认证仓库接口到实现类
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    /**
     * 绑定 AI 助手仓库接口到实现类
     */
    @Binds
    @Singleton
    abstract fun bindAiAssistantRepository(
        impl: AiAssistantRepositoryImpl
    ): AiAssistantRepository

    /**
     * 绑定传感器数据远程仓库接口到实现类
     */
    @Binds
    @Singleton
    abstract fun bindSensorDataRemoteRepository(
        impl: SensorDataRemoteRepositoryImpl
    ): SensorDataRemoteRepository

    /**
     * 绑定传感器数据仓库接口到实现类
     */
    @Binds
    @Singleton
    abstract fun bindSensorDataRepository(
        impl: SensorDataRepositoryImpl
    ): SensorDataRepository

    /**
     * 绑定历史记录仓库接口到实现类
     */
    @Binds
    @Singleton
    abstract fun bindHistoryRecordRepository(
        impl: HistoryRecordRepositoryImpl
    ): HistoryRecordRepository

    /**
     * 绑定用户资料仓库接口到实现类
     */
    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(
        impl: UserProfileRepositoryImpl
    ): UserProfileRepository

    /**
     * 绑定AI对话仓库接口到实现类
     */
    @Binds
    @Singleton
    abstract fun bindAiConversationRepository(
        impl: AiConversationRepositoryImpl
    ): AiConversationRepository
}
