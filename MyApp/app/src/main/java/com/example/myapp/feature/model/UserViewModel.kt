package com.example.myapp.feature.model

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.myapp.core.navigation.Screen
import com.example.myapp.service.UserRepository
import com.example.myapp.data.User
import com.example.myapp.service.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 用户ViewModel类，负责处理用户个人信息相关的业务逻辑
 *
 * 管理用户资料、头像、邮箱、密码等个人信息的更新和维护
 *
 * @param userRepository 用户数据仓库
 * @param userService 用户服务
 *
 * @author Your Name
 * @since 1.0.0
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userService: UserService,
) : ViewModel() {
    /**
     * 用户信息的私有可变状态流 */
    private val _user = MutableStateFlow<User?>(null)

    /**
     * 对外暴露的用户信息状态流 */
    val user: StateFlow<User?> = _user

    /**
     * 头像加载状态的私有可变状态流 */
    private val _avatarLoading = MutableStateFlow(false)

    /**
     * 对外暴露的头像加载状态流 */
    val avatarLoading: StateFlow<Boolean> = _avatarLoading

    /**
     * 初始化登录后用户数据
     *
     * 登录成功后调用此方法加载用户基本信息和缓存头像
     */
    // 新增方法：供登录成功后调用
    fun initializeAfterLogin() {
        viewModelScope.launch {
            try {
                val userId = userService.getCurrentUserId() ?: return@launch

                // 1. 先加载基本用户数据
                loadCachedUserData(userId)

                // 2. 检查是否需要缓存头像
                user.value?.avatarUrl?.let { url ->
                    if (url.isNotBlank()) {
                        val cacheFile = userRepository.getAvatarCacheFile(userId)
                        if (!cacheFile.exists() || cacheFile.length() == 0L) {
                            _avatarLoading.value = true
                            try {
                                userRepository.cacheAvatar(userId, url) // 缓存用户头像
                                loadCachedUserData(userId) // 缓存后重新加载用户数据
                            } finally {
                                _avatarLoading.value = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("UserVM", "初始化失败", e)
            }
        }
    }

    /**
     * 加载缓存的用户数据
     *
     * 从本地缓存中加载用户基本信息和头像文件
     *
     * @param userId 用户ID
     */
    private suspend fun loadCachedUserData(userId: String) {
        _user.value = User(
            id = userId,
            username = userService.getCurrentUsername() ?: "默认用户",
            email = userService.getCurrentEmail() ?: "",
            phone = userService.getCurrentPhone(),
            avatarUrl = userService.getCurrentAvatarUrl()
        )

        // 加载本地头像文件
        val avatarFile = userRepository.getAvatarCacheFile(userId).takeIf { it.exists() }
        _avatarFile.value = avatarFile
    }

    /**
     * 更新数据功能
     */

    /**
     * 头像更新状态的私有可变状态流 */
    private val _avatarUpdateState = MutableStateFlow<UpdateState>(UpdateState.Idle)

    /**
     * 对外暴露的头像更新状态流 */
    val avatarUpdateState: StateFlow<UpdateState> = _avatarUpdateState

    /**
     * 用户名更新状态的私有可变状态流 */
    private val _usernameUpdateState = MutableStateFlow<UpdateState>(UpdateState.Idle)

    /**
     * 对外暴露的用户名更新状态流 */
    val usernameUpdateState: StateFlow<UpdateState> = _usernameUpdateState

    /**
     * 更新状态密封类
     *
     * 定义用户信息更新过程中的各种状态
     */
    sealed class UpdateState {
        /** 空闲状态 */
        object Idle : UpdateState()

        /** 更新中状态 */
        object Updating : UpdateState()

        /** 成功状态，包含更新类型 */
        data class Success(val type: String) : UpdateState()

        /** 错误状态，包含错误信息 */
        data class Error(val message: String) : UpdateState()
    }

    /**
     * 更新用户名
     *
     * 执行用户名更新流程
     *
     * @param newName 新用户名
     */
    fun updateUsername(newName: String) {
        viewModelScope.launch {
            _usernameUpdateState.value = UpdateState.Updating // 设置状态为更新中
            try {
                val updatedUser = userRepository.updateUsername(newName) // 调用仓库更新用户名
                _user.value = updatedUser // 更新用户信息状态
                _usernameUpdateState.value = UpdateState.Success("昵称") // 设置状态为成功
            } catch (e: Exception) {
                _usernameUpdateState.value = UpdateState.Error(e.message ?: "未知错误") // 设置状态为错误
                println("DEBUG: 用户名更新失败: ${e.message}")
            }
        }
    }

    /**
     * 更新头像功能
     */

    /**
     * 头像文件的私有可变状态流 */
    private val _avatarFile = MutableStateFlow<File?>(null)

    /**
     * 对外暴露的头像文件状态流 */
    val avatarFile: StateFlow<File?> = _avatarFile

    /**
     * 更新用户头像
     *
     * 执行头像更新流程，包括文件处理和服务器上传
     *
     * @param newAvatarUri 新头像的URI
     */
    fun updateAvatar(newAvatarUri: Uri) {
        viewModelScope.launch {
            _avatarUpdateState.value = UpdateState.Updating // 设置状态为更新中
            try {
                // 1. 先将本地选择的图片转为缓存文件
                val localFile = userRepository.prepareAvatarFile(newAvatarUri)

                // 2. 上传到服务器（同时使用本地文件作为临时显示）
                _avatarFile.value = localFile // 立即更新本地显示
                val updatedUser = userRepository.updateAvatar(newAvatarUri) // 调用仓库更新头像

                // 3. 更新用户数据（此时avatarFile已指向新文件）
                _user.value = updatedUser
                _avatarUpdateState.value = UpdateState.Success("头像") // 设置状态为成功
            } catch (e: Exception) {
                _avatarUpdateState.value = UpdateState.Error(e.message ?: "未知错误") // 设置状态为错误
            }
        }
    }

    /**
     * 更新邮箱功能
     */

    /**
     * 邮箱更新状态的私有可变状态流 */
    private val _emailUpdateState = MutableStateFlow<EmailUpdateState>(EmailUpdateState.Idle)

    /**
     * 对外暴露的邮箱更新状态流 */
    val emailUpdateState: StateFlow<EmailUpdateState> = _emailUpdateState

    /**
     * 邮箱更新状态密封类
     *
     * 定义邮箱更新过程中的各种状态
     */
    sealed class EmailUpdateState {
        /** 空闲状态 */
        object Idle : EmailUpdateState()

        /** 验证旧邮箱状态，包含旧邮箱和错误信息 */
        data class VerifyOldEmail(
            val oldEmail: String,
            val error: String? = null // 新增错误字段
        ) : EmailUpdateState()

        /** 设置新邮箱状态，包含旧邮箱和错误信息 */
        data class SetNewEmail(
            val oldEmail: String,
            val error: String? = null // 新增错误字段
        ) : EmailUpdateState()

        /** 成功状态，包含新邮箱 */
        data class Success(val newEmail: String) : EmailUpdateState()
    }

    /**
     * 开始邮箱更新流程
     *
     * 初始化邮箱更新流程，进入验证旧邮箱状态
     *
     * @param email 当前用户邮箱
     */
    // 开始邮箱更新流程
    fun startEmailUpdate(email: String) {
        _emailUpdateState.value = EmailUpdateState.VerifyOldEmail(email)
    }

    /**
     * 发送旧邮箱验证码
     *
     * 向用户当前邮箱发送验证码用于验证身份
     *
     * @param email 用户当前邮箱
     */
    // 发送旧邮箱验证码
    fun sendOldEmailVerification(email: String) {
        viewModelScope.launch {
            try {
                // 调用用户服务发送并验证验证码
                val success = userService.sendAndVerifyCode(
                    email = email,
                    checkExists = false, // 不需要检查存在状态
                    expectedExists = true,
                    onError = { error ->
                        _emailUpdateState.value = EmailUpdateState.VerifyOldEmail(
                            oldEmail = email
                        )
                    }
                )

                if (success) {
                    _emailUpdateState.value = EmailUpdateState.VerifyOldEmail(
                        oldEmail = email
                    )
                }
            } catch (e: Exception) {
                _emailUpdateState.value = EmailUpdateState.VerifyOldEmail(
                    oldEmail = email
                )
            }
        }
    }

    /**
     * 验证旧邮箱验证码
     *
     * 验证用户输入的旧邮箱验证码是否正确
     *
     * @param code 用户输入的验证码
     */
    // 验证旧邮箱验证码
    fun verifyOldEmail(code: String) {
        viewModelScope.launch {
            try {
                val currentState = _emailUpdateState.value as? EmailUpdateState.VerifyOldEmail
                    ?: throw IllegalStateException("无效状态")

                // 调用用户服务验证邮箱验证码
                val isValid = userService.verifyEmailCode(currentState.oldEmail, code)
                if (isValid) {
                    // 验证成功，进入设置新邮箱状态
                    _emailUpdateState.value = EmailUpdateState.SetNewEmail(currentState.oldEmail)
                } else {
                    // 验证失败，保持当前状态并显示错误信息
                    // 强制创建新状态对象
                    _emailUpdateState.value = EmailUpdateState.VerifyOldEmail(
                        oldEmail = currentState.oldEmail,
                        error = "验证码不正确"
                    ).copy() // 使用copy确保创建新实例
                }
            } catch (e: Exception) {
                _emailUpdateState.value = EmailUpdateState.VerifyOldEmail(
                    oldEmail = (_emailUpdateState.value as? EmailUpdateState.VerifyOldEmail)?.oldEmail ?: "",
                    error = e.message ?: "验证失败"
                ).copy() // 使用copy确保创建新实例
            }
        }
    }

    /**
     * 发送新邮箱验证码
     *
     * 向用户新邮箱发送验证码用于验证
     *
     * @param newEmail 新邮箱地址
     * @param onSuccess 成功回调函数
     */
    // 发送新邮箱验证码
    fun sendNewEmailVerification(newEmail: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // 调用用户服务发送并验证验证码
                val success = userService.sendAndVerifyCode(
                    email = newEmail,
                    checkExists = true, // 需要检查存在状态
                    expectedExists = false, // 期望账号不存在
                    onError = { error ->
                        _emailUpdateState.value = EmailUpdateState.SetNewEmail(
                            oldEmail = user.value?.email ?: "",
                            error = error // 新增错误字段
                        )
                    }
                )

                if (success) {
                    onSuccess() // 调用成功回调
                }
            } catch (e: Exception) {
                _emailUpdateState.value = EmailUpdateState.SetNewEmail(
                    oldEmail = user.value?.email ?: "",
                    error = "发送验证码失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 验证新邮箱并更新
     *
     * 验证新邮箱验证码并更新用户邮箱信息
     *
     * @param email 新邮箱地址
     * @param code 验证码
     * @param onSuccess 成功回调函数
     */
    // 验证新邮箱并更新
    fun verifyNewEmail(email: String, code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentState = _emailUpdateState.value as? EmailUpdateState.SetNewEmail
                    ?: throw IllegalStateException("无效状态")

                // 调用用户服务验证邮箱验证码
                val isValid = userService.verifyEmailCode(email, code)
                if (isValid) {
                    // 验证成功，调用仓库更新邮箱
                    val updatedUser = userRepository.updateEmail(email)
                    _user.value = updatedUser
                    _emailUpdateState.value = EmailUpdateState.Success(email) // 设置状态为成功
                    onSuccess() // 调用成功回调
                } else {
                    // 验证失败，保持当前状态并显示错误信息
                    // 强制创建新状态对象
                    _emailUpdateState.value = EmailUpdateState.SetNewEmail(
                        oldEmail = currentState.oldEmail,
                        error = "验证码不正确"
                    ).copy()
                }
            } catch (e: Exception) {
                _emailUpdateState.value = EmailUpdateState.SetNewEmail(
                    oldEmail = (_emailUpdateState.value as? EmailUpdateState.SetNewEmail)?.oldEmail ?: "",
                    error = e.message ?: "验证失败"
                ).copy()
            }
        }
    }

    /**
     * 重置邮箱更新状态到空闲状态
     */
    fun resetEmailUpdateState() {
        _emailUpdateState.value = EmailUpdateState.Idle
    }

    /**
     * 修改密码功能
     */

    /**
     * 验证密码
     *
     * 验证用户输入的旧密码是否正确
     *
     * @param password 用户输入的密码
     * @return 密码是否正确
     */
    //修改密码
    suspend fun verifyPassword(password: String): Boolean {
        return try {
            userRepository.verifyPassword(password) // 调用仓库验证密码
        } catch (e: Exception) {
            false // 发生异常时返回验证失败
        }
    }

    /**
     * 修改密码
     *
     * 执行密码修改流程，包括旧密码验证和新密码更新
     *
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @param callback 结果回调函数，参数为成功状态和错误信息
     */
    fun changePassword(oldPassword: String, newPassword: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. 验证旧密码
                val isValid = verifyPassword(oldPassword)
                if (!isValid) {
                    callback(false, "旧密码不正确")
                    return@launch
                }

                // 2. 检查新旧密码是否相同
                if (oldPassword == newPassword) {
                    callback(false, "新密码不能与旧密码相同")
                    return@launch
                }

                // 3. 更新密码
                userRepository.updatePassword(newPassword) // 调用仓库更新密码
                callback(true, null) // 调用回调函数返回成功
            } catch (e: Exception) {
                callback(false, "修改密码失败: ${e.message}") // 调用回调函数返回失败
            }
        }
    }

    /**
     * 注销登录
     *
     * 清除用户数据并导航到登录页面
     *
     * @param navController 导航控制器
     */
    //注销登录
    fun logout(navController: NavController) {
        viewModelScope.launch {
            userService.clearUserData() // 清除用户数据
            // 导航到登录页面并清除返回栈
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}
