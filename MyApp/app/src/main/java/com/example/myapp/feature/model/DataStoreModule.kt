package com.example.myapp.feature.model

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DataStore模块类，负责提供DataStore依赖注入配置
 *
 * 配置和提供Preferences DataStore实例用于数据持久化
 *
 * @author Your Name
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    /**
     * 提供Preferences DataStore实例
     *
     * 创建并配置Preferences DataStore用于存储用户偏好设置
     *
     * @param context 应用上下文
     * @return 配置好的DataStore实例
     */
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = {
                context.preferencesDataStoreFile("user_preferences") // 指定数据存储文件名
            }
        )
    }
}
