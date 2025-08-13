package com.example.myapp.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myapp.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.myapp.core.components.AuthErrorTip
import com.example.myapp.core.navigation.Screen
import com.example.myapp.feature.model.AuthViewModel
import com.example.myapp.feature.model.DeviceViewModel
import com.example.myapp.feature.model.UserViewModel
import kotlinx.coroutines.delay

/**
 * 登录界面组件
 *
 * 提供用户登录功能，包括邮箱和密码输入、登录验证、注册和忘记密码跳转等功能
 *
 * @param navController 导航控制器，用于页面跳转
 * @param authViewModel 认证ViewModel，处理登录逻辑
 * @param userViewModel 用户ViewModel，处理用户数据
 * @param onRegisterClicked 注册按钮点击回调
 * @param onForgotPasswordClicked 忘记密码按钮点击回调
 * @param onLoginSuccess 登录成功回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    onRegisterClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit,
    onLoginSuccess: (email: String) -> Unit
) {
    // 定义邮箱输入状态变量，用于存储用户输入的邮箱地址
    var emailInput by remember { mutableStateOf("") }

    // 定义密码输入状态变量，用于存储用户输入的密码
    var passwordInput by remember { mutableStateOf("") }

    // 收集认证ViewModel中的登录状态，用于监听登录结果
    val loginState by authViewModel.loginState.collectAsState()

    // 收集数据加载状态，用于监听用户数据加载过程
    val loadingState by authViewModel.dataLoadingState.collectAsState()

    // 通过Hilt注入设备ViewModel实例
    val deviceViewModel: DeviceViewModel = hiltViewModel()

    // 监听登录状态变化的副作用函数
    // 当登录状态发生变化时执行相应的处理逻辑
    LaunchedEffect(loginState) {
        // 检查当前登录状态是否为成功状态
        when (loginState) {
            // 如果登录成功，执行后续操作
            is AuthViewModel.LoginState.Success -> {
                // 初始化用户登录后的数据
                userViewModel.initializeAfterLogin()

                // 根据数据加载状态执行不同的导航操作
                when (loadingState) {
                    // 如果数据加载成功，导航到主页
                    is AuthViewModel.DataLoadingState.Success -> {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                    // 如果数据加载失败，可以显示错误或重试（当前未实现）
                    is AuthViewModel.DataLoadingState.Error -> {
                        // 可以显示错误或重试
                    }
                    // 其他状态不处理
                    else -> {} // 其他状态不处理
                }
            }
            // 其他登录状态不处理
            else -> {}
        }
    }

    // 在登录按钮下方添加加载指示器
    // 当数据加载状态为加载中时显示进度指示器和提示文本
    if (loadingState == AuthViewModel.DataLoadingState.Loading) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("正在加载数据...")
        }
    }

    // 监听登录错误状态的副作用函数
    // 当登录状态为错误时，3秒后自动重置登录状态
    LaunchedEffect(loginState is AuthViewModel.LoginState.Error) {
        // 检查当前登录状态是否为错误状态
        if (loginState is AuthViewModel.LoginState.Error) {
            delay(3000) // 3秒后自动消失
            authViewModel.resetLoginState() // 重置登录状态
        }
    }

    // 创建一个覆盖全屏的Box容器
    Box(modifier = Modifier.fillMaxSize()) {
        // 创建垂直排列的Column容器，用于放置登录界面的各个组件
        Column(
            modifier = Modifier
                .fillMaxSize() // 填充整个屏幕
                .padding(horizontal = 32.dp) // 只设置水平padding
                .padding(top = 16.dp), // 顶部padding减少，整体上移
            verticalArrangement = Arrangement.Top, // 改为顶部对齐
            horizontalAlignment = Alignment.CenterHorizontally // 水平居中对齐
        ) {
            // 添加垂直间距，使界面元素向下偏移
            Spacer(modifier = Modifier.height(100.dp))

            // 显示应用图标
            Image(
                painter = painterResource(R.drawable.myicon), // 使用默认资源
                contentDescription = null, // 临时设为null
                modifier = Modifier.size(120.dp) // 设置图标大小
            )

            // 添加垂直间距
            Spacer(modifier = Modifier.height(24.dp))

            // 显示应用名称
            Text(
                text = stringResource(R.string.app_name), // 从资源文件获取应用名称
                style = MaterialTheme.typography.headlineMedium // 设置文本样式
            )

            // 添加垂直间距
            Spacer(modifier = Modifier.height(40.dp))

            // 统一输入框样式，定义输入框的通用修饰符
            val textFieldModifier = Modifier
                .fillMaxWidth(0.85f) // 设置为85%宽度

            // 邮箱输入框组件
            OutlinedTextField(
                value = emailInput, // 绑定邮箱输入值
                onValueChange = {
                    // 限制邮箱输入长度不超过20个字符
                    if (it.length <= 20) emailInput = it
                },
                label = { Text("邮箱") }, // 设置输入框标签
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // 设置键盘类型为邮箱
                modifier = textFieldModifier, // 应用通用修饰符
                shape = MaterialTheme.shapes.medium, // 设置输入框形状
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, // 聚焦时边框颜色
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline // 失焦时边框颜色
                )
            )

            // 添加垂直间距
            Spacer(modifier = Modifier.height(16.dp))

            // 密码输入框组件
            OutlinedTextField(
                value = passwordInput, // 绑定密码输入值
                onValueChange = {
                    // 限制密码输入长度不超过16个字符
                    if (it.length <= 16) passwordInput = it
                },
                label = { Text("密码") }, // 设置输入框标签
                visualTransformation = PasswordVisualTransformation(), // 密码输入时隐藏文本
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // 设置键盘类型为密码
                modifier = textFieldModifier, // 应用通用修饰符
                shape = MaterialTheme.shapes.medium, // 设置输入框形状
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary, // 聚焦时边框颜色
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline // 失焦时边框颜色
                )
            )

            // 添加垂直间距
            Spacer(modifier = Modifier.height(48.dp))

            // 统一按钮样式，定义按钮的通用修饰符
            val buttonModifier = Modifier
                .fillMaxWidth(0.85f) // 与输入框同宽
                .height(48.dp) // 统一高度

            // 登录按钮组件
            Button(
                onClick = {
                    // 点击时调用认证ViewModel的登录方法
                    authViewModel.login(emailInput, passwordInput)
                },
                modifier = buttonModifier, // 应用通用修饰符
                enabled = loginState !is AuthViewModel.LoginState.Loading, // 登录过程中禁用按钮
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary, // 按钮背景色
                    contentColor = MaterialTheme.colorScheme.onPrimary // 按钮文字颜色
                ),
                shape = MaterialTheme.shapes.medium // 设置按钮形状
            ) {
                // 根据登录状态显示不同内容
                if (loginState is AuthViewModel.LoginState.Loading) {
                    // 登录中显示进度指示器
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), // 设置指示器大小
                        color = MaterialTheme.colorScheme.onPrimary // 设置指示器颜色
                    )
                } else {
                    // 非登录中状态显示登录文本
                    Text("登录", style = MaterialTheme.typography.labelLarge)
                }
            }

            // 添加垂直间距
            Spacer(modifier = Modifier.height(12.dp))

            // 注册账号按钮组件
            TextButton(
                onClick = onRegisterClicked, // 点击时调用注册点击回调
                modifier = buttonModifier, // 应用通用修饰符
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary // 设置按钮文字颜色
                )
            ) {
                Text("注册账号", style = MaterialTheme.typography.labelLarge) // 设置按钮文本
            }

            // 忘记密码按钮组件
            TextButton(
                onClick = onForgotPasswordClicked, // 点击时调用忘记密码点击回调
                modifier = buttonModifier, // 应用通用修饰符
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary // 设置按钮文字颜色
                )
            ) {
                Text("忘记密码", style = MaterialTheme.typography.labelLarge) // 设置按钮文本
            }
        }
    }

    // 显示认证错误提示组件
    AuthErrorTip(
        visible = loginState is AuthViewModel.LoginState.Error, // 根据登录状态控制显示
        message = when (loginState) {
            // 当登录状态为错误时显示错误消息
            is AuthViewModel.LoginState.Error -> (loginState as AuthViewModel.LoginState.Error).message
            else -> "" // 其他状态显示空字符串
        },
    )
}
