package com.example.myapp.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 应用模块，提供应用级别的依赖注入配置
 *
 * 该模块主要提供应用上下文等基础依赖
 *
 * @author Your Name
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /**
     * 提供应用上下文实例
     *
     * @param context 应用上下文
     * @return 应用上下文实例
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
