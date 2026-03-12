package com.example.smartshoe

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*

/**
 * 统一的图标表示接口
 */
sealed class AppIcon {
    data class MaterialIcon(val icon: ImageVector) : AppIcon()
    data class ResourceIcon(@DrawableRes val resId: Int) : AppIcon()
}

/**
 * 图标资源管理
 */
object AppIcons {
    val Home = AppIcon.MaterialIcon(Icons.Filled.Home)
    val Build = AppIcon.MaterialIcon(Icons.Filled.Build)
    val Settings = AppIcon.MaterialIcon(Icons.Filled.Settings)
    val Notifications = AppIcon.MaterialIcon(Icons.Filled.Notifications)
    val Person = AppIcon.MaterialIcon(Icons.Filled.Person)
    val Info = AppIcon.MaterialIcon(Icons.Filled.Info)
    val Delete = AppIcon.MaterialIcon(Icons.Filled.Delete)
    val ArrowRight = AppIcon.MaterialIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight)

    // 资源图标需要根据实际资源ID调整
    val Cloud = AppIcon.ResourceIcon(R.drawable.cloud)
    val Bluetooth = AppIcon.ResourceIcon(R.drawable.bluetooth)
    val Help = AppIcon.ResourceIcon(R.drawable.help)
    val Security = AppIcon.ResourceIcon(R.drawable.security)
}