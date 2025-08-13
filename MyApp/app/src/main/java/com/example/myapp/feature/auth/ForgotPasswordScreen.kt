package com.example.myapp.feature.auth

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.core.components.AuthErrorTip
import com.example.myapp.core.components.SendEmailVerification
import com.example.myapp.core.navigation.Screen
import com.example.myapp.feature.model.AuthViewModel
import com.example.myapp.feature.model.AuthViewModel.CodeType
import kotlinx.coroutines.delay

/**
 * 忘记密码界面组件
 *
 * 提供用户重置密码功能，包括邮箱验证、验证码输入和新密码设置等步骤
 *
 * @param navController 导航控制器，用于页面跳转
 * @param authViewModel 认证ViewModel，处理密码重置逻辑
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // 定义邮箱输入状态变量，用于存储用户输入的邮箱地址
    var email by remember { mutableStateOf("") }

    // 定义验证码输入状态变量，用于存储用户输入的验证码
    var code by remember { mutableStateOf("") }

    // 定义新密码输入状态变量，用于存储用户输入的新密码
    var newPassword by remember { mutableStateOf("") }

    // 定义确认新密码输入状态变量，用于存储用户确认的新密码
    var confirmPassword by remember { mutableStateOf("") }

    // 定义倒计时状态变量，用于存储验证码发送后的倒计时秒数
    var countdown by remember { mutableIntStateOf(60) }

    // 定义是否正在倒计时的状态变量
    var isCountingDown by remember { mutableStateOf(false) }

    // 定义当前步骤状态变量，1表示验证身份步骤，2表示设置新密码步骤
    var currentStep by remember { mutableIntStateOf(1) }


    // 收集重置密码状态
    val resetPasswordState by authViewModel.resetPasswordState.collectAsState()

    // 收集验证码发送状态
    val verificationState by authViewModel.verificationState.collectAsState()

    // 自动重置错误状态的副作用函数
    // 当重置密码状态为错误时，3秒后自动重置状态
    LaunchedEffect(resetPasswordState is AuthViewModel.ResetPasswordState.Error) {
        // 检查重置密码状态是否为错误状态
        if (resetPasswordState is AuthViewModel.ResetPasswordState.Error) {
            delay(3000) // 等待3秒
            authViewModel.resetPasswordStateToIdle() // 重置密码状态为空闲状态
        }
    }

    // 监听重置密码成功状态的副作用函数
    // 当重置密码状态为成功时，5秒后自动跳转到登录页面
    LaunchedEffect(resetPasswordState is AuthViewModel.ResetPasswordState.Success) {
        // 检查重置密码状态是否为成功状态
        if (resetPasswordState is AuthViewModel.ResetPasswordState.Success) {
            delay(5000) // 显示5秒成功提示
            // 导航到登录页面，并清除返回栈
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            } // 返回登录页面
        }
    }

    // 倒计时LaunchedEffect
    // 当倒计时状态为true时，启动倒计时循环
    LaunchedEffect(isCountingDown) {
        // 检查是否正在倒计时
        if (isCountingDown) {
            // 当倒计时大于0时继续倒计时
            while (countdown > 0) {
                delay(1000) // 每秒减少1秒
                countdown-- // 倒计时减1
            }
            // 倒计时结束后将状态设为false
            isCountingDown = false
        }
    }

    // 监听验证码发送状态变化的副作用函数
    LaunchedEffect(verificationState) {
        // 根据验证码发送状态执行不同操作
        when (verificationState) {
            // 如果验证码发送失败
            is SendEmailVerification.VerificationState.Error -> {
                // 设置注册错误信息，显示验证码发送失败的原因
                authViewModel.setRegistrationError(
                    (verificationState as SendEmailVerification.VerificationState.Error).message,
                    AuthViewModel.ErrorType.CODE
                )
            }
            // 如果验证码发送成功
            is SendEmailVerification.VerificationState.Success -> {
                isCountingDown = true // 开始倒计时
                countdown = 60 // 初始化倒计时为60秒
            }
            // 其他状态不处理
            else -> {}
        }
    }

    // 使用Scaffold组件构建页面结构，包含顶部应用栏
    Scaffold(
        topBar = {
            // 创建顶部应用栏
            TopAppBar(
                title = {
                    // 根据当前步骤显示不同的标题
                    Text(
                        when(currentStep) {
                            1 -> "验证身份" // 第一步显示验证身份
                            2 -> "设置新密码" // 第二步显示设置新密码
                            else -> "忘记密码" // 默认显示忘记密码
                        }
                    )
                },
                navigationIcon = {
                    // 创建导航图标按钮
                    IconButton(onClick = {
                        // 根据当前步骤执行不同的返回操作
                        if (currentStep == 1) {
                            // 如果是第一步，直接返回上一页
                            navController.popBackStack()
                        } else {
                            // 如果是第二步，返回第一步
                            currentStep = 1
                        }
                    }) {
                        // 显示返回图标
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        // 创建覆盖全屏的Box容器
        Box(
            modifier = Modifier
                .fillMaxSize() // 填充整个屏幕
                .padding(innerPadding) // 应用内部padding
        ) {
            // 创建垂直排列的Column容器，用于放置忘记密码界面的各个组件
            Column(
                modifier = Modifier
                    .fillMaxSize() // 填充整个屏幕
                    .padding(16.dp), // 设置内边距
                verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
            ) {
                // 根据当前步骤显示不同的内容
                if (currentStep == 1) {
                    // 邮箱验证步骤
                    // 邮箱输入框组件
                    OutlinedTextField(
                        value = email, // 绑定邮箱输入值
                        onValueChange = {
                            // 限制邮箱输入长度不超过20个字符
                            if (it.length <= 20) email = it
                        },
                        label = { Text("邮箱") }, // 设置输入框标签
                        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                        // 根据重置密码状态设置是否显示错误样式
                        isError = resetPasswordState is AuthViewModel.ResetPasswordState.Error
                    )

                    // 验证码输入行组件
                    Row(
                        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                        verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
                    ) {
                        // 验证码输入框组件
                        OutlinedTextField(
                            value = code, // 绑定验证码输入值
                            onValueChange = {
                                // 限制验证码输入长度不超过6个字符
                                if (it.length <= 6) code = it
                            },
                            label = { Text("验证码") }, // 设置输入框标签
                            modifier = Modifier.weight(1f), // 占据剩余空间
                            // 根据重置密码状态设置是否显示错误样式
                            isError = resetPasswordState is AuthViewModel.ResetPasswordState.Error
                        )

                        // 获取验证码按钮组件
                        Button(
                            onClick = {
                                // 根据不同条件执行获取验证码操作或显示错误信息
                                when {
                                    // 检查邮箱是否为空
                                    email.isBlank() -> authViewModel.setResetPasswordError("请输入邮箱")
                                    // 检查邮箱格式是否正确
                                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                        authViewModel.setResetPasswordError("邮箱格式错误")
                                    // 邮箱有效时执行发送验证码操作
                                    else -> authViewModel.sendVerificationCode(
                                        email, // 传递邮箱地址
                                        AuthViewModel.CodeType.RESET_PASSWORD, // 指定验证码类型为重置密码
                                        onResult = { success, errorMessage -> // 回调函数处理发送结果
                                            // 检查发送是否失败
                                            if (!success) {
                                                isCountingDown = false // 停止倒计时
                                                // 如果有错误信息，则设置重置密码错误
                                                errorMessage?.let { msg ->
                                                    authViewModel.setResetPasswordError(msg)
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                            enabled = !isCountingDown // 根据倒计时状态控制按钮是否可用
                        ) {
                            // 根据倒计时状态显示不同文本
                            if (isCountingDown) Text("${countdown}s")
                            else Text("获取验证码")
                        }
                    }

                    // 下一步按钮组件
                    Button(
                        onClick = {
                            // 根据不同条件执行下一步操作或显示错误信息
                            when {
                                // 检查验证码是否为空
                                code.isBlank() -> authViewModel.setResetPasswordError("请输入验证码")
                                // 验证码不为空时执行验证码验证操作
                                else -> authViewModel.verifyCode(email, code) { success ->
                                    // 根据验证结果执行不同操作
                                    if (success) currentStep = 2 // 验证成功进入第二步
                                    else authViewModel.setResetPasswordError("验证码错误") // 验证失败显示错误信息
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth() // 填充最大宽度
                    ) {
                        Text("下一步") // 设置按钮文本
                    }
                } else {
                    // 设置新密码步骤
                    // 新密码输入框组件
                    OutlinedTextField(
                        value = newPassword, // 绑定新密码输入值
                        onValueChange = {
                            // 限制新密码输入长度不超过16个字符
                            if (it.length <= 16) newPassword = it
                        },
                        label = { Text("新密码") }, // 设置输入框标签
                        visualTransformation = PasswordVisualTransformation(), // 密码输入时隐藏文本
                        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                        // 根据重置密码状态设置是否显示错误样式
                        isError = resetPasswordState is AuthViewModel.ResetPasswordState.Error
                    )

                    // 确认新密码输入框组件
                    OutlinedTextField(
                        value = confirmPassword, // 绑定确认新密码输入值
                        onValueChange = {
                            // 限制确认新密码输入长度不超过16个字符
                            if (it.length <= 16) confirmPassword = it
                        },
                        label = { Text("确认新密码") }, // 设置输入框标签
                        visualTransformation = PasswordVisualTransformation(), // 密码输入时隐藏文本
                        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                        // 根据重置密码状态设置是否显示错误样式
                        isError = resetPasswordState is AuthViewModel.ResetPasswordState.Error
                    )

                    // 确认修改按钮组件
                    Button(
                        onClick = {
                            // 根据不同条件执行密码重置操作或显示错误信息
                            when {
                                // 检查新密码是否为空
                                newPassword.isBlank() -> authViewModel.setResetPasswordError("新密码不能为空")
                                // 检查新密码长度是否小于6位
                                newPassword.length < 6 -> authViewModel.setResetPasswordError("密码不能小于6位")
                                // 检查两次输入的密码是否一致
                                newPassword != confirmPassword -> authViewModel.setResetPasswordError("两次密码不一致")
                                // 所有验证通过时执行密码重置操作
                                else -> authViewModel.resetPassword(email, code, newPassword)
                            }
                        },
                        modifier = Modifier.fillMaxWidth() // 填充最大宽度
                    ) {
                        Text("确认修改") // 设置按钮文本
                    }
                }
            }

            // 错误提示组件
            AuthErrorTip(
                visible = resetPasswordState is AuthViewModel.ResetPasswordState.Error, // 根据重置密码状态控制显示
                message = (resetPasswordState as? AuthViewModel.ResetPasswordState.Error)?.message ?: "", // 显示错误信息
                modifier = Modifier
                    .align(Alignment.TopCenter) // 在顶部居中显示
            )

            // 成功提示组件
            AuthErrorTip(
                visible = resetPasswordState is AuthViewModel.ResetPasswordState.Success, // 根据重置密码状态控制显示
                message = (resetPasswordState as? AuthViewModel.ResetPasswordState.Success)?.message ?: "", // 显示成功信息
                modifier = Modifier
                    .align(Alignment.TopCenter), // 在顶部居中显示
                isSuccess = true // 标记为成功提示
            )
        }
    }
}
