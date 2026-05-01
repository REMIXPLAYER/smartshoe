package com.example.smartshoe.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.room.Room
import com.example.smartshoe.data.local.AppDatabase
import com.example.smartshoe.data.local.LocalDataSource
import com.example.smartshoe.data.local.SharedPreferencesDataSource
import com.example.smartshoe.data.remote.AiAssistantApiService
import com.example.smartshoe.data.remote.AiAssistantApiServiceImpl
import com.example.smartshoe.data.remote.AuthApiService
import com.example.smartshoe.data.remote.AuthApiServiceImpl
import com.example.smartshoe.data.remote.SensorDataApiService
import com.example.smartshoe.data.remote.SensorDataApiServiceImpl
import com.example.smartshoe.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 应用级协程作用域限定符
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

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
         * 提供应用级协程作用域
         * 用于需要在应用生命周期内运行的后台任务
         */
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }

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

        /**
         * 提供OkHttpClient
         * 用于SSE和其他网络请求
         */
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)  // SSE需要长超时
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        /**
         * 提供Room数据库
         */
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME
            ).build()
        }

        /**
         * 提供AI对话DAO
         */
        @Provides
        fun provideAiConversationDao(database: AppDatabase) = database.aiConversationDao()

        /**
         * 提供AI消息DAO
         */
        @Provides
        fun provideAiMessageDao(database: AppDatabase) = database.aiMessageDao()

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

    /**
     * 绑定 AiAssistantApiService 接口到其实现类
     */
    @Binds
    @Singleton
    abstract fun bindAiAssistantApiService(
        impl: AiAssistantApiServiceImpl
    ): AiAssistantApiService
}
