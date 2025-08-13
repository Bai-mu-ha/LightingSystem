package com.example.myapp.feature.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapp.core.components.AvatarImage
import com.example.myapp.core.components.BottomNavBar
import com.example.myapp.feature.model.UserViewModel
import java.io.File

/**
 * 个人中心界面组件
 *
 * 显示用户个人信息和相关功能入口
 *
 * @param navController 导航控制器，用于页面跳转
 * @param userViewModel 用户ViewModel，处理用户相关业务逻辑
 * @param onSettingsClick 设置按钮点击回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavHostController,
    userViewModel: UserViewModel,
    onSettingsClick:() -> Unit
) {
    // 收集用户信息状态流
    val user by userViewModel.user.collectAsState(null)

    // 收集用户头像文件状态流
    val avatarFile by userViewModel.avatarFile.collectAsState(null)

    // 定义是否显示全屏头像的状态变量
    var showFullScreenAvatar by remember { mutableStateOf(false) }

    // 如果需要显示全屏头像，创建全屏头像组件
    if (showFullScreenAvatar) {
        FullScreenAvatar(
            avatarFile = avatarFile, // 传递头像文件
            onDismiss = { showFullScreenAvatar = false } // 关闭回调
        )
    }

    // 使用Scaffold构建页面结构，包含顶部应用栏和底部导航栏
    Scaffold(
        topBar = {
            // 创建顶部应用栏
            TopAppBar(
                title = { Text("个人中心") }, // 设置应用栏标题
                actions = {
                    // 设置按钮移到右上角
                    IconButton(onClick = onSettingsClick) {
                        // 显示设置图标
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onSurface // 设置图标颜色
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface, // 设置应用栏背景色
                    titleContentColor = MaterialTheme.colorScheme.onSurface // 设置标题文字颜色
                )
            )
        },
        bottomBar = { BottomNavBar(navController) } // 保留底部导航栏
    ) { innerPadding ->
        // 创建Box容器应用内部padding
        Box(
            modifier = Modifier
                .padding(innerPadding) // 应用内部padding
                .fillMaxSize() // 填充整个屏幕
        ) {
            // 创建垂直排列的Column容器
            Column(
                modifier = Modifier
                    .fillMaxSize() // 填充整个屏幕
                    .verticalScroll(rememberScrollState()), // 添加垂直滚动支持
                horizontalAlignment = Alignment.CenterHorizontally // 水平居中对齐
            ) {
                // 添加垂直间距
                Spacer(modifier = Modifier.height(32.dp))

                // 用户头像
                Box(
                    modifier = Modifier
                        .size(120.dp) // 设置大小
                        .shadow(8.dp, shape = CircleShape) // 添加阴影效果
                        .clip(CircleShape) // 裁剪为圆形
                        .background(MaterialTheme.colorScheme.surfaceVariant) // 设置背景色
                        .clickable {
                            if (avatarFile != null) showFullScreenAvatar = true // 点击时显示全屏头像
                        }, // 设置点击事件
                    contentAlignment = Alignment.Center // 内容居中对齐
                ) {
                    // 使用AvatarImage组件显示头像
                    AvatarImage(
                        avatarFile = avatarFile, // 传递头像文件
                        modifier = Modifier.size(120.dp) // 设置大小
                    )
                }

                // 添加垂直间距
                Spacer(modifier = Modifier.height(16.dp))

                // 用户名显示
                Text(
                    text = user?.username ?: "用户名", // 显示用户名或默认文本
                    style = MaterialTheme.typography.headlineSmall, // 设置文本样式
                    fontWeight = FontWeight.Bold // 设置粗体
                )

                // 邮箱显示
                Text(
                    text = user?.email ?: "邮箱未设置", // 显示邮箱或默认文本
                    style = MaterialTheme.typography.bodyMedium, // 设置文本样式
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // 设置透明度
                )

                // 添加垂直间距
                Spacer(modifier = Modifier.height(32.dp))

                // 功能按钮区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth() // 填充最大宽度
                        .padding(horizontal = 24.dp) // 设置水平内边距
                ) {
                    // 版本信息按钮
                    ProfileButton(
                        icon = Icons.Default.Info, // 按钮图标
                        text = "版本信息", // 按钮文本
                        onClick = { /* TODO: 跳转版本信息 */ } // 点击回调
                    )

                    // 帮助中心按钮
                    ProfileButton(
                        icon = Icons.Default.Search, // 按钮图标
                        text = "帮助中心", // 按钮文本
                        onClick = { /* TODO: 跳转帮助中心 */ } // 点击回调
                    )

                    // 使用教程按钮
                    ProfileButton(
                        icon = Icons.Default.Notifications, // 按钮图标
                        text = "使用教程", // 按钮文本
                        onClick = { /* TODO: 跳转教程页面 */ } // 点击回调
                    )

                    // 反馈建议按钮
                    ProfileButton(
                        icon = Icons.Default.Create, // 按钮图标
                        text = "反馈建议", // 按钮文本
                        onClick = { /* TODO: 跳转反馈页面 */ } // 点击回调
                    )
                }
            }
        }
    }
}

