package com.example.smartshoe.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * 主屏幕视图模型
 * 管理主屏幕的UI状态，包括底部导航栏选中状态等
 *
 * 职责：
 * - 管理底部导航栏选中状态
 * - 管理页面切换状态
 * - 提供UI状态持久化（配置更改时保持状态）
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    // 底部导航栏选中状态
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    /**
     * 切换底部导航栏选中项
     * @param tabIndex 选中的标签索引
     */
    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    /**
     * 获取当前选中的标签索引
     */
    fun getCurrentTab(): Int = _selectedTab.value
}
