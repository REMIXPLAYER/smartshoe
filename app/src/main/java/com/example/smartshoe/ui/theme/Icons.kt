package com.example.smartshoe.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import com.example.smartshoe.R

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
    // Material 图标
    val Home = AppIcon.MaterialIcon(Icons.Filled.Home)
    val Build = AppIcon.MaterialIcon(Icons.Filled.Build)
    val Settings = AppIcon.MaterialIcon(Icons.Filled.Settings)
    val Notifications = AppIcon.MaterialIcon(Icons.Filled.Notifications)
    val Person = AppIcon.MaterialIcon(Icons.Filled.Person)
    val Info = AppIcon.MaterialIcon(Icons.Filled.Info)
    val Delete = AppIcon.MaterialIcon(Icons.Filled.Delete)
    val ArrowRight = AppIcon.MaterialIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight)
    val Email = AppIcon.MaterialIcon(Icons.Filled.Email)
    val Lock = AppIcon.MaterialIcon(Icons.Filled.Lock)
    val Check = AppIcon.MaterialIcon(Icons.Filled.CheckCircle)
    val Refresh = AppIcon.MaterialIcon(Icons.Filled.Refresh)
    val Menu = AppIcon.MaterialIcon(Icons.Filled.Menu)
    val Close = AppIcon.MaterialIcon(Icons.Filled.Close)
    val Search = AppIcon.MaterialIcon(Icons.Filled.Search)
    val Add = AppIcon.MaterialIcon(Icons.Filled.Add)
    val Edit = AppIcon.MaterialIcon(Icons.Filled.Edit)
    val Logout = AppIcon.MaterialIcon(Icons.Filled.ExitToApp)

    // 资源图标
    val Cloud = AppIcon.ResourceIcon(R.drawable.cloud)
    val Bluetooth = AppIcon.ResourceIcon(R.drawable.bluetooth)
    val Help = AppIcon.ResourceIcon(R.drawable.help)
    val Security = AppIcon.ResourceIcon(R.drawable.security)
    val Man = AppIcon.ResourceIcon(R.drawable.man)
    val Warning = AppIcon.ResourceIcon(R.drawable.warning)
    val LeftArrow = AppIcon.ResourceIcon(R.drawable.leftarrow)
    val Visibility = AppIcon.ResourceIcon(R.drawable.visibility)
    val VisibilityOff = AppIcon.ResourceIcon(R.drawable.visibility_off)
}
