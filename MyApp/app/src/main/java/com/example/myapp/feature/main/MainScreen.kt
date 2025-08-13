package com.example.myapp.feature.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.myapp.core.components.BottomNavBar
import com.example.myapp.data.Device
import com.example.myapp.feature.model.DeviceViewModel
import com.example.myapp.feature.model.DeviceViewModel.BindingState
import com.example.myapp.feature.model.UserViewModel

/**
 * 主界面组件
 *
 * 显示设备列表、公告信息，并提供设备搜索功能
 *
 * @param navController 导航控制器，用于页面跳转
 * @param viewModel 设备ViewModel，处理设备相关逻辑
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: DeviceViewModel = hiltViewModel()
) {
    // 收集绑定设备状态，实时监听绑定设备列表变化
    val boundDevices by viewModel.boundDevices.collectAsState()

    // 定义公告列表
    val announcements = listOf(
        "系统维护通知：本周六凌晨2-4点",
        "新固件v1.2.0已发布，请及时更新",
        "欢迎使用智能家居控制系统"
    )

    // 定义是否显示离线提示的状态变量
    var showOfflineToast by remember { mutableStateOf(false) }

    // 获取本地上下文，用于显示Toast提示
    val context = LocalContext.current

    // 当需要显示离线提示时，显示Toast并重置状态
    if (showOfflineToast) {
        LaunchedEffect(showOfflineToast) {
            // 显示离线提示Toast
            Toast.makeText(context, "设备离线，请开启设备", Toast.LENGTH_SHORT).show()
            showOfflineToast = false // 重置状态
        }
    }

    // 使用Scaffold构建页面结构，包含顶部应用栏和底部导航栏
    Scaffold(
        bottomBar = { BottomNavBar(navController) }, // 底部导航栏
        topBar = {
            // 创建顶部应用栏
            TopAppBar(
                title = { Text("智能家居控制") }, // 应用栏标题
                actions = {
                    // 创建搜索设备按钮
                    TextButton(
                        onClick = { navController.navigate("discovery") }, // 点击时导航到设备发现页面
                        modifier = Modifier.padding(end = 8.dp) // 设置右边距
                    ) {
                        // 创建水平排列的Row容器显示按钮内容
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 显示搜索图标
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "搜索设备",
                                modifier = Modifier.size(20.dp) // 设置图标大小
                            )
                            // 添加水平间距
                            Spacer(modifier = Modifier.width(4.dp))
                            // 显示按钮文本
                            Text("搜索设备")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // 创建垂直排列的Column容器
        Column(modifier = Modifier.padding(innerPadding)) {
            // 公告部分卡片
            Card(
                modifier = Modifier
                    .padding(16.dp) // 设置外边距
                    .fillMaxWidth(), // 填充最大宽度
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant // 设置卡片背景色
                )
            ) {
                // 创建垂直排列的Column容器显示公告内容
                Column(modifier = Modifier.padding(16.dp)) {
                    // 显示公告标题
                    Text(
                        text = "公告",
                        style = MaterialTheme.typography.titleLarge, // 设置文本样式
                        color = MaterialTheme.colorScheme.primary // 设置文本颜色
                    )
                    // 添加垂直间距
                    Spacer(modifier = Modifier.height(8.dp))
                    // 遍历公告列表显示每条公告
                    announcements.forEach { announcement ->
                        Text(
                            text = "• $announcement", // 显示公告内容
                            modifier = Modifier.padding(vertical = 4.dp) // 设置垂直内边距
                        )
                    }
                }
            }

            // 设备部分标题
            Text(
                text = "我的设备 (${boundDevices.size})", // 显示设备数量
                style = MaterialTheme.typography.titleMedium, // 设置文本样式
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp) // 设置外边距
            )

            // 根据设备列表是否为空显示不同内容
            if (boundDevices.isEmpty()) {
                // 如果没有设备，显示提示信息
                Box(
                    modifier = Modifier
                        .fillMaxSize() // 填充整个屏幕
                        .padding(16.dp), // 设置内边距
                    contentAlignment = Alignment.Center // 内容居中对齐
                ) {
                    Text("暂无设备，点击右上角按钮添加设备") // 显示提示文本
                }
            } else {
                // 如果有设备，显示设备列表
                LazyColumn {
                    // 遍历绑定设备列表创建设备卡片
                    items(
                        items = boundDevices,
                        key = { device -> device.macAddress } // 设置设备的唯一键
                    ) { device ->
                        DeviceCard(
                            device = device, // 传递设备信息
                            onClick = {
//                                navController.navigate("device_control/${device.macAddress}") {
//                                        launchSingleTop = true
//                                    }

                                // 检查设备是否在线
                                if (device.status == "online") {
                                    // 如果设备在线，导航到设备控制页面
                                    navController.navigate("device_control/${device.macAddress}") {
                                        launchSingleTop = true // 防止重复创建页面
                                    }
                                } else {
                                    // 如果设备离线，显示提示
                                    showOfflineToast = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设备卡片组件
 *
 * 显示单个设备的基本信息和状态
 *
 * @param device 设备对象，包含设备信息
 * @param onClick 卡片点击回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit
) {
    // 创建卡片组件显示设备信息
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp) // 设置水平和垂直外边距
            .fillMaxWidth() // 填充最大宽度
            .clickable(onClick = onClick), // 设置点击事件
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // 设置卡片背景色
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // 设置卡片阴影
    ) {
        // 创建水平排列的Row容器
        Row(
            modifier = Modifier
                .padding(24.dp)  // 增加内边距
                .fillMaxWidth(), // 填充最大宽度
            verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
        ) {
            // 放大图标部分
            Icon(
                imageVector = Icons.Default.DateRange, // 显示设备图标
                contentDescription = "Device Icon", // 图标描述
                modifier = Modifier.size(64.dp),  // 放大图标
                tint = if (device.status == "online")
                    MaterialTheme.colorScheme.primary // 在线时使用主题色
                else
                    MaterialTheme.colorScheme.outline // 离线时使用轮廓色
            )

            // 添加水平间距
            Spacer(modifier = Modifier.width(24.dp))  // 增加间距

            // 放大文字部分
            Column(
                modifier = Modifier.weight(1f) // 占据剩余空间
            ) {
                // 显示设备名称
                Text(
                    text = device.name, // 设备名称
                    style = MaterialTheme.typography.titleLarge,  // 使用更大的字体
                    maxLines = 1, // 最多显示一行
                    overflow = TextOverflow.Ellipsis // 超出部分显示省略号
                )
                // 添加垂直间距
                Spacer(modifier = Modifier.height(4.dp))
                // 显示设备MAC地址
                Text(
                    text = device.macAddress, // MAC地址
                    style = MaterialTheme.typography.bodyMedium,  // 中等大小字体
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // 设置透明度
                )
                // 添加垂直间距
                Spacer(modifier = Modifier.height(8.dp))
                // 创建水平排列的Row容器显示设备状态
                Row(
                    verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
                ) {
                    // 显示状态指示点
                    Box(
                        modifier = Modifier
                            .size(12.dp)  // 放大状态点
                            .background(
                                color = if (device.status == "online")
                                    Color.Green // 在线状态显示绿色
                                else
                                    Color.Red, // 离线状态显示红色
                                shape = CircleShape // 圆形形状
                            )
                    )
                    // 添加水平间距
                    Spacer(modifier = Modifier.width(8.dp))
                    // 显示状态文本
                    Text(
                        text = if (device.status == "online") "在线" else "离线", // 根据状态显示文本
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold // 设置粗体
                        )
                    )
                }
            }
        }
    }
}
