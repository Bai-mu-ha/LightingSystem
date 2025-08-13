package com.example.myapp.feature.auth

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.core.components.AuthErrorTip
import com.example.myapp.core.components.SendEmailVerification
import com.example.myapp.feature.model.AuthViewModel
import com.example.myapp.feature.model.AuthViewModel.CodeType
import com.example.myapp.feature.model.AuthViewModel.RegistrationState
import kotlinx.coroutines.delay

/**
 * 注册界面组件
 *
 * 提供用户注册功能，包括邮箱验证、验证码输入、密码设置等步骤
 *
 * @param navController 导航控制器，用于页面跳转
 * @param authViewModel 认证ViewModel，处理注册逻辑
 * @param onBack 返回按钮点击回调
 * @param onRegisterSuccess 注册成功回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    // 定义邮箱输入状态变量，用于存储用户输入的邮箱地址
    var email by remember { mutableStateOf("") }

    // 定义验证码输入状态变量，用于存储用户输入的验证码
    var code by remember { mutableStateOf("") }

    // 定义密码输入状态变量，用于存储用户输入的密码
    var password by remember { mutableStateOf("") }

    // 定义确认密码输入状态变量，用于存储用户确认密码输入
    var confirmPassword by remember { mutableStateOf("") }

    // 新增倒计时状态变量，用于存储验证码发送后的倒计时秒数
    var countdown by remember { mutableIntStateOf(0) }

    // 定义是否正在倒计时的状态变量
    var isCountingDown by remember { mutableStateOf(false) }

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

    // 监听验证码发送状态
    // 收集认证ViewModel中的验证码发送状态
    val verificationState by authViewModel.verificationState.collectAsState()

    // 定义是否正在发送验证码的状态变量
    var isSendingCode by remember { mutableStateOf(false) }

    // 监听验证码发送状态变化的副作用函数
    LaunchedEffect(verificationState) {
        // 根据验证码发送状态执行不同操作
        when (verificationState) {
            // 如果验证码发送成功
            is SendEmailVerification.VerificationState.Success -> {
                isSendingCode = false // 设置发送状态为false
                // 可以在这里显示发送成功的提示
            }
            // 如果验证码发送失败
            is SendEmailVerification.VerificationState.Error -> {
                isSendingCode = false // 设置发送状态为false
                isCountingDown = false // 停止倒计时
                // 设置注册错误信息，显示验证码发送失败的原因
                authViewModel.setRegistrationError(
                    (verificationState as SendEmailVerification.VerificationState.Error).message,
                    AuthViewModel.ErrorType.CODE
                )
            }
            // 其他状态不处理
            else -> {}
        }
    }

    // 验证码发送按钮点击处理函数
    // 处理用户点击获取验证码按钮的逻辑
    fun onSendCodeClick() {
        // 检查邮箱是否为空
        if (email.isBlank()) {
            // 如果邮箱为空，设置注册错误信息
            authViewModel.setRegistrationError("请输入邮箱", AuthViewModel.ErrorType.EMAIL)
            return // 直接返回，不执行后续操作
        }

        // 设置发送验证码状态为true
        isSendingCode = true
        // 设置倒计时状态为true
        isCountingDown = true
        // 初始化倒计时为60秒
        countdown = 60

        // 调用认证ViewModel发送验证码方法
        authViewModel.sendVerificationCode(
            email = email, // 传递邮箱地址
            type = CodeType.REGISTER, // 指定验证码类型为注册
            onResult = { success, errorMessage -> // 回调函数处理发送结果
                isSendingCode = false // 设置发送状态为false
                // 检查发送是否失败
                if (!success) {
                    isCountingDown = false // 停止倒计时
                    // 如果有错误信息，则设置注册错误
                    errorMessage?.let { msg ->
                        authViewModel.setRegistrationError(msg, AuthViewModel.ErrorType.EMAIL)
                    }
                }
            }
        )
    }

    // 收集注册状态
    val registrationState by authViewModel.registrationState.collectAsState()

    // 监听注册结果的副作用函数
    LaunchedEffect(registrationState) {
        // 根据注册状态执行不同操作
        when (val state = registrationState) {
            // 如果注册失败
            is RegistrationState.Error -> {
                // 3秒后自动隐藏错误信息
                delay(3000)
                authViewModel.resetRegistrationState() // 重置注册状态
            }
            // 如果注册成功
            is RegistrationState.Success -> {
                delay(3000) // 同样显示3秒成功信息
                onRegisterSuccess() // 调用注册成功回调
                authViewModel.resetRegistrationState() // 重置注册状态
            }
            // 其他状态不处理
            else -> Unit
        }
    }

    // 使用Scaffold组件构建页面结构，包含顶部应用栏
    Scaffold(
        topBar = {
            // 创建顶部应用栏
            TopAppBar(
                title = { Text("注册账号") }, // 设置应用栏标题
                navigationIcon = {
                    // 创建导航图标按钮
                    IconButton(onClick = { navController.popBackStack() }) {
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
            // 错误提示 - 放在Box内最上层
            AuthErrorTip(
                visible = registrationState is RegistrationState.Error, // 根据注册状态控制显示
                message = (registrationState as? RegistrationState.Error)?.message ?: "", // 显示错误信息
                modifier = Modifier
                    .align(Alignment.TopCenter) // 在顶部居中显示
            )

            // 新增成功提示
            AuthErrorTip(
                visible = registrationState is RegistrationState.Success, // 根据注册状态控制显示
                message = (registrationState as? RegistrationState.Success)?.message ?: "", // 显示成功信息
                modifier = Modifier
                    .align(Alignment.TopCenter) // 在顶部居中显示
            )

            // 创建垂直排列的Column容器，用于放置注册界面的各个组件
            Column(
                modifier = Modifier
                    .fillMaxSize() // 填充整个屏幕
                    .padding(32.dp), // 设置内边距
                verticalArrangement = Arrangement.Center // 垂直居中排列
            ) {
                // 如果注册状态为加载中，显示进度指示器
                if (registrationState is RegistrationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                // 邮箱输入框组件
                OutlinedTextField(
                    value = email, // 绑定邮箱输入值
                    onValueChange = {
                        // 限制邮箱输入长度不超过20个字符
                        if (it.length <= 20) email = it
                    },
                    label = { Text("邮箱") }, // 设置输入框标签
                    modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // 设置键盘类型为邮箱
                    // 根据注册状态和错误类型设置是否显示错误样式
                    isError = registrationState is RegistrationState.Error &&
                            (registrationState as RegistrationState.Error).type == AuthViewModel.ErrorType.EMAIL
                )

                // 添加垂直间距
                Spacer(modifier = Modifier.height(16.dp))

                // 验证码输入行组件
                Row(
                    modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // 设置水平间距
                    verticalAlignment = Alignment.CenterVertically // 添加垂直居中
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
                        // 根据注册状态和错误类型设置是否显示错误样式
                        isError = registrationState is RegistrationState.Error &&
                                (registrationState as RegistrationState.Error).type == AuthViewModel.ErrorType.CODE
                    )

                    // 获取验证码按钮组件
                    Button(
                        onClick = {
                            // 检查邮箱是否为空
                            if (email.isBlank()) {
                                // 如果邮箱为空，设置注册错误信息
                                authViewModel.setRegistrationError("请输入邮箱", AuthViewModel.ErrorType.EMAIL)
                            }
                            // 检查邮箱格式是否正确
                            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                // 如果邮箱格式错误，设置注册错误信息
                                authViewModel.setRegistrationError("邮箱格式错误", AuthViewModel.ErrorType.EMAIL)
                            }
                            // 邮箱有效时执行发送验证码操作
                            else {
                                onSendCodeClick() // 调用发送验证码函数
                            }
                        },
                        enabled = !isSendingCode && !isCountingDown, // 根据发送状态和倒计时状态控制按钮是否可用
                        modifier = Modifier.height(56.dp) // 与输入框高度对齐
                    ) {
                        // 根据倒计时状态显示不同文本
                        if (isCountingDown) Text("${countdown}s") else Text("获取验证码")
                    }
                }

                // 添加垂直间距
                Spacer(modifier = Modifier.height(16.dp))

                // 密码输入框组件
                OutlinedTextField(
                    value = password, // 绑定密码输入值
                    onValueChange = {
                        // 限制密码输入长度不超过16个字符
                        if (it.length <= 16) password = it
                    },
                    label = { Text("密码") }, // 设置输入框标签
                    visualTransformation = PasswordVisualTransformation(), // 密码输入时隐藏文本
                    modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                    // 根据注册状态和错误类型设置是否显示错误样式
                    isError = registrationState is RegistrationState.Error &&
                            (registrationState as RegistrationState.Error).type == AuthViewModel.ErrorType.PASSWORD
                )

                // 添加垂直间距
                Spacer(modifier = Modifier.height(16.dp))

                // 确认密码输入框组件
                OutlinedTextField(
                    value = confirmPassword, // 绑定确认密码输入值
                    onValueChange = {
                        // 限制确认密码输入长度不超过16个字符
                        if (it.length <= 16) confirmPassword = it
                    },
                    label = { Text("确认密码") }, // 设置输入框标签
                    visualTransformation = PasswordVisualTransformation(), // 密码输入时隐藏文本
                    modifier = Modifier.fillMaxWidth() // 填充最大宽度
                )

                // 添加垂直间距
                Spacer(modifier = Modifier.height(24.dp))

                // 立即注册按钮组件
                Button(
                    onClick = {
                        // 根据不同条件执行注册操作或显示错误信息
                        when {
                            // 检查邮箱是否为空
                            email.isBlank() -> authViewModel.setRegistrationError("请输入邮箱", AuthViewModel.ErrorType.EMAIL)
                            // 检查邮箱格式是否正确
                            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                authViewModel.setRegistrationError("邮箱格式错误", AuthViewModel.ErrorType.EMAIL)
                            // 检查验证码是否为空
                            code.isBlank() -> authViewModel.setRegistrationError("请输入验证码", AuthViewModel.ErrorType.CODE)
                            // 检查密码是否为空
                            password.isBlank() -> authViewModel.setRegistrationError("请输入密码", AuthViewModel.ErrorType.PASSWORD)
                            // 检查密码长度是否小于6位
                            password.length < 6 -> authViewModel.setRegistrationError("密码不能小于6位", AuthViewModel.ErrorType.PASSWORD)
                            // 检查两次输入的密码是否一致
                            password != confirmPassword -> authViewModel.setRegistrationError("两次密码不一致", AuthViewModel.ErrorType.PASSWORD)
                            // 所有验证通过时执行注册操作
                            else -> authViewModel.register(email, code, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth() // 填充最大宽度
                        .align(Alignment.CenterHorizontally), // 居中显示
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, // 深蓝色背景
                        contentColor = MaterialTheme.colorScheme.onPrimary // 白色文字
                    )
                ) {
                    Text("立即注册") // 设置按钮文本
                }
            }
        }
    }
}
