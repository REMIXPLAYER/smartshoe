package com.example.smartshoe.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.smartshoe.data.local.LocalDataSource
import com.example.smartshoe.data.local.SharedPreferencesDataSource
import com.example.smartshoe.data.remote.AuthApiService
import com.example.smartshoe.data.remote.AuthApiServiceImpl
import com.example.smartshoe.data.remote.SensorDataApiService
import com.example.smartshoe.data.remote.SensorDataApiServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用级依赖注入模块
 * 提供应用级别的单例依赖
 *
 * 注意：Repository 类（AuthRepositoryImpl, SensorDataManager 等）已经使用 @Singleton 和 @Inject 注解，
 * Hilt 会自动处理它们的创建，不需要在此模块中提供。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    companion object {
        /**
         * 提供应用上下文
         */
        @Provides
        @Singleton
        fun provideContext(@ApplicationContext context: Context): Context = context

        /**
         * 提供蓝牙适配器
         */
        @Provides
        @Singleton
        fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter? {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            return bluetoothManager?.adapter
        }

        /**
         * 提供本地数据源
         */
        @Provides
        @Singleton
        fun provideLocalDataSource(@ApplicationContext context: Context): LocalDataSource {
            return SharedPreferencesDataSource(context)
        }

        // UserPreferencesManager 使用 @Inject 构造函数，Hilt 会自动处理
        // 不需要手动提供
    }

    /**
     * 绑定 SensorDataApiService 接口到其实现类
     */
    @Binds
    @Singleton
    abstract fun bindSensorDataApiService(
        impl: SensorDataApiServiceImpl
    ): SensorDataApiService

    /**
     * 绑定 AuthApiService 接口到其实现类
     */
    @Binds
    @Singleton
    abstract fun bindAuthApiService(
        impl: AuthApiServiceImpl
    ): AuthApiService
}
