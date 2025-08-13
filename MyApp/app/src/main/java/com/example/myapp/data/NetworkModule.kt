package com.example.myapp.data

import com.example.myapp.service.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * 网络模块，提供网络相关的依赖注入配置
 *
 * 该模块负责配置和提供网络客户端、Retrofit实例以及API服务接口
 *
 * @author Your Name
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    //private const val BASE_URL = "http://10.0.2.2:3000/" // 模拟器用此地址
    private const val BASE_URL = "https://light.uphengbai77.xyz"

    /**
     * 提供配置好的OkHttpClient实例
     *
     * 该客户端包含HTTP日志拦截器，用于调试网络请求
     *
     * @return 配置好的OkHttpClient实例
     */
    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // 打印完整请求和响应
            })
            .build()
    }

    /**
     * 提供配置好的Retrofit实例
     *
     * 使用指定的基础URL和Gson转换器工厂构建Retrofit实例
     *
     * @param okHttpClient 网络客户端实例
     * @return 配置好的Retrofit实例
     */
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 提供API服务接口实例
     *
     * 通过Retrofit创建API服务接口实例
     *
     * @param retrofit Retrofit实例
     * @return API服务接口实例
     */
    @Singleton
    @Provides
    fun provideAuthApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
