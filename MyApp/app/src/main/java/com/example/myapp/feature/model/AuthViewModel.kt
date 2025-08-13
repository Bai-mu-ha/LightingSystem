package com.example.myapp.feature.model

import android.util.Patterns
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.core.components.SendEmailVerification
import com.example.myapp.service.ApiService
import com.example.myapp.service.BaseResponse
import com.example.myapp.service.DeviceRepository
import com.example.myapp.service.LoginRequest
import com.example.myapp.service.RegisterRequest
import com.example.myapp.service.ResetPasswordRequest
import com.example.myapp.service.UserData
import com.example.myapp.service.UserRepository
import com.example.myapp.service.UserService
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * 认证ViewModel类，负责处理用户认证相关的业务逻辑
 *
 * 管理用户注册、登录、密码重置等认证流程
 *
 * @param authApiService 认证API服务
 * @param userRepository 用户数据仓库
 * @param userService 用户服务
 * @param deviceRepository 设备数据仓库
 * @param emailVerification 邮箱验证服务
 *
 * @author Your Name
 * @since 1.0.0
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authApiService: ApiService,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val deviceRepository: DeviceRepository,
    private val emailVerification: SendEmailVerification
) : ViewModel() {

    /**
     * 设置注册错误信息
     *
     * 更新注册状态为错误状态，并在3秒后自动重置
     *
     * @param message 错误消息
     * @param type 错误类型
     */
    //注册功能
    fun setRegistrationError(message: String, type: ErrorType) {
        _registrationState.value = RegistrationState.Error(message, type)
        viewModelScope.launch {
            delay(3000) // 3秒后自动重置
            resetRegistrationState()
        }
    }

    /**
     * 注册状态密封类
     *
     * 定义注册过程中的各种状态
     */
    sealed class RegistrationState {
        /** 空闲状态 */
        object Idle : RegistrationState()

        /** 加载中状态 */
        object Loading : RegistrationState()

        /** 成功状态，包含认证令牌和成功消息 */
        data class Success(
            val token: String,
            val message: String = "注册成功" // 添加默认成功消息
        ) : RegistrationState()

        /** 错误状态，包含错误信息和错误类型 */
        data class Error(
            val message: String,
            val type: ErrorType // 添加错误类型枚举
        ) : RegistrationState()
    }

    /**
     * 错误类型枚举
     *
     * 定义注册过程中可能出现的错误类型
     */
    enum class ErrorType {
        /** 邮箱错误 */
        EMAIL,

        /** 验证码错误 */
        CODE,

        /** 密码错误 */
        PASSWORD,

        /** 网络错误 */
        NETWORK
    }

    /**
     * 注册状态的私有可变状态流 */
    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)

    /**
     * 对外暴露的注册状态流 */
    val registrationState: StateFlow<RegistrationState> = _registrationState

    /**
     * 对外暴露的验证码验证状态流 */
    val verificationState: StateFlow<SendEmailVerification.VerificationState>
        get() = emailVerification.state

    /**
     * 发送验证码
     *
     * 根据不同的场景（注册或重置密码）发送验证码
     *
     * @param email 目标邮箱地址
     * @param type 验证码类型（注册或重置密码）
     * @param onResult 结果回调函数，参数为成功状态和错误信息
     */
    //发送验证码
    fun sendVerificationCode(
        email: String,
        type: CodeType,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            println("DEBUG: 开始发送验证码流程 - 邮箱: $email, 类型: $type")
            // 1. 邮箱格式验证（通用）
            if (!userService.verifyEmailFormat(email)) {
                onResult(false, "邮箱格式不正确")
                return@launch
            }

            // 2. 根据场景检查账号状态
            println("DEBUG: 开始检查账号存在状态...")
            val accountExists = try {
                userService.checkAccountExists(email).also {
                    println("DEBUG: [3/4] 检查结果: 账号${if(it) "已存在" else "不存在"}")
                }
            } catch (e: Exception) {
                println("DEBUG: [3/4] 检查异常: ${e.stackTraceToString()}")
                onResult(false, "检查账号状态失败")
                return@launch
            }

            // 根据验证码类型执行不同的账号状态检查
            when(type) {
                CodeType.REGISTER -> {
                    // 注册场景：账号不应已存在
                    if (accountExists) {
                        println("DEBUG: 注册场景 - 账号已存在，应阻止发送")
                        onResult(false, "账号已存在")
                        return@launch
                    }
                }
                CodeType.RESET_PASSWORD -> {
                    // 重置密码场景：账号应已存在
                    if (!accountExists) {
                        println("DEBUG: 重置密码场景 - 账号不存在，应阻止发送")
                        onResult(false, "账号不存在")
                        return@launch
                    }
                }
            }

            // 3. 发送验证码
            try {
                println("DEBUG: 准备发送验证码...")
                emailVerification.sendVerificationCode(email) // 调用邮箱验证服务发送验证码
                collectVerificationState(type, onResult) // 收集验证码发送状态
            } catch (e: Exception) {
                onResult(false, "发送验证码失败: ${e.message}")
            }
        }
    }

    /**
     * 收集验证码验证状态
     *
     * 监听验证码发送和验证过程的状态变化
     *
     * @param type 验证码类型
     * @param onResult 结果回调函数
     */
    private fun collectVerificationState(type: CodeType, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            // 收集邮箱验证服务的状态流
            emailVerification.state.collect { state ->
                when (state) {
                    is SendEmailVerification.VerificationState.Success -> {
                        // 验证码发送成功
                        onResult(true, null)
                    }
                    is SendEmailVerification.VerificationState.Error -> {
                        // 验证码发送失败
                        val error = state.message
                        when (type) {
                            CodeType.REGISTER -> setRegistrationError(error, ErrorType.CODE)
                            CodeType.RESET_PASSWORD -> _resetPasswordState.value = ResetPasswordState.Error(error)
                        }
                        onResult(false, error)
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * 用户注册
     *
     * 执行用户注册流程，包括参数验证、API调用和结果处理
     *
     * @param email 用户邮箱
     * @param code 验证码
     * @param password 用户密码
     */
    fun register(email: String, code: String, password: String) {
        _registrationState.value = RegistrationState.Loading // 设置注册状态为加载中
        viewModelScope.launch {
            try {
                // 调用API服务执行注册
                val response = authApiService.register(RegisterRequest(email, code, password))
                when {
                    response.isSuccessful -> {
                        response.body()?.let { body ->
                            when (body.code) {
                                200 -> {
                                    // 注册成功
                                    body.data?.let { data ->
                                        val token = data.token ?: run {
                                            _registrationState.value = RegistrationState.Error(
                                                "获取token失败",
                                                ErrorType.NETWORK
                                            )
                                            return@let
                                        }
                                        _registrationState.value = RegistrationState.Success(
                                            token = token,
                                            message = body.message // 确保有默认消息
                                        )

                                        // 保存用户认证数据
                                        userService.saveAuthData(
                                            token = token,
                                            userId = data.userId ?: "",
                                            email = email
                                        )
                                    }
                                }
                                400 -> {
                                    // 专门处理400错误（客户端错误）
                                    val errorType = when {
                                        body.message.contains("验证码") == true -> ErrorType.CODE
                                        body.message.contains("密码") == true -> ErrorType.PASSWORD
                                        else -> ErrorType.NETWORK
                                    }
                                    _registrationState.value = RegistrationState.Error(
                                        body.message,
                                        errorType
                                    )
                                }
                                else -> {
                                    // 其他错误状态
                                    _registrationState.value = RegistrationState.Error(
                                        body.message,
                                        ErrorType.NETWORK
                                    )
                                }
                            }
                        } ?: run {
                            _registrationState.value = RegistrationState.Error(
                                "无效响应",
                                ErrorType.NETWORK
                            )
                        }
                    }
                    else -> {
                        // 解析错误信息
                        val errorBody = response.errorBody()?.string()
                        val errorMsg = errorBody?.takeIf { it.contains("验证码") }
                            ?.let { "验证码错误" }
                            ?: response.message()

                        _registrationState.value = RegistrationState.Error(
                            errorMsg ?: "注册失败 (${response.code()})",
                            if (errorBody?.contains("验证码") == true) ErrorType.CODE else ErrorType.NETWORK
                        )
                    }
                }
            } catch (e: Exception) {
                // 网络异常处理
                _registrationState.value = RegistrationState.Error(
                    "网络错误: ${e.message?.takeIf { it.isNotBlank() } ?: "请检查网络连接"}",
                    ErrorType.NETWORK
                )
            }
        }
    }

    /**
     * 登录功能
     */

    /**
     * 登录状态密封类
     *
     * 定义登录过程中的各种状态
     */
    sealed class LoginState {
        /** 空闲状态 */
        object Idle : LoginState()

        /** 加载中状态 */
        object Loading : LoginState()

        /** 成功状态，包含认证令牌和用户ID */
        data class Success(val token: String, val userId: String) : LoginState()

        /** 错误状态，包含错误信息 */
        data class Error(val message: String) : LoginState()
    }

    /**
     * 登录状态的私有可变状态流 */
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)

    /**
     * 对外暴露的登录状态流 */
    val loginState: StateFlow<LoginState> = _loginState

    /**
     * 数据加载状态密封类
     *
     * 定义用户数据加载过程中的各种状态
     */
    sealed class DataLoadingState {
        /** 空闲状态 */
        object Idle : DataLoadingState()

        /** 加载中状态 */
        object Loading : DataLoadingState()

        /** 成功状态 */
        object Success : DataLoadingState()

        /** 错误状态，包含错误信息 */
        data class Error(val message: String) : DataLoadingState()
    }

    /**
     * 数据加载状态的私有可变状态流 */
    private val _dataLoadingState = MutableStateFlow<DataLoadingState>(DataLoadingState.Idle)

    /**
     * 对外暴露的数据加载状态流 */
    val dataLoadingState: StateFlow<DataLoadingState> = _dataLoadingState

    /**
     * 验证登录输入参数
     *
     * 检查邮箱和密码是否符合要求
     *
     * @param email 用户邮箱
     * @param password 用户密码
     * @return 错误信息，如果没有错误则返回null
     */
    private fun validateLoginInput(email: String, password: String): String? {
        return when {
            email.isBlank() || password.isBlank() -> "邮箱和密码不能为空"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "邮箱格式不正确"
            password.length < 6 -> "密码长度不能小于6位"
            else -> null
        }
    }

    /**
     * 用户登录
     *
     * 执行用户登录流程，包括参数验证、API调用和数据加载
     *
     * @param email 用户邮箱
     * @param password 用户密码
     */
    fun login(email: String, password: String) {
        // 先进行本地验证
        validateLoginInput(email, password)?.let { errorMsg ->
            _loginState.value = LoginState.Error(errorMsg)
            return
        }

        _loginState.value = LoginState.Loading // 设置登录状态为加载中
        viewModelScope.launch {
            try {
                // 设置15秒超时执行登录API调用
                val response = withTimeout(15_000) {
                    authApiService.login(LoginRequest(email, password))
                }

                if (response.isSuccessful) {
                    response.body()?.let { authResponse ->
                        when (authResponse.code) {
                            200 -> {
                                response.body()?.data?.let { loginData ->
                                    // 仅保存关键认证数据
                                    userService.saveAuthData(
                                        token = loginData.token ?: "",
                                        userId = loginData.userId ?: "",
                                        email = email
                                    )

                                    // 开始加载所有数据
                                    _dataLoadingState.value = DataLoadingState.Loading
                                    loadAllData(loginData) // 加载用户相关所有数据

                                    _loginState.value = LoginState.Success(
                                        token = loginData.token ?: "",
                                        userId = loginData.userId ?: ""
                                    )
                                } ?: run {
                                    _loginState.value = LoginState.Error("服务器返回空数据")
                                }
                            }
                            401 -> {
                                // 专门处理401错误（未授权）
                                val errorMsg = authResponse.message
                                _loginState.value = LoginState.Error(
                                    when {
                                        errorMsg.contains("账号") -> "账号未注册"
                                        errorMsg.contains("密码") -> "密码错误"
                                        else -> errorMsg
                                    }
                                )
                            }
                            else -> {
                                // 其他错误状态
                                _loginState.value = LoginState.Error(
                                    authResponse.message
                                )
                            }
                        }
                    } ?: run {
                        _loginState.value = LoginState.Error("服务器返回空数据")
                    }
                } else {
                    // 处理HTTP错误状态码
                    val errorMsg = try {
                        response.errorBody()?.string()?.let { errorBody ->
                            // 尝试解析错误JSON
                            val errorResponse = Gson().fromJson(errorBody, BaseResponse::class.java)
                            errorResponse.message ?: "登录失败 (${response.code()})"
                        } ?: "登录失败 (${response.code()})"
                    } catch (e: Exception) {
                        "登录失败 (${response.code()})"
                    }

                    _loginState.value = LoginState.Error(errorMsg)
                }
                resetLoginStateAfterDelay() // 延迟重置登录状态
            } catch (e: Exception) {
                // 异常处理
                val errorMsg = when (e) {
                    is TimeoutCancellationException -> "请求超时，请检查网络"
                    else -> "网络错误: ${e.message?.take(50)}"
                }
                _loginState.value = LoginState.Error(errorMsg)
                resetLoginStateAfterDelay() // 延迟重置登录状态
            }
        }
    }

    /**
     * 加载所有用户数据
     *
     * 并行加载用户信息、头像和设备数据
     *
     * @param loginData 登录返回的用户数据
     */
    private suspend fun loadAllData(loginData: UserData) {
        try {
            // 并行加载所有必要数据
            val userJob = viewModelScope.launch {
                userService.saveAuthData(
                    token = loginData.token ?: "",
                    userId = loginData.userId ?: "",
                    username = loginData.username,
                    email = loginData.email,
                    phone = loginData.phone,
                    avatarUrl = loginData.avatarUrl
                )
            }

            val avatarJob = viewModelScope.launch {
                loginData.avatarUrl?.let { url ->
                    userRepository.cacheAvatar(loginData.userId ?: "", url)
                }
            }

            val devicesJob = viewModelScope.launch {
                deviceRepository.loadDevices()
            }

            // 等待所有任务完成
            joinAll(userJob, avatarJob, devicesJob)

            _dataLoadingState.value = DataLoadingState.Success // 设置数据加载状态为成功
            _loginState.value = LoginState.Success(
                token = loginData.token ?: "",
                userId = loginData.userId ?: ""
            )
        } catch (e: Exception) {
            // 数据加载失败处理
            _dataLoadingState.value = DataLoadingState.Error(e.message ?: "数据加载失败")
        }
    }

    /**
     * 延迟重置登录状态
     *
     * 在3秒后将登录状态重置为空闲状态
     */
    private fun resetLoginStateAfterDelay() {
        viewModelScope.launch {
            delay(3000) // 3秒后自动重置
            _loginState.value = LoginState.Idle
        }
    }

    /**
     * 重置登录状态到空闲状态
     */
    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * 重置注册状态到空闲状态
     */
    fun resetRegistrationState() {
        _registrationState.value = RegistrationState.Idle
    }

    /**
     * 忘记密码功能
     */

    /**
     * 重置密码状态密封类
     *
     * 定义密码重置过程中的各种状态
     */
    sealed class ResetPasswordState {
        /** 空闲状态 */
        object Idle : ResetPasswordState()

        /** 加载中状态 */
        object Loading : ResetPasswordState()

        /** 成功状态，包含成功消息 */
        data class Success(val message: String) : ResetPasswordState()

        /** 错误状态，包含错误信息 */
        data class Error(val message: String) : ResetPasswordState()
    }

    /**
     * 设置重置密码错误信息
     *
     * 更新重置密码状态为错误状态，并在3秒后自动重置
     *
     * @param message 错误消息
     */
    fun setResetPasswordError(message: String) {
        _resetPasswordState.value = ResetPasswordState.Error(message)
        viewModelScope.launch {
            delay(3000) // 3秒后自动重置
            resetPasswordStateToIdle()
        }
    }

    /**
     * 重置密码状态的私有可变状态流 */
    private val _resetPasswordState = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)

    /**
     * 对外暴露的重置密码状态流 */
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState

    /**
     * 验证验证码
     *
     * 验证用户输入的验证码是否正确
     *
     * @param email 用户邮箱
     * @param code 验证码
     * @param callback 验证结果回调函数
     */
    fun verifyCode(email: String, code: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                println("DEBUG: 开始验证验证码 - 邮箱:$email 验证码:$code")
                // 调用用户服务验证重置密码验证码
                val isValid = userService.verifyResetPasswordCode(email, code).also {
                    println("DEBUG: 验证结果: $it")
                }

                if (!isValid) {
                    // 验证失败时更新状态
                    _resetPasswordState.value = ResetPasswordState.Error("验证码错误或已过期")
                }

                callback(isValid) // 调用回调函数返回验证结果
            } catch (e: Exception) {
                callback(false) // 发生异常时返回验证失败
            }
        }
    }

    /**
     * 重置密码
     *
     * 执行密码重置流程
     *
     * @param email 用户邮箱
     * @param code 验证码
     * @param newPassword 新密码
     */
    fun resetPassword(email: String, code: String, newPassword: String) {
        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState.Loading // 设置状态为加载中
            try {
                // 调用API服务执行密码重置
                val response = authApiService.resetPassword(
                    ResetPasswordRequest(email, code, newPassword)
                )

                when {
                    response.isSuccessful && response.body()?.code == 200 -> {
                        // 重置成功
                        _resetPasswordState.value = ResetPasswordState.Success("密码修改成功")
                        delay(100) // 确保UI能观察到状态变化
                    }
                    response.isSuccessful && response.body()?.code == 409 -> {
                        // 专门处理验证码无效的情况
                        _resetPasswordState.value = ResetPasswordState.Error(
                            response.body()?.message ?: "修改密码失败"
                        )
                        delay(100)
                    }
                    else -> {
                        // 其他错误情况
                        _resetPasswordState.value = ResetPasswordState.Error(
                            response.body()?.message ?: "修改密码失败"
                        )
                        delay(100)
                    }
                }
            } catch (e: Exception) {
                // 异常处理
                _resetPasswordState.value = ResetPasswordState.Error(
                    "网络错误: ${e.message}"
                )
                delay(100)
            }
        }
    }

    /**
     * 重置密码状态到空闲状态
     */
    fun resetPasswordStateToIdle() {
        _resetPasswordState.value = ResetPasswordState.Idle
    }

    /**
     * 验证码类型枚举
     *
     * 定义验证码的使用场景类型
     */
    enum class CodeType {
        /** 注册场景 */
        REGISTER,

        /** 重置密码场景 */
        RESET_PASSWORD
    }
}
