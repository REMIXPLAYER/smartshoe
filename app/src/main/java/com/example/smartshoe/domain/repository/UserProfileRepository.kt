package com.example.smartshoe.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * 用户资料仓库接口
 *
 * 职责：定义用户个人资料相关的数据操作
 * 这是领域层接口，不依赖任何数据层实现细节
 */
interface UserProfileRepository {

    /**
     * 用户体重状态流
     */
    val userWeightFlow: StateFlow<Float>

    /**
     * 获取用户体重
     * @return 体重值，默认0f
     */
    fun getUserWeight(): Float

    /**
     * 保存用户体重
     * @param weight 体重值
     * @return 是否保存成功
     */
    fun saveUserWeight(weight: Float): Boolean

    /**
     * 获取用户名
     * @return 用户名，可能为null
     */
    fun getUsername(): String?

    /**
     * 保存用户名
     * @param username 用户名
     * @return 是否保存成功
     */
    fun saveUsername(username: String): Boolean

    /**
     * 检查是否是首次启动
     * @return 是否为首次启动
     */
    fun isFirstLaunch(): Boolean

    /**
     * 设置首次启动标记
     * @param isFirst 是否为首次启动
     */
    fun setFirstLaunch(isFirst: Boolean)

    /**
     * 清除所有用户资料数据
     */
    fun clearUserProfile()
}