/**
 * 个人中心功能按钮组件
 *
 * 显示个人中心页面的功能按钮
 *
 * @param icon 按钮图标
 * @param text 按钮文本
 * @param onClick 按钮点击回调
 * @param modifier 修饰符
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun ProfileButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 创建轮廓按钮
    OutlinedButton(
        onClick = onClick, // 设置点击回调
        modifier = modifier
            .fillMaxWidth() // 填充最大宽度
            .padding(horizontal = 24.dp, vertical = 8.dp), // 设置内边距
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface // 设置文字颜色
        )
    ) {
        // 显示按钮图标
        Icon(
            imageVector = icon,
            contentDescription = null, // 图标描述
            modifier = Modifier.size(24.dp) // 设置图标大小
        )
        // 添加水平间距
        Spacer(modifier = Modifier.width(16.dp))
        // 显示按钮文本
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge // 设置文本样式
        )
        // 添加弹性间距占据剩余空间
        Spacer(modifier = Modifier.weight(1f))
        // 显示前往图标
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "前往",
            modifier = Modifier.size(20.dp) // 设置图标大小
        )
    }
}

/**
 * 全屏头像组件
 *
 * 以全屏方式显示用户头像
 *
 * @param avatarFile 头像文件
 * @param onDismiss 关闭回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun FullScreenAvatar(
    avatarFile: File?,
    onDismiss: () -> Unit
) {
    // 创建对话框显示全屏头像
    Dialog(
        onDismissRequest = onDismiss, // 关闭回调
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 不使用平台默认宽度
            dismissOnClickOutside = true // 点击外部可关闭
        )
    ) {
        // 创建Box容器显示头像内容
        Box(
            modifier = Modifier
                .fillMaxSize() // 填充整个屏幕
                .background(Color.Black.copy(alpha = 0.9f)) // 设置半透明黑色背景
                .clickable(onClick = onDismiss), // 点击任意位置关闭
            contentAlignment = Alignment.Center // 内容居中对齐
        ) {
            // 如果有头像文件，显示头像图片
            if (avatarFile != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = avatarFile.absolutePath), // 加载头像
                    contentDescription = "全屏头像", // 图片描述
                    modifier = Modifier.fillMaxSize(), // 填充整个屏幕
                    contentScale = ContentScale.Fit // 设置缩放方式
                )
            } else {
                // 否则显示加载指示器
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center) // 居中显示
                )
            }

            // 关闭按钮
            IconButton(
                onClick = onDismiss, // 关闭回调
                modifier = Modifier
                    .align(Alignment.TopEnd) // 定位到右上角
                    .padding(16.dp) // 设置内边距
            ) {
                // 显示关闭图标
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurface // 设置图标颜色
                )
            }
        }
    }
}
