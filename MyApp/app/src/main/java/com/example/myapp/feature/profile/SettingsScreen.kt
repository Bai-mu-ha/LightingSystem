package com.example.myapp.feature.profile

import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapp.core.navigation.Screen
import com.example.myapp.feature.model.UserViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 设置界面组件
 *
 * 提供用户个人信息设置功能，包括头像、昵称、邮箱、密码修改和注销登录等操作
 *
 * @param navController 导航控制器，用于页面跳转
 * @param userViewModel 用户ViewModel，处理用户相关业务逻辑
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    // 收集用户信息状态流
    val user by userViewModel.user.collectAsState()

    // 收集用户头像文件状态流
    val avatarFile by userViewModel.avatarFile.collectAsState()

    // 定义昵称状态变量，用于存储用户输入的昵称
    var nickname by remember { mutableStateOf("") }

    // 定义临时头像URI状态变量，用于存储用户选择的新头像
    var tempAvatarUri by remember { mutableStateOf<Uri?>(null) }

    // 邮箱更新相关状态
    // 收集邮箱更新状态流
    val emailUpdateState by userViewModel.emailUpdateState.collectAsState()

    // 定义是否显示邮箱更新弹窗的状态变量
    var showEmailUpdateSheet by remember { mutableStateOf(false) }

    // 定义是否显示邮箱更新成功对话框的状态变量
    var showEmailUpdateSuccessDialog by remember { mutableStateOf(false) }

    // 定义更新后的邮箱状态变量
    var updatedEmail by remember { mutableStateOf("") }

    // 新增注销确认状态
    // 定义是否显示注销确认对话框的状态变量
    var showLogoutDialog by remember { mutableStateOf(false) }

    // 创建启动器用于选择图片文件
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // 当用户选择图片后，更新临时头像URI
        uri?.let { tempAvatarUri = it }
    }

    // 初始化昵称的副作用函数
    // 当用户信息发生变化时，更新昵称状态变量
    LaunchedEffect(user) {
        nickname = user?.username ?: ""
    }

    // 创建Snackbar宿主状态，用于显示提示信息
    val snackbarHostState = remember { SnackbarHostState() }

    // 获取协程作用域，用于执行异步操作
    val scope = rememberCoroutineScope()

    // 使用Scaffold构建页面结构，包含顶部应用栏和Snackbar宿主
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // 创建顶部应用栏
            TopAppBar(
                title = { Text("设置") }, // 设置应用栏标题
                navigationIcon = {
                    // 创建导航图标按钮
                    IconButton(onClick = { navController.popBackStack() }) {
                        // 显示返回图标
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 创建保存按钮
                    IconButton(
                        onClick = {
                            // 启动协程执行保存操作
                            scope.launch {
                                try {
                                    // 如果有新的头像，更新头像
                                    if (tempAvatarUri != null) {
                                        userViewModel.updateAvatar(tempAvatarUri!!)
                                    }
                                    // 如果昵称有变化，更新昵称
                                    if (nickname != user?.username) {
                                        userViewModel.updateUsername(nickname)
                                    }
                                    // 显示保存成功的提示
                                    snackbarHostState.showSnackbar("设置已保存")
                                } catch (e: Exception) {
                                    // 发生异常时显示保存失败的提示
                                    snackbarHostState.showSnackbar("保存失败: ${e.message}")
                                }
                            }
                        }
                    ) {
                        // 显示保存图标
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "保存"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // 设置应用栏背景色
                    titleContentColor = MaterialTheme.colorScheme.onSurface // 设置标题文字颜色
                )
            )
        }
    ) { innerPadding ->
        // 创建垂直排列的Column容器
        Column(
            modifier = Modifier
                .padding(innerPadding) // 应用内部padding
                .fillMaxSize() // 填充整个屏幕
                .verticalScroll(rememberScrollState()) // 添加垂直滚动支持
        ) {
            // 头像区域
            Column(
                modifier = Modifier
                    .fillMaxWidth() // 填充最大宽度
                    .padding(vertical = 24.dp), // 设置垂直内边距
                horizontalAlignment = Alignment.CenterHorizontally // 水平居中对齐
            ) {
                // 创建头像显示框
                Box(
                    modifier = Modifier
                        .size(120.dp) // 设置大小
                        .shadow(8.dp, shape = CircleShape) // 添加阴影效果
                        .clip(CircleShape) // 裁剪为圆形
                        .background(MaterialTheme.colorScheme.surfaceVariant) // 设置背景色
                        .clickable { launcher.launch("image/*") }, // 设置点击事件用于选择图片
                    contentAlignment = Alignment.Center // 内容居中对齐
                ) {
                    // 根据不同条件显示不同的头像
                    when {
                        // 如果有临时头像，显示临时头像
                        tempAvatarUri != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(model = tempAvatarUri), // 加载临时头像
                                contentDescription = "新头像", // 图片描述
                                modifier = Modifier.fillMaxSize(), // 填充整个框
                                contentScale = ContentScale.Crop // 裁剪方式
                            )
                        }
                        // 如果有缓存头像文件，显示缓存头像
                        avatarFile != null -> {
                            Image(
                                painter = rememberAsyncImagePainter(model = avatarFile!!.absolutePath), // 加载缓存头像
                                contentDescription = "用户头像", // 图片描述
                                modifier = Modifier.fillMaxSize(), // 填充整个框
                                contentScale = ContentScale.Crop // 裁剪方式
                            )
                        }
                        // 否则显示默认头像图标
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person, // 默认头像图标
                                contentDescription = "默认头像", // 图标描述
                                modifier = Modifier.size(60.dp), // 设置图标大小
                                tint = MaterialTheme.colorScheme.onSurfaceVariant // 设置图标颜色
                            )
                        }
                    }
                }

                // 添加垂直间距
                Spacer(modifier = Modifier.height(16.dp))

                // 显示用户邮箱
                Text(
                    text = user?.email ?: "未绑定邮箱", // 显示邮箱或默认文本
                    style = MaterialTheme.typography.bodyMedium, // 设置文本样式
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // 设置透明度
                )
            }

            // 用户信息部分
            Column(
                modifier = Modifier
                    .fillMaxWidth() // 填充最大宽度
                    .padding(horizontal = 24.dp), // 设置水平内边距
                horizontalAlignment = Alignment.CenterHorizontally // 水平居中对齐
            ) {
                // 统一尺寸参数
                val fieldWidth = 280.dp // 输入框宽度
                val buttonHeight = 48.dp // 按钮高度

                // 昵称输入框
                OutlinedTextField(
                    value = nickname, // 绑定昵称值
                    onValueChange = { nickname = it }, // 输入变化时更新昵称
                    modifier = Modifier
                        .width(280.dp) // 设置宽度
                        .height(68.dp), // 设置高度
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface, // 聚焦时背景色
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface // 失焦时背景色
                    ),
                    shape = MaterialTheme.shapes.medium, // 设置形状
                    singleLine = true, // 单行输入
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center // 使输入文本居中
                    ),
                    label = {
                        Text("昵称") // 输入框标签
                    },
                    placeholder = {
                        Text(
                            "请输入昵称", // 占位符文本
                            modifier = Modifier.fillMaxWidth(), // 填充宽度
                            textAlign = TextAlign.Center // 占位符居中
                        )
                    }
                )

                // 添加垂直间距
                Spacer(modifier = Modifier.height(24.dp))

                // 统一宽度的按钮修饰符
                val buttonModifier = Modifier
                    .width(fieldWidth) // 设置宽度
                    .height(buttonHeight) // 设置高度

                // 更改邮箱按钮
                Button(
                    onClick = {
                        showEmailUpdateSheet = true // 显示邮箱更新弹窗
                        user?.email?.let { email ->
                            userViewModel.startEmailUpdate(email) // 启动邮箱更新流程
                        }
                    },
                    modifier = buttonModifier, // 应用按钮修饰符
                    shape = MaterialTheme.shapes.medium, // 设置按钮形状
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, // 按钮背景色
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant // 按钮文字颜色
                    )
                ) {
                    Text("更改邮箱") // 按钮文本
                }

                // 添加垂直间距
                Spacer(modifier = Modifier.height(16.dp))

                // 修改密码按钮
                Button(
                    onClick = { navController.navigate(Screen.ChangePwd.route) }, // 导航到修改密码页面
                    modifier = buttonModifier, // 应用按钮修饰符
                    shape = MaterialTheme.shapes.medium, // 设置按钮形状
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, // 按钮背景色
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant // 按钮文字颜色
                    )
                ) {
                    Text("修改密码") // 按钮文本
                }

                // 添加垂直间距
                Spacer(modifier = Modifier.height(16.dp))

                // 注销登录按钮
                OutlinedButton(
                    onClick = { showLogoutDialog = true }, // 显示注销确认对话框
                    modifier = buttonModifier, // 应用按钮修饰符
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error // 按钮文字颜色
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error) // 设置边框
                ) {
                    Text("注销登录") // 按钮文本
                }
            }
        }

        // 邮箱更新弹窗
        if (showEmailUpdateSheet) {
            // 根据邮箱更新状态显示不同的对话框
            when (emailUpdateState) {
                // 验证旧邮箱状态
                is UserViewModel.EmailUpdateState.VerifyOldEmail -> {
                    VerifyOldEmailDialog(
                        state = emailUpdateState as UserViewModel.EmailUpdateState.VerifyOldEmail, // 传递状态
                        onDismiss = { showEmailUpdateSheet = false }, // 关闭弹窗回调
                        onVerify = { code -> userViewModel.verifyOldEmail(code) }, // 验证验证码回调
                        onResend = { email -> userViewModel.sendOldEmailVerification(email) } // 重新发送验证码回调
                    )
                }
                // 设置新邮箱状态
                is UserViewModel.EmailUpdateState.SetNewEmail -> {
                    SetNewEmailDialog(
                        state = emailUpdateState as UserViewModel.EmailUpdateState.SetNewEmail, // 传递状态
                        onDismiss = { showEmailUpdateSheet = false }, // 关闭弹窗回调
                        onVerify = { email, code ->
                            userViewModel.verifyNewEmail(email, code) { // 验证新邮箱回调
                                updatedEmail = email // 更新邮箱
                                showEmailUpdateSuccessDialog = true // 显示成功对话框
                            }
                        },
                        onResend = { email ->
                            userViewModel.sendNewEmailVerification(email) {} // 重新发送验证码回调
                        }
                    )
                }
                // 更新成功状态
                is UserViewModel.EmailUpdateState.Success -> {
                    showEmailUpdateSheet = false // 关闭弹窗
                    userViewModel.resetEmailUpdateState() // 重置邮箱更新状态
                }
                // 其他状态不处理
                else -> {}
            }
        }

        // 邮箱更新成功提示
        if (showEmailUpdateSuccessDialog) {
            // 创建警告对话框显示更新成功信息
            AlertDialog(
                onDismissRequest = { showEmailUpdateSuccessDialog = false }, // 关闭对话框回调
                title = { Text("邮箱更新成功") }, // 对话框标题
                text = { Text("您的新邮箱: $updatedEmail") }, // 对话框内容
                confirmButton = {
                    // 确认按钮
                    Button(
                        onClick = { showEmailUpdateSuccessDialog = false } // 关闭对话框
                    ) {
                        Text("确定") // 按钮文本
                    }
                }
            )
        }

        // 注销确认对话框
        if (showLogoutDialog) {
            // 创建警告对话框确认注销操作
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false }, // 关闭对话框回调
                title = {
                    Text("确认注销登录",
                        modifier = Modifier.fillMaxWidth(), // 填充宽度
                        textAlign = TextAlign.Center) // 文本居中
                },
                text = {
                    Text("确定要退出当前账号吗？退出后将清除本地用户数据",
                        textAlign = TextAlign.Center) // 文本居中
                },
                confirmButton = {
                    // 确认按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(), // 填充宽度
                        horizontalArrangement = Arrangement.SpaceEvenly // 水平均匀分布
                    ) {
                        // 取消按钮
                        OutlinedButton(
                            onClick = { showLogoutDialog = false }, // 关闭对话框
                            modifier = Modifier.width(120.dp) // 设置按钮宽度
                        ) {
                            Text("取消") // 按钮文本
                        }
                        // 确认退出按钮
                        Button(
                            onClick = {
                                showLogoutDialog = false // 关闭对话框
                                scope.launch {
                                    userViewModel.logout(navController) // 执行注销操作
                                }
                            },
                            modifier = Modifier.width(120.dp), // 设置按钮宽度
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error, // 按钮背景色
                                contentColor = MaterialTheme.colorScheme.onError // 按钮文字颜色
                            )
                        ) {
                            Text("确认退出") // 按钮文本
                        }
                    }
                },
                dismissButton = null // 不显示取消按钮
            )
        }
    }
}

