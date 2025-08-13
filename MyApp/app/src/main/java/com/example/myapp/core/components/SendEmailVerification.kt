package com.example.myapp.core.components

import com.example.myapp.service.ApiService
import com.example.myapp.service.EmailRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 邮箱验证发送器，负责向用户邮箱发送验证码
 *
 * 该类使用单例模式，通过Hilt依赖注入ApiService来发送验证请求，
 * 并维护发送状态。
 *
 * @author Your Name
 * @since 1.0.0
 */
@Singleton
class SendEmailVerification @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * 验证状态密封类，表示发送验证码的不同状态
     */
    sealed class VerificationState {
        /** 空闲状态 */
        object Idle : VerificationState()

        /** 发送中状态 */
        object Sending : VerificationState()

        /** 发送成功状态，包含邮箱地址 */
        data class Success(val email: String) : VerificationState()

        /** 发送失败状态，包含错误信息 */
        data class Error(val message: String) : VerificationState()
    }

    /** 状态流的私有可变状态 */
    private val _state = MutableStateFlow<VerificationState>(VerificationState.Idle)

    /** 对外暴露的只读状态流 */
    val state: StateFlow<VerificationState> = _state

    /**
     * 发送验证码到指定邮箱
     *
     * @param email 目标邮箱地址
     */
    suspend fun sendVerificationCode(email: String) {
        _state.value = VerificationState.Sending
        try {
            val response = apiService.sendVerificationCode(EmailRequest(email))
            if (response.isSuccessful) {
                _state.value = VerificationState.Success(email)
            } else {
                _state.value = VerificationState.Error("发送失败: ${response.message()}")
            }
        } catch (e: Exception) {
            _state.value = VerificationState.Error("网络错误: ${e.message}")
        }
    }

    /**
     * 重置状态为初始状态
     */
    fun resetState() {
        _state.value = VerificationState.Idle
    }
}
