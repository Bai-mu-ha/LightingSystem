package com.example.myapp.service

import android.util.Patterns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.myapp.core.components.SendEmailVerification
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户服务类，负责处理用户相关的业务逻辑和数据存储操作
 *
 * 管理用户认证、邮箱验证、数据持久化等核心功能
 *
 * @param apiService API服务接口，用于与后端通信
 * @param dataStore 数据存储服务，用于本地数据持久化
 * @param emailVerification 邮箱验证服务，用于发送和验证验证码
 *
 * @author Your Name
 * @since 1.0.0
 */
@Singleton
class UserService @Inject constructor(
    private val apiService: ApiService,
    private val dataStore: DataStore<Preferences>,
    private val emailVerification: SendEmailVerification
) {
    /**
     * 偏好设置键对象，定义所有用户相关数据的存储键
     */
    object PreferencesKeys {
        /** 认证令牌键 */
        val AUTH_TOKEN = stringPreferencesKey("auth_token")

        /** 用户ID键 */
        val USER_ID = stringPreferencesKey("user_id")

        /** 用户名键 */
        val USERNAME = stringPreferencesKey("username")

        /** 邮箱键 */
        val EMAIL = stringPreferencesKey("email")

        /** 手机号键 */
        val PHONE = stringPreferencesKey("phone")

        /** 头像URL键 */
        val AVATAR_URL = stringPreferencesKey("avatar_url")
    }

    /**
     * 获取当前用户的ID
     *
     * @return 当前用户ID，如果未登录则返回null
     */
    suspend fun getCurrentUserId(): String? = dataStore.data.first()[PreferencesKeys.USER_ID]

    /**
     * 获取当前用户的用户名
     *
     * @return 当前用户名，如果未登录则返回null
     */
    suspend fun getCurrentUsername(): String? = dataStore.data.first()[PreferencesKeys.USERNAME]

    /**
     * 获取当前用户的邮箱
     *
     * @return 当前用户邮箱，如果未设置则返回null
     */
    suspend fun getCurrentEmail(): String? = dataStore.data.first()[PreferencesKeys.EMAIL]

    /**
     * 获取当前用户的手机号
     *
     * @return 当前用户手机号，如果未设置则返回null
     */
    suspend fun getCurrentPhone(): String? = dataStore.data.first()[PreferencesKeys.PHONE]

    /**
     * 获取当前用户的头像URL
     *
     * @return 当前用户头像URL，如果未设置则返回null
     */
    suspend fun getCurrentAvatarUrl(): String? = dataStore.data.first()[PreferencesKeys.AVATAR_URL]

    /**
     * 获取认证令牌
     *
     * @return 认证令牌，如果未登录则返回null
     */
    suspend fun getToken(): String? {
        return dataStore.data.first()[PreferencesKeys.AUTH_TOKEN]
    }

    /**
     * 检查账号是否存在
     *
     * 通过API检查指定邮箱的账号是否已注册
     *
     * @param email 要检查的邮箱地址
     * @return 账号是否存在
     */
    suspend fun checkAccountExists(email: String): Boolean {
        return try {
            // 调用API检查账号是否存在
            val response = apiService.checkAccountExists(EmailRequest(email))
            response.isSuccessful && response.body()?.data == true
        } catch (e: Exception) {
            println("DEBUG: 检查账号存在失败: ${e.message}") // 添加调试日志
            false
        }
    }

    /**
     * 验证邮箱格式
     *
     * 检查邮箱地址是否符合标准格式
     *
     * @param email 要验证的邮箱地址
     * @return 邮箱格式是否正确
     */
    fun verifyEmailFormat(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * 验证邮箱验证码
     *
     * 验证用户输入的邮箱验证码是否正确
     *
     * @param email 用户邮箱地址
     * @param code 用户输入的验证码
     * @return 验证码是否正确
     * @throws Exception 未登录时抛出异常
     */
    suspend fun verifyEmailCode(email: String, code: String): Boolean {
        val token = getToken() ?: throw Exception("未登录")
        // 调用API验证验证码
        val response = apiService.verifyCode(
            token = "Bearer $token",
            request = CodeVerificationRequest(
                email = email,
                code = code
            )
        )
        return response.isSuccessful && response.body()?.data == true
    }

    /**
     * 发送并验证验证码
     *
     * 执行完整的验证码发送和验证流程
     *
     * @param email 目标邮箱地址
     * @param checkExists 是否需要检查账号存在状态
     * @param expectedExists 期望的账号存在状态
     * @param onError 错误回调函数
     * @return 操作是否成功
     */
    suspend fun sendAndVerifyCode(
        email: String,
        checkExists: Boolean,
        expectedExists: Boolean,
        onError: (String) -> Unit
    ): Boolean {
        return try {
            // 1. 检查账号存在状态（如果需要）
            if (checkExists) {
                val exists = apiService.checkAccountExists(EmailRequest(email)).body()?.data == true
                if (exists != expectedExists) {
                    val errorMsg = if (expectedExists) "账号不存在" else "账号已存在"
                    onError(errorMsg)
                    return false
                }
            }

            // 2. 发送验证码
            val response = apiService.sendVerificationCode(EmailRequest(email))
            if (!response.isSuccessful) {
                onError("发送验证码失败: ${response.code()}")
                return false
            }

            true
        } catch (e: Exception) {
            onError("网络错误: ${e.message}")
            false
        }
    }

    /**
     * 验证重置密码验证码
     *
     * 专门用于密码重置流程的验证码验证
     *
     * @param email 用户邮箱地址
     * @param code 用户输入的验证码
     * @return 验证码是否正确
     */
    suspend fun verifyResetPasswordCode(email: String, code: String): Boolean {
        return try {
            println("DEBUG: 验证重置密码验证码 - 邮箱:$email")
            // 调用API验证重置密码验证码
            val response = apiService.verifyCode(
                token = null,
                request = CodeVerificationRequest(
                    email = email,
                    code = code
                )
            )

            if (!response.isSuccessful) {
                println("DEBUG: 验证失败 - 状态码:${response.code()}, 错误:${response.errorBody()?.string()}")
                return false
            }

            response.body()?.data == true
        } catch (e: Exception) {
            println("ERROR: 验证异常: ${e.stackTraceToString()}")
            false
        }
    }

    /**
     * 数据存储操作
     *
     * 保存用户认证数据到本地存储
     *
     * @param token 认证令牌
     * @param userId 用户ID
     * @param username 用户名（可选）
     * @param email 邮箱（可选）
     * @param phone 手机号（可选）
     * @param avatarUrl 头像URL（可选）
     */
    suspend fun saveAuthData(
        token: String,
        userId: String,
        username: String? = null,
        email: String? = null,
        phone: String? = null,
        avatarUrl: String? = null
    ) {
        // 编辑数据存储中的用户数据
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
            preferences[PreferencesKeys.USER_ID] = userId
            username?.let { preferences[PreferencesKeys.USERNAME] = it }
            email?.let { preferences[PreferencesKeys.EMAIL] = it }
            phone?.let { preferences[PreferencesKeys.PHONE] = it }
            avatarUrl?.let { preferences[PreferencesKeys.AVATAR_URL] = it }
        }
    }

    /**
     * 清除用户数据
     *
     * 注销登录时清除所有本地存储的用户数据
     */
    suspend fun clearUserData() {
        dataStore.edit { it.clear() }
    }
}
