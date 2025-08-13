package com.example.myapp.service

import com.example.myapp.data.Device
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * API服务接口，定义所有与后端通信的API端点
 *
 * 使用Retrofit注解定义HTTP请求方法、路径和参数
 *
 * @author Your Name
 * @since 1.0.0
 */
interface ApiService {
    /**
     * 账户检查相关API
     */

    // 检查账号是否存在
    @POST("api/auth/check-account")
    suspend fun checkAccountExists(@Body request: EmailRequest): Response<BaseResponse<Boolean>>

    /**
     * 验证码相关API
     */

    // 发送验证码
    @POST("api/auth/send-code")
    suspend fun sendVerificationCode(@Body request: EmailRequest): Response<BaseResponse<Unit>>

    // 验证验证码
    @POST("api/auth/verify-code")
    suspend fun verifyCode(
        @Header("Authorization") token: String? = null,
        @Body request: CodeVerificationRequest
    ): Response<BaseResponse<Boolean>>

    /**
     * 认证相关API
     */

    // 用户注册
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    // 用户登录
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    // 重置密码
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<AuthResponse>

    /**
     * 用户资料相关API
     */

    // 更新用户名
    @PATCH("api/user/me/username")
    suspend fun updateUsername(
        @Header("Authorization") token: String,
        @Body request: SingleFieldRequest
    ): Response<AuthResponse>

    // 更新头像
    @Multipart
    @PATCH("api/user/me/avatar")
    suspend fun updateAvatar(
        @Header("Authorization") token: String,
        @Part avatar: MultipartBody.Part
    ): Response<AuthResponse>

    // 更新邮箱
    @PATCH("api/user/email")
    suspend fun updateEmail(
        @Header("Authorization") token: String,
        @Body request: SingleFieldRequest
    ): Response<AuthResponse>

    /**
     * 密码相关API
     */

    // 验证密码
    @POST("api/user/verify-password")
    suspend fun verifyPassword(
        @Header("Authorization") token: String,
        @Body request: SingleFieldRequest
    ): Response<BaseResponse<Boolean>>

    // 更新密码
    @PATCH("api/user/password")
    suspend fun updatePassword(
        @Header("Authorization") token: String,
        @Body request: SingleFieldRequest
    ): Response<BaseResponse<Unit>>

    /**
     * 文件下载相关API
     */

    // 下载文件
    @GET @Streaming
    suspend fun downloadFile(@Url url: String): Response<ResponseBody>

    /**
     * 设备相关API
     */

    // 绑定设备
    @POST("api/device/bind")
    suspend fun bindDevice(
        @Header("Authorization") token: String,
        @Body request: BindDeviceRequest
    ): Response<BaseResponse<Device>>

    // 检查设备注册状态
    @POST("api/device/check-registered")
    suspend fun checkDeviceRegistered(
        @Body request: CheckDeviceRequest
    ): Response<BaseResponse<CheckRegisteredResponse>>

    // 获取绑定设备列表
    @GET("api/device/list")
    suspend fun getBoundDevices(
        @Header("Authorization") token: String,
        @Query("userId") userId: String
    ): Response<BaseResponse<List<DeviceResponse>>>

    // 更新设备模式
    @PATCH("api/device/{deviceId}/mode")
    suspend fun updateDeviceMode(
        @Path("deviceId") deviceId: String,
        @Body request: ModeRequest
    ): Response<BaseResponse<Unit>>

}

/**
 * 统一响应结构数据类
 *
 * 定义API响应的基本结构
 *
 * @param T 响应数据类型
 * @property code 响应状态码
 * @property message 响应消息
 * @property data 响应数据
 *
 * @author Your Name
 * @since 1.0.0
 */
data class BaseResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

/**
 * 统一认证响应数据类
 *
 * 定义认证相关API的响应结构
 *
 * @property code 响应状态码
 * @property message 响应消息
 * @property data 用户数据
 *
 * @author Your Name
 * @since 1.0.0
 */
