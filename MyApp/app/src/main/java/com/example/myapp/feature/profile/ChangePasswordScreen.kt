package com.example.myapp.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapp.core.navigation.Screen
import com.example.myapp.feature.model.UserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 修改密码界面组件
 *
 * 提供用户修改密码功能，包括旧密码验证和新密码设置
 *
 * @param navController 导航控制器，用于页面跳转
 * @param userViewModel 用户ViewModel，处理用户相关业务逻辑
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    // 定义当前步骤状态变量，1表示验证旧密码，2表示设置新密码
    var currentStep by remember { mutableIntStateOf(1) }

    // 定义旧密码状态变量
    var oldPassword by remember { mutableStateOf("") }

    // 定义新密码状态变量
    var newPassword by remember { mutableStateOf("") }

    // 定义确认新密码状态变量
    var confirmPassword by remember { mutableStateOf("") }

    // 定义错误信息状态变量
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 定义是否显示成功对话框的状态变量
    var showSuccessDialog by remember { mutableStateOf(false) }

    // 创建Snackbar宿主状态，用于显示提示信息
    val snackbarHostState = remember { SnackbarHostState() }

    // 获取协程作用域，用于执行异步操作
    val scope = rememberCoroutineScope()

    // 错误消息自动消失的副作用函数
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000) // 延迟3秒
            errorMessage = null // 清除错误信息
        }
    }

    // 使用Scaffold构建页面结构，包含顶部应用栏和Snackbar宿主
    Scaffold(
        topBar = {
            // 创建居中对齐的顶部应用栏
            CenterAlignedTopAppBar(
                title = {
                    // 根据当前步骤显示不同的标题
                    Text(
                        when(currentStep) {
                            1 -> "验证身份" // 第一步标题
                            2 -> "设置新密码" // 第二步标题
                            else -> "修改密码" // 默认标题
                        },
                        style = MaterialTheme.typography.titleMedium // 设置文本样式
                    )
                },
                navigationIcon = {
                    // 创建导航图标按钮
                    IconButton(onClick = {
                        // 根据当前步骤执行不同的返回操作
                        if (currentStep == 1) navController.popBackStack() // 第一步直接返回
                        else currentStep = 1 // 第二步返回第一步
                    }) {
                        // 显示返回图标
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // 设置Snackbar宿主
    ) { innerPadding ->
        // 创建垂直排列的Column容器
        Column(
            modifier = Modifier
                .padding(innerPadding) // 应用内部padding
                .fillMaxSize() // 填充整个屏幕
                .padding(horizontal = 24.dp) // 设置水平内边距
                .verticalScroll(rememberScrollState()), // 添加垂直滚动支持
            horizontalAlignment = Alignment.CenterHorizontally, // 水平居中对齐
            verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
        ) {
            // 添加垂直间距
            Spacer(modifier = Modifier.height(24.dp))

            // 根据当前步骤显示不同内容
            if (currentStep == 1) {
                // 第一步：验证旧密码
                Column(
                    modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                    verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
                ) {
                    // 旧密码输入框
                    OutlinedTextField(
                        value = oldPassword, // 绑定旧密码值
                        onValueChange = {
                            oldPassword = it // 更新旧密码
                            errorMessage = null // 清除错误信息
                        },
                        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                        label = { Text("当前密码") }, // 输入框标签
                        visualTransformation = PasswordVisualTransformation(), // 密码输入时隐藏文本
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // 设置键盘类型为密码
                        isError = errorMessage != null, // 根据错误信息设置错误状态
                        trailingIcon = {
                            // 忘记密码按钮
                            TextButton(
                                onClick = { navController.navigate(Screen.ForgotPassword.route) }, // 导航到忘记密码页面
                                modifier = Modifier.padding(end = 4.dp) // 设置右边距
                            ) {
                                // 显示忘记密码文本
                                Text(
                                    "忘记密码？",
                                    style = MaterialTheme.typography.labelSmall, // 设置文本样式
                                    color = MaterialTheme.colorScheme.primary // 设置文字颜色
                                )
                            }
                        }
                    )

                    // 错误提示（使用Snackbar样式但不影响布局）
                    if (errorMessage != null) {
                        // 创建表面容器显示错误信息
                        Surface(
                            modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                            shape = MaterialTheme.shapes.small, // 设置形状
                            color = MaterialTheme.colorScheme.errorContainer, // 设置背景色
                            contentColor = MaterialTheme.colorScheme.onErrorContainer // 设置文字颜色
                        ) {
                            // 显示错误文本
                            Text(
                                text = errorMessage!!, // 错误信息
                                modifier = Modifier.padding(8.dp), // 设置内边距
                                style = MaterialTheme.typography.bodySmall // 设置文本样式
                            )
                        }
                    }

                    // 添加垂直间距
                    Spacer(modifier = Modifier.height(16.dp))

                    // 验证按钮
                    Button(
                        onClick = {
                            // 旧密码输入验证
                            when {
                                oldPassword.isEmpty() -> errorMessage = "请输入当前密码" // 密码为空
                                else -> {
                                    // 启动协程验证旧密码
                                    scope.launch {
                                        try {
                                            // 调用ViewModel验证密码
                                            val isValid = userViewModel.verifyPassword(oldPassword)
                                            if (isValid) {
                                                currentStep = 2 // 验证成功进入第二步
                                                errorMessage = null // 清除错误信息
                                            } else {
                                                errorMessage = "当前密码不正确" // 验证失败
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "验证失败: ${e.message}" // 异常处理
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth() // 填充最大宽度
                            .height(48.dp), // 设置高度
                        shape = MaterialTheme.shapes.medium // 设置按钮形状
                    ) {
                        Text("下一步") // 按钮文本
                    }
                }
            } else {
                // 第二步：设置新密码
                Column(
                    modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                    verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
                ) {
                    // 新密码输入框
                    OutlinedTextField(
                        value = newPassword, // 绑定新密码值
                        onValueChange = {
                            newPassword = it // 更新新密码
                            errorMessage = null // 清除错误信息
                        },
                        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                        label = { Text("新密码") }, // 输入框标签
                        visualTransformation = PasswordVisualTransformation(), // 密码输入时隐藏文本
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // 设置键盘类型为密码
                        isError = errorMessage?.contains("新密码") == true, // 根据错误信息设置错误状态
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface, // 聚焦时背景色
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface // 失焦时背景色
                        ),
                        shape = MaterialTheme.shapes.medium // 设置输入框形状
                    )

                    // 确认新密码输入框
                    OutlinedTextField(
                        value = confirmPassword, // 绑定确认新密码值
                        onValueChange = {
                            confirmPassword = it // 更新确认新密码
                            errorMessage = null // 清除错误信息
                        },
                        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                        label = { Text("确认新密码") }, // 输入框标签
                        visualTransformation = PasswordVisualTransformation(), // 密码输入时隐藏文本
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // 设置键盘类型为密码
                        isError = errorMessage?.contains("不匹配") == true, // 根据错误信息设置错误状态
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface, // 聚焦时背景色
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface // 失焦时背景色
                        ),
                        shape = MaterialTheme.shapes.medium // 设置输入框形状
                    )

                    // 错误提示
                    if (errorMessage != null) {
                        // 创建表面容器显示错误信息
                        Surface(
                            modifier = Modifier.fillMaxWidth(), // 填充最大宽度
                            shape = MaterialTheme.shapes.small, // 设置形状
                            color = MaterialTheme.colorScheme.errorContainer, // 设置背景色
                            contentColor = MaterialTheme.colorScheme.onErrorContainer // 设置文字颜色
                        ) {
                            // 显示错误文本
                            Text(
                                text = errorMessage!!, // 错误信息
                                modifier = Modifier.padding(8.dp), // 设置内边距
                                style = MaterialTheme.typography.bodySmall // 设置文本样式
                            )
                        }
                    }

                    // 添加垂直间距
                    Spacer(modifier = Modifier.height(16.dp))

                    // 确认按钮
                    Button(
                        onClick = {
                            // 新密码输入验证
                            when {
                                newPassword.isEmpty() -> errorMessage = "请输入新密码" // 新密码为空
                                confirmPassword.isEmpty() -> errorMessage = "请确认新密码" // 确认密码为空
                                newPassword != confirmPassword -> errorMessage = "两次输入的密码不匹配" // 密码不匹配
                                newPassword.length < 6 -> errorMessage = "密码长度不能少于6位" // 密码长度不足
                                else -> {
                                    // 启动协程修改密码
                                    scope.launch {
                                        try {
                                            // 调用ViewModel修改密码
                                            userViewModel.changePassword(
                                                oldPassword = oldPassword, // 传递旧密码
                                                newPassword = newPassword // 传递新密码
                                            ) { success, message ->
                                                if (success) {
                                                    showSuccessDialog = true // 显示成功对话框
                                                } else {
                                                    errorMessage = message // 显示错误信息
                                                }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "修改失败: ${e.message}" // 异常处理
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth() // 填充最大宽度
                            .height(48.dp), // 设置高度
                        shape = MaterialTheme.shapes.medium // 设置按钮形状
                    ) {
                        Text("确认修改") // 按钮文本
                    }
                }
            }
        }
    }

    // 修改成功对话框
    if (showSuccessDialog) {
        // 创建警告对话框显示修改成功信息
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false // 关闭对话框
                oldPassword = "" // 清空旧密码
                newPassword = "" // 清空新密码
                confirmPassword = "" // 清空确认密码
                // 导航到登录页面并清除返回栈
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.ChangePwd.route) { inclusive = true }
                }
            },
            title = { Text("修改成功") }, // 对话框标题
            text = { Text("密码已成功修改，请使用新密码登录") }, // 对话框内容
            confirmButton = {
                // 确认按钮
                Button(
                    onClick = {
                        showSuccessDialog = false // 关闭对话框
                        oldPassword = "" // 清空旧密码
                        newPassword = "" // 清空新密码
                        confirmPassword = "" // 清空确认密码
                        // 导航到登录页面并清除返回栈
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.ChangePwd.route) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary, // 按钮背景色
                        contentColor = MaterialTheme.colorScheme.onPrimary // 按钮文字颜色
                    )
                ) {
                    Text("确定") // 按钮文本
                }
            }
        )
    }
}
