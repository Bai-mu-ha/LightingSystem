package com.example.myapp.feature.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapp.data.Device
import com.example.myapp.feature.model.DeviceViewModel
import kotlinx.coroutines.delay

/**
 * 设备发现界面组件
 *
 * 提供搜索和发现新设备的功能，并支持设备绑定操作
 *
 * @param onBack 返回按钮点击回调
 * @param viewModel 设备ViewModel，处理设备发现和绑定逻辑
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoveryScreen(
    onBack: () -> Unit,
    viewModel: DeviceViewModel = hiltViewModel()
) {
    // 收集设备列表状态，实时监听发现的设备列表变化
    val devices by viewModel.devices.collectAsState()

    // 收集发现状态，监听设备发现过程状态变化
    val discoveryState by viewModel.discoveryState.collectAsState()

    // 收集绑定状态，监听设备绑定过程状态变化
    val bindingState by viewModel.bindingState.collectAsState()

    // 控制绑定状态提示的显示状态变量
    var showBindingMessage by remember { mutableStateOf(false) }

    // 绑定状态消息状态变量
    var bindingMessage by remember { mutableStateOf("") }

    // 绑定是否成功的状态变量
    var isBindingSuccess by remember { mutableStateOf(false) }

    // 处理绑定状态提示的副作用函数
    LaunchedEffect(bindingState) {
        // 根据绑定状态执行不同操作
        when (bindingState) {
            // 如果绑定成功
            is DeviceViewModel.BindingState.Success -> {
                bindingMessage = "设备绑定成功" // 设置成功消息
                isBindingSuccess = true // 标记为成功
                showBindingMessage = true // 显示提示
                delay(3000) // 延迟3秒
                showBindingMessage = false // 隐藏提示
                viewModel.resetBindingState() // 重置绑定状态
            }
            // 如果绑定失败
            is DeviceViewModel.BindingState.Error -> {
                // 设置错误消息
                bindingMessage = (bindingState as DeviceViewModel.BindingState.Error).message
                isBindingSuccess = false // 标记为失败
                showBindingMessage = true // 显示提示
                delay(3000) // 延迟3秒
                showBindingMessage = false // 隐藏提示
                viewModel.resetBindingState() // 重置绑定状态
            }
            // 其他状态不处理
            else -> {}
        }
    }

    // 处理搜索超时的状态变量
    var showTimeoutMessage by remember { mutableStateOf(false) }

    // 处理搜索超时的副作用函数
    LaunchedEffect(discoveryState) {
        // 如果发现状态为加载中
        if (discoveryState is DeviceViewModel.DiscoveryState.Loading) {
            showTimeoutMessage = false // 重置超时提示
            delay(20000) // 延迟20秒（超时时间）
            // 只需要检查设备列表是否为空，因为discoveryState可能已经改变
            if (devices.isEmpty()) {
                showTimeoutMessage = true // 显示超时提示
                // 这里需要确保ViewModel允许修改状态
                viewModel.resetDiscoveryState() // 添加这个方法到ViewModel
            }
        }
    }

    // 使用Scaffold构建页面结构，包含顶部应用栏
    Scaffold(
        topBar = {
            // 创建顶部应用栏
            TopAppBar(
                title = { Text("设备发现") }, // 应用栏标题
                navigationIcon = {
                    // 创建导航图标按钮
                    IconButton(onClick = onBack) {
                        // 显示返回图标
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    // 创建水平排列的Row容器显示搜索操作
                    Row(
                        verticalAlignment = Alignment.CenterVertically, // 垂直居中对齐
                        modifier = Modifier.clickable { viewModel.discoverDevices() } // 设置点击事件
                    ) {
                        // 显示搜索文本
                        Text(
                            text = "搜索设备",
                            style = MaterialTheme.typography.bodyMedium, // 设置文本样式
                            modifier = Modifier.padding(end = 4.dp) // 设置右边距
                        )
                        // 创建图标按钮
                        IconButton(onClick = { viewModel.discoverDevices() }) {
                            // 显示搜索图标
                            Icon(Icons.Default.Search, "搜索设备")
                        }
                    }
                }
            )
        }
    ) { padding ->
        // 创建Box容器应用内边距
        Box(modifier = Modifier.padding(padding)) {
            // 根据发现状态显示不同内容
            when (discoveryState) {
                // 如果发现状态为加载中
                is DeviceViewModel.DiscoveryState.Loading -> {
                    // 创建垂直排列的Column容器显示加载状态
                    Column(
                        modifier = Modifier.fillMaxSize(), // 填充整个屏幕
                        horizontalAlignment = Alignment.CenterHorizontally, // 水平居中对齐
                        verticalArrangement = Arrangement.Center // 垂直居中排列
                    ) {
                        // 显示进度指示器
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp), // 设置大小
                            strokeWidth = 4.dp, // 设置线宽
                            color = MaterialTheme.colorScheme.primary // 设置颜色
                        )
                        // 添加垂直间距
                        Spacer(modifier = Modifier.height(16.dp))
                        // 显示加载文本
                        Text("正在搜索设备...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                // 如果发现状态为错误
                is DeviceViewModel.DiscoveryState.Error -> {
                    // 创建Box容器居中显示错误信息
                    Box(
                        modifier = Modifier.fillMaxSize(), // 填充整个屏幕
                        contentAlignment = Alignment.Center // 内容居中对齐
                    ) {
                        // 显示错误文本
                        Text(
                            text = (discoveryState as DeviceViewModel.DiscoveryState.Error).message, // 错误消息
                            color = MaterialTheme.colorScheme.error // 设置错误颜色
                        )
                    }
                }
                // 其他状态（成功或空闲）
                else -> {
                    // 如果需要显示超时提示
                    if (showTimeoutMessage) {
                        // 创建Box容器居中显示超时信息
                        Box(
                            modifier = Modifier.fillMaxSize(), // 填充整个屏幕
                            contentAlignment = Alignment.Center // 内容居中对齐
                        ) {
                            // 显示超时提示文本
                            Text(
                                text = "未找到设备，请确保设备已开启并处于同一网络",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // 设置透明度
                            )
                        }
                    }
                    // 如果设备列表为空
                    else if (devices.isEmpty()) {
                        // 创建Box容器居中显示提示信息
                        Box(
                            modifier = Modifier.fillMaxSize(), // 填充整个屏幕
                            contentAlignment = Alignment.Center // 内容居中对齐
                        ) {
                            // 显示提示文本
                            Text(
                                text = "点击搜索按钮开始查找设备",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // 设置透明度
                            )
                        }
                    }
                    // 如果有发现的设备
                    else {
                        // 创建LazyColumn显示设备列表
                        LazyColumn {
                            // 遍历设备列表创建设备项
                            items(devices) { device ->
                                DeviceDiscoveryItem(
                                    device = device, // 传递设备信息
                                    onBindClick = { viewModel.connectToDevice(device) } // 绑定点击回调
                                )
                            }
                        }
                    }
                }
            }

            // 绑定状态提示 (浮动在底部)
            // 如果需要显示绑定状态提示
            if (showBindingMessage) {
                // 创建Box容器定位在底部中心
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter) // 底部居中对齐
                        .padding(16.dp) // 设置内边距
                ) {
                    // 创建卡片组件显示绑定状态
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBindingSuccess)
                                MaterialTheme.colorScheme.primaryContainer // 成功时背景色
                            else
                                MaterialTheme.colorScheme.errorContainer // 失败时背景色
                        ),
                        elevation = CardDefaults.cardElevation(8.dp) // 设置阴影
                    ) {
                        // 显示绑定状态文本
                        Text(
                            text = bindingMessage, // 绑定消息
                            modifier = Modifier.padding(16.dp), // 设置内边距
                            color = if (isBindingSuccess)
                                MaterialTheme.colorScheme.onPrimaryContainer // 成功时文字颜色
                            else
                                MaterialTheme.colorScheme.onErrorContainer // 失败时文字颜色
                        )
                    }
                }
            }
        }
    }
}

/**
 * 设备发现项组件
 *
 * 显示单个发现设备的信息和绑定按钮
 *
 * @param device 设备对象，包含设备信息
 * @param onBindClick 绑定按钮点击回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun DeviceDiscoveryItem(
    device: Device,
    onBindClick: () -> Unit
) {
    // 创建卡片组件显示设备信息
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp) // 设置水平和垂直外边距
            .fillMaxWidth() // 填充最大宽度
            .clickable(onClick = onBindClick), // 设置点击事件
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant // 设置卡片背景色
        ),
        elevation = CardDefaults.cardElevation(4.dp) // 设置阴影
    ) {
        // 创建水平排列的Row容器
        Row(
            modifier = Modifier.padding(16.dp), // 设置内边距
            verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
        ) {
            // 显示设备图标
            Icon(
                imageVector = Icons.Default.DateRange, // 设备图标
                contentDescription = "设备图标", // 图标描述
                modifier = Modifier.size(48.dp), // 设置大小
                tint = MaterialTheme.colorScheme.primary // 设置颜色
            )
            // 添加水平间距
            Spacer(modifier = Modifier.width(16.dp))
            // 创建垂直排列的Column容器显示设备信息
            Column {
                // 显示设备名称
                Text(
                    text = device.name, // 设备名称
                    style = MaterialTheme.typography.titleMedium, // 设置文本样式
                    color = MaterialTheme.colorScheme.onSurface // 设置文字颜色
                )
                // 添加垂直间距
                Spacer(modifier = Modifier.height(4.dp))
                // 显示设备MAC地址
                Text(
                    text = "MAC: ${device.macAddress}", // MAC地址
                    style = MaterialTheme.typography.bodySmall, // 设置文本样式
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // 设置透明度
                )
                // 显示设备IP地址
                Text(
                    text = "IP: ${device.ip}", // IP地址
                    style = MaterialTheme.typography.bodySmall, // 设置文本样式
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // 设置透明度
                )
            }
        }
    }
}