data class AuthResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: UserData?
)

/**
 * 用户数据类
 *
 * 定义用户信息的数据结构
 *
 * @property token 认证令牌
 * @property userId 用户ID
 * @property username 用户名
 * @property phone 手机号
 * @property email 邮箱
 * @property avatarUrl 头像URL
 *
 * @author Your Name
 * @since 1.0.0
 */
data class UserData(
    @SerializedName("token") val token: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("avatarUrl") val avatarUrl: String?
)

/**
 * 单字段请求数据类
 *
 * 用于只需要单个字段值的API请求
 *
 * @property value 字段值
 *
 * @author Your Name
 * @since 1.0.0
 */
data class SingleFieldRequest(
    @SerializedName("value") val value: String
)

/**
 * 验证码验证请求数据类
 *
 * 用于验证码验证的API请求
 *
 * @property email 邮箱地址
 * @property code 验证码
 *
 * @author Your Name
 * @since 1.0.0
 */
data class CodeVerificationRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String
)

/**
 * 邮箱请求数据类
 *
 * 用于邮箱相关操作的API请求
 *
 * @param email 邮箱地址
 *
 * @author Your Name
 * @since 1.0.0
 */
data class EmailRequest(val email: String)

/**
 * 注册请求数据类
 *
 * 用于用户注册的API请求
 *
 * @property email 邮箱地址
 * @property code 验证码
 * @property password 密码
 *
 * @author Your Name
 * @since 1.0.0
 */
data class RegisterRequest(val email: String, val code: String, val password: String)

/**
 * 登录请求数据类
 *
 * 用于用户登录的API请求
 *
 * @property email 邮箱地址
 * @property password 密码
 *
 * @author Your Name
 * @since 1.0.0
 */
data class LoginRequest(val email: String, val password: String)

/**
 * 重置密码请求数据类
 *
 * 用于密码重置的API请求
 *
 * @property email 邮箱地址
 * @property code 验证码
 * @property newPassword 新密码
 *
 * @author Your Name
 * @since 1.0.0
 */
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

/**
 * 绑定设备请求数据类
 *
 * 用于设备绑定的API请求
 *
 * @property userId 用户ID
 * @property deviceId 设备ID
 *
 * @author Your Name
 * @since 1.0.0
 */
data class BindDeviceRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("deviceId") val deviceId: String
)

/**
 * 模式请求数据类
 *
 * 用于设备模式更新的API请求
 *
 * @property mode 设备模式
 * @property params 模式参数
 *
 * @author Your Name
 * @since 1.0.0
 */
data class ModeRequest(
    @SerializedName("mode")
    val mode: String,
    @SerializedName("params")
    val params: Map<String, Any> = emptyMap()
)

/**
 * 检查设备请求数据类
 *
 * 用于检查设备注册状态的API请求
 *
 * @property deviceId 设备ID
 *
 * @author Your Name
 * @since 1.0.0
 */
data class CheckDeviceRequest(
    @SerializedName("deviceId") val deviceId: String
)

/**
 * 检查注册响应数据类
 *
 * 定义设备注册状态检查的响应结构
 *
 * @property isRegistered 是否已注册
 * @property isBound 是否已绑定
 *
 * @author Your Name
 * @since 1.0.0
 */
data class CheckRegisteredResponse(
    @SerializedName("isRegistered")
    val isRegistered: Boolean,
    @SerializedName("isBound")
    val isBound: Boolean
)

/**
 * 设备响应数据类
 *
 * 定义设备信息的响应结构
 *
 * @property deviceId 设备ID
 * @property color 颜色
 * @property mode 模式
 * @property name 设备名称
 * @property isOnline 是否在线
 *
 * @author Your Name
 * @since 1.0.0
 */
data class DeviceResponse(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("color") val color: String?,
    @SerializedName("mode") val mode: String?,
    val name: String?,
    val isOnline: Boolean
)