/**
 * 验证旧邮箱对话框组件
 *
 * 用于验证用户当前邮箱的验证码输入界面
 *
 * @param state 邮箱更新状态
 * @param onDismiss 关闭对话框回调
 * @param onVerify 验证验证码回调
 * @param onResend 重新发送验证码回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifyOldEmailDialog(
    state: UserViewModel.EmailUpdateState.VerifyOldEmail,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    onResend: (String) -> Unit
) {
    // 定义验证码状态变量
    var code by remember { mutableStateOf("") }

    // 定义是否正在倒计时的状态变量
    var isCountingDown by remember { mutableStateOf(false) }

    // 定义倒计时状态变量
    var countdown by remember { mutableIntStateOf(60) }

    // 定义错误信息状态变量
    var errorMessage by remember { mutableStateOf(state.error) }
    //val scope = rememberCoroutineScope()

    // 监听状态错误信息变化的副作用函数
    LaunchedEffect(state.error) {
        errorMessage = state.error // 当state.error变化时更新本地errorMessage
    }

    // 监听错误信息变化的副作用函数
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000) // 延迟3秒
            errorMessage = null // 清除错误信息
        }
    }

    // 监听倒计时状态变化的副作用函数
    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            // 执行倒计时循环
            while (countdown > 0) {
                delay(1000) // 延迟1秒
                countdown-- // 倒计时减1
            }
            isCountingDown = false // 停止倒计时
            countdown = 60 // 重置倒计时
        }
    }

    // 创建基础警告对话框
    BasicAlertDialog(
        onDismissRequest = onDismiss, // 关闭对话框回调
        modifier = Modifier.padding(16.dp) // 设置外边距
    ) {
        // 创建表面容器
        Surface(
            modifier = Modifier.fillMaxWidth(), // 填充最大宽度
            shape = MaterialTheme.shapes.extraLarge // 设置形状
        ) {
            // 创建垂直排列的Column容器
            Column(modifier = Modifier.padding(24.dp)) {
                // 标题和关闭按钮行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 关闭按钮
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "关闭") // 关闭图标
                    }
                    // 对话框标题
                    Text("验证邮箱", style = MaterialTheme.typography.titleLarge)
                }

                // 添加垂直间距
                Spacer(Modifier.height(16.dp))

                // 显示当前邮箱
                Text("当前邮箱: ${state.oldEmail}")

                // 添加垂直间距
                Spacer(Modifier.height(16.dp))

                // 验证码输入框
                OutlinedTextField(
                    value = code, // 绑定验证码值
                    onValueChange = {
                        code = it // 更新验证码
                        errorMessage = null // 清除错误
                    },
                    label = { Text("验证码") }, // 输入框标签
                    isError = errorMessage != null, // 根据错误信息设置错误状态
                    supportingText = { errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) } }, // 错误提示文本
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // 设置键盘类型为数字
                )

                // 统一按钮样式修饰符
                val buttonModifier = Modifier
                    .fillMaxWidth() // 填充最大宽度
                    .height(48.dp) // 设置高度

                // 添加垂直间距
                Spacer(Modifier.height(8.dp))

                // 获取验证码按钮
                Button(
                    onClick = {
                        onResend(state.oldEmail) // 重新发送验证码
                        isCountingDown = true // 开始倒计时
                    },
                    modifier = buttonModifier, // 应用按钮修饰符
                    enabled = !isCountingDown, // 根据倒计时状态设置是否可用
                    shape = MaterialTheme.shapes.medium // 设置按钮形状
                ) {
                    // 根据倒计时状态显示不同文本
                    Text(if (isCountingDown) "重新发送($countdown)" else "获取验证码")
                }

                // 添加垂直间距
                Spacer(Modifier.height(16.dp))

                // 验证按钮
                Button(
                    onClick = {
                        // 验证码输入验证
                        when {
                            code.isEmpty() -> errorMessage = "请输入验证码" // 验证码为空
                            code.length != 6 -> errorMessage = "验证码必须为6位" // 验证码长度不正确
                            !code.all { it.isDigit() } -> errorMessage = "验证码必须为数字" // 验证码包含非数字字符
                            else -> {
                                errorMessage = null // 清除之前的错误
                                onVerify(code) // 调用验证回调
                            }
                        }
                    },
                    modifier = buttonModifier, // 应用按钮修饰符
                    shape = MaterialTheme.shapes.medium // 设置按钮形状
                ) {
                    Text("下一步") // 按钮文本
                }
            }
        }
    }
}

/**
 * 设置新邮箱对话框组件
 *
 * 用于设置用户新邮箱和验证码验证的界面
 *
 * @param state 邮箱更新状态
 * @param onDismiss 关闭对话框回调
 * @param onVerify 验证新邮箱回调
 * @param onResend 重新发送验证码回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetNewEmailDialog(
    state: UserViewModel.EmailUpdateState.SetNewEmail,
    onDismiss: () -> Unit,
    onVerify: (String, String) -> Unit,
    onResend: (String) -> Unit
) {
    // 定义新邮箱状态变量
    var newEmail by remember { mutableStateOf("") }

    // 定义验证码状态变量
    var code by remember { mutableStateOf("") }

    // 定义是否正在倒计时的状态变量
    var isCountingDown by remember { mutableStateOf(false) }

    // 定义倒计时状态变量
    var countdown by remember { mutableIntStateOf(60) }

    // 定义错误信息状态变量
    var errorMessage by remember { mutableStateOf(state.error) }
    //val scope = rememberCoroutineScope()

    // 监听状态错误信息变化的副作用函数
    LaunchedEffect(state.error) {
        errorMessage = state.error // 当state.error变化时更新
    }

    // 监听错误信息变化的副作用函数
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            delay(3000) // 延迟3秒
            errorMessage = null // 清除错误信息
        }
    }

    // 监听倒计时状态变化的副作用函数
    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            // 执行倒计时循环
            while (countdown > 0) {
                delay(1000) // 延迟1秒
                countdown-- // 倒计时减1
            }
            isCountingDown = false // 停止倒计时
            countdown = 60 // 重置倒计时
        }
    }

    // 创建基础警告对话框
    BasicAlertDialog(
        onDismissRequest = onDismiss, // 关闭对话框回调
        modifier = Modifier.padding(16.dp) // 设置外边距
    ) {
        // 创建表面容器
        Surface(
            modifier = Modifier.fillMaxWidth(), // 填充最大宽度
            shape = MaterialTheme.shapes.extraLarge // 设置形状
        ) {
            // 创建垂直排列的Column容器
            Column(modifier = Modifier.padding(24.dp)) {
                // 标题和关闭按钮行
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 关闭按钮
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "关闭") // 关闭图标
                    }
                    // 对话框标题
                    Text("设置新邮箱", style = MaterialTheme.typography.titleLarge)
                }

                // 添加垂直间距
                Spacer(Modifier.height(16.dp))

                // 新邮箱输入框
                OutlinedTextField(
                    value = newEmail, // 绑定新邮箱值
                    onValueChange = { newEmail = it }, // 输入变化时更新邮箱
                    label = { Text("新邮箱") }, // 输入框标签
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // 设置键盘类型为邮箱
                    isError = errorMessage?.contains("邮箱") == true // 根据错误信息设置错误状态
                )

                // 添加垂直间距
                Spacer(Modifier.height(8.dp))

                // 验证码输入框
                OutlinedTextField(
                    value = code, // 绑定验证码值
                    onValueChange = {
                        code = it // 更新验证码
                        errorMessage = null // 清除错误
                    },
                    label = { Text("验证码") }, // 输入框标签
                    isError = errorMessage != null, // 根据错误信息设置错误状态
                    supportingText = { errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) } }, // 错误提示文本
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) // 设置键盘类型为数字
                )

                // 统一按钮样式修饰符
                val buttonModifier = Modifier
                    .fillMaxWidth() // 填充最大宽度
                    .height(48.dp) // 设置高度

                // 添加垂直间距
                Spacer(Modifier.height(8.dp))

                // 获取验证码按钮
                Button(
                    onClick = {
                        // 新邮箱输入验证
                        when {
                            newEmail.isEmpty() -> errorMessage = "请输入新邮箱" // 邮箱为空
                            !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches() ->
                                errorMessage = "邮箱格式不正确" // 邮箱格式不正确
                            else -> {
                                onResend(newEmail) // 重新发送验证码
                                isCountingDown = true // 开始倒计时
                            }
                        }
                    },
                    modifier = buttonModifier, // 应用按钮修饰符
                    enabled = !isCountingDown && Patterns.EMAIL_ADDRESS.matcher(newEmail).matches(), // 根据条件设置是否可用
                    shape = MaterialTheme.shapes.medium // 设置按钮形状
                ) {
                    // 根据倒计时状态显示不同文本
                    Text(if (isCountingDown) "重新发送($countdown)" else "获取验证码")
                }

                // 添加垂直间距
                Spacer(Modifier.height(16.dp))

                // 确认按钮
                Button(
                    onClick = {
                        // 输入验证
                        when {
                            newEmail.isEmpty() -> errorMessage = "请输入新邮箱" // 新邮箱为空
                            !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches() ->
                                errorMessage = "邮箱格式不正确" // 邮箱格式不正确
                            code.isEmpty() -> errorMessage = "请输入验证码" // 验证码为空
                            code.length != 6 -> errorMessage = "验证码必须为6位" // 验证码长度不正确
                            !code.all { it.isDigit() } -> errorMessage = "验证码必须为数字" // 验证码包含非数字字符
                            else -> {
                                errorMessage = null // 清除错误
                                onVerify(newEmail, code) // 调用验证回调
                            }
                        }
                    },
                    modifier = buttonModifier, // 应用按钮修饰符
                    shape = MaterialTheme.shapes.medium // 设置按钮形状
                ) {
                    Text("确认更改") // 按钮文本
                }
            }
        }
    }
}
