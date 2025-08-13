package com.example.myapp.feature.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapp.R
import com.example.myapp.data.Device
import com.example.myapp.feature.model.DeviceViewModel
import androidx.core.graphics.toColorInt

/**
 * 设备控制界面组件
 *
 * 提供对智能设备的详细控制功能，包括模式选择、参数调节等
 *
 * @param macAddress 设备MAC地址，用于标识特定设备
 * @param viewModel 设备ViewModel，处理设备控制逻辑
 * @param onBack 返回按钮点击回调
 *
 * @author Your Name
 * @since 1.0.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlScreen(
    macAddress: String, // 改为传递MAC地址
    viewModel: DeviceViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    // 收集设备更新状态，实时监听设备状态变化
    val device by viewModel.deviceUpdates.collectAsState()

    // 获取当前设备信息，优先使用更新状态中的设备，否则从ViewModel中获取
    val currentDevice = device ?: viewModel.getDeviceByMac(macAddress) ?: run {
        // 如果设备数据加载失败，显示错误信息
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("设备数据加载失败", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    // 当前选择的模式状态变量，初始化为设备当前模式
    var selectedMode by remember { mutableStateOf(currentDevice.currentMode) }

    // 当前模式参数状态变量，初始化为设备当前参数
    var currentParams by remember { mutableStateOf(currentDevice.modeParams) }

    // 当模式切换时重置参数的副作用函数
    LaunchedEffect(selectedMode) {
        // 当选择的模式发生变化时，获取该模式的默认参数
        currentParams = viewModel.getDefaultParamsForMode(selectedMode)
    }

    // 使用Scaffold构建页面结构，包含顶部应用栏
    Scaffold(
        topBar = {
            // 创建居中对齐的顶部应用栏
            CenterAlignedTopAppBar(
                title = { Text("设备控制") }, // 设置应用栏标题
                navigationIcon = {
                    // 创建导航图标按钮
                    IconButton(onClick = onBack) {
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
        // 创建垂直排列的Column容器，用于放置设备控制界面的各个组件
        Column(
            modifier = Modifier
                .padding(innerPadding) // 应用内部padding
                .padding(16.dp) // 设置外边距
                .verticalScroll(rememberScrollState()), // 添加垂直滚动支持
            verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
        ) {
            // 设备信息卡片组件
            DeviceInfoCard(currentDevice)

            // 添加垂直间距
            Spacer(modifier = Modifier.height(24.dp))

            // 模式选择器标题
            Text("模式: ", style = MaterialTheme.typography.titleMedium)

            // 模式选择器组件
            ModeSelector(
                currentMode = selectedMode, // 传递当前选择的模式
                onModeSelected = { mode ->
                    selectedMode = mode // 更新选择的模式
                    // viewModel.changeMode(currentDevice.macAddress, mode, currentDevice.modeParams)
                }
            )

            // 添加垂直间距
            Spacer(modifier = Modifier.height(24.dp))

            // 动态显示当前模式的控制面板
            // 根据选择的模式显示对应的控制面板
            when (selectedMode) {
                // 常亮模式控制面板
                "solid" -> SolidControlPanel(
                    device = currentDevice, // 传递当前设备信息
                    params = currentParams, // 传递当前参数
                    onParamsChange = { newParams ->
                        currentParams = newParams // 更新参数
                    },
                    onApply = { color, brightness ->
                        // 应用设置到设备
                        viewModel.changeMode(
                            currentDevice.macAddress,
                            selectedMode,
                            mapOf("color" to color, "brightness" to brightness)
                        )
                    }
                )

                // 彩虹跑马灯模式控制面板
                "rainbow" -> RainbowControlPanel(
                    params = currentParams, // 传递当前参数
                    onParamsChange = { newParams ->
                        currentParams = newParams // 更新参数
                    },
                    onApply = { params ->
                        // 应用设置到设备
                        viewModel.changeMode(currentDevice.macAddress, selectedMode, params)
                    }
                )

                // 星星闪烁模式控制面板
                "twinkle" -> TwinkleControlPanel(
                    params = currentParams, // 传递当前参数
                    onParamsChange = { newParams ->
                        currentParams = newParams // 更新参数
                    },
                    onApply = { params ->
                        // 应用设置到设备
                        viewModel.changeMode(currentDevice.macAddress, selectedMode, params)
                    }
                )

                // 波动灯模式控制面板
                "wave" -> WaveControlPanel(
                    params = currentParams, // 传递当前参数
                    onParamsChange = { newParams ->
                        currentParams = newParams // 更新参数
                    },
                    onApply = { params ->
                        // 应用设置到设备
                        viewModel.changeMode(currentDevice.macAddress, selectedMode, params)
                    }
                )

                // 彩虹流动模式控制面板
                "flowing_rainbow" -> FlowingRainbowControlPanel(
                    params = currentParams, // 传递当前参数
                    onParamsChange = { newParams ->
                        currentParams = newParams // 更新参数
                    },
                    onApply = { params ->
                        // 应用设置到设备
                        viewModel.changeMode(currentDevice.macAddress, selectedMode, params)
                    }
                )

                // 彩虹呼吸流动模式控制面板
                "breathing_rainbow" -> BreathingRainbowControlPanel(
                    params = currentParams, // 传递当前参数
                    onParamsChange = { newParams ->
                        currentParams = newParams // 更新参数
                    },
                    onApply = { params ->
                        // 应用设置到设备
                        viewModel.changeMode(currentDevice.macAddress, selectedMode, params)
                    }
                )

                // 水波纹模式控制面板
                "ripple" -> RippleControlPanel(
                    params = currentParams, // 传递当前参数
                    onParamsChange = { newParams ->
                        currentParams = newParams // 更新参数
                    },
                    onApply = { params ->
                        // 应用设置到设备
                        viewModel.changeMode(currentDevice.macAddress, selectedMode, params)
                    }
                )
            }
        }
    }
}

/**
 * 设备信息卡片组件
 *
 * 显示设备的基本信息，包括名称、MAC地址、IP地址和在线状态
 *
 * @param device 设备对象，包含设备信息
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun DeviceInfoCard(device: Device) {
    // 创建卡片组件显示设备信息
    Card(
        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
        shape = MaterialTheme.shapes.medium // 设置卡片形状
    ) {
        // 创建水平排列的Row容器
        Row(
            modifier = Modifier.padding(16.dp), // 设置内边距
            verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
        ) {
            // 显示设备图片
            Image(
                painter = painterResource(R.drawable.light), // 从资源文件加载图片
                contentDescription = "设备图片", // 图片描述
                modifier = Modifier
                    .width(128.dp) // 设置宽度
                    .height(64.dp) // 设置高度
                    .clip(CircleShape) // 裁剪为圆形
            )

            // 添加水平间距
            Spacer(modifier = Modifier.width(16.dp))

            // 创建垂直排列的Column容器显示设备文本信息
            Column {
                // 显示设备名称
                Text(device.name, style = MaterialTheme.typography.titleMedium)

                // 显示设备MAC地址
                Text("MAC: ${device.macAddress}", style = MaterialTheme.typography.bodySmall)

                // 显示设备IP地址
                Text("IP: ${device.ip}", style = MaterialTheme.typography.bodySmall)

                // 创建水平排列的Row容器显示设备状态
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 显示状态指示点
                    Box(
                        modifier = Modifier
                            .size(10.dp) // 设置大小
                            .background(
                                color = when (device.status) {
                                    "online" -> Color.Green // 在线状态显示绿色
                                    else -> Color.Red // 离线状态显示红色
                                },
                                shape = CircleShape // 圆形形状
                            )
                    )

                    // 添加水平间距
                    Spacer(modifier = Modifier.width(8.dp))

                    // 显示状态文本
                    Text(
                        "状态: ${if (device.status == "online") "在线" else "离线"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * 模式选择器组件
 *
 * 提供设备支持的各种模式选择功能
 *
 * @param currentMode 当前选择的模式
 * @param onModeSelected 模式选择回调函数
 * @param modifier 修饰符
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun ModeSelector(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 定义支持的模式列表
    val modes = listOf(
        "solid" to "常亮",
        "rainbow" to "彩虹跑马",
        "twinkle" to "星星闪烁",
        "wave" to "波动",
        "flowing_rainbow" to "彩虹流动",
        "breathing_rainbow" to "彩虹呼吸",
        "ripple" to "水波纹"
    )

    // 创建水平滚动的LazyRow容器
    LazyRow(
        modifier = modifier.fillMaxWidth(), // 填充最大宽度
        horizontalArrangement = Arrangement.spacedBy(8.dp) // 设置水平间距
    ) {
        // 遍历模式列表创建选择项
        items(modes) { (mode, name) ->
            // 创建过滤芯片组件作为模式选择项
            FilterChip(
                selected = currentMode == mode, // 根据当前模式设置选中状态
                onClick = { onModeSelected(mode) }, // 点击时调用选择回调
                label = { Text(name) }, // 显示模式名称
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary, // 选中时的背景色
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary // 选中时的文字颜色
                )
            )
        }
    }
}

/**
 * 常亮模式控制面板组件
 *
 * 提供常亮模式的颜色和亮度调节功能
 *
 * @param device 设备对象
 * @param params 当前模式参数
 * @param onParamsChange 参数变化回调函数
 * @param onApply 应用设置回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun SolidControlPanel(
    device: Device,
    params: Map<String, Any>,
    onParamsChange: (Map<String, Any>) -> Unit,
    onApply: (Color, Float) -> Unit
) {
    // 定义选中颜色状态变量，初始化为参数中的颜色或设备当前颜色
    var selectedColor by remember {
        mutableStateOf(
            (params["color"] as? String)?.let { Color(it.toColorInt()) }
                ?: device.getColor()
        )
    }

    // 定义亮度状态变量，初始化为参数中的亮度或设备当前亮度
    var brightness by remember {
        mutableStateOf(params["brightness"] as? Float ?: device.getBrightness())
    }

    // 创建垂直排列的Column容器
    Column(
        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
        verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
    ) {
        // 显示面板标题
        Text("常亮模式设置", style = MaterialTheme.typography.titleMedium)

        // 简化颜色选择器组件
        SimpleColorPicker(
            selectedColor = selectedColor, // 传递选中的颜色
            onColorSelected = { selectedColor = it } // 颜色选择回调
        )

        // 亮度调节滑块
        Text("亮度: ${"%.0f".format(brightness * 100)}%") // 显示当前亮度百分比
        Slider(
            value = brightness, // 绑定亮度值
            onValueChange = { brightness = it }, // 滑块值变化时更新亮度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 应用按钮组件
        Button(
            onClick = {
                // 将颜色转换为十六进制字符串
                val colorHex = String.format("#%06X", 0xFFFFFF and selectedColor.toArgb())
                // 格式化亮度值
                val brightnessRounded = "%.2f".format(brightness).toFloat()

                // 调用参数变化回调
                onParamsChange(mapOf(
                    "color" to colorHex,
                    "brightness" to brightnessRounded
                ))
                // 调用应用设置回调
                onApply(selectedColor, brightnessRounded)
            },
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        ) {
            Text("应用设置") // 按钮文本
        }
    }
}

/**
 * 彩虹跑马灯控制面板组件
 *
 * 提供彩虹跑马灯模式的速度和拖尾长度调节功能
 *
 * @param params 当前模式参数
 * @param onParamsChange 参数变化回调函数
 * @param onApply 应用设置回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun RainbowControlPanel(
    params: Map<String, Any>,
    onParamsChange: (Map<String, Any>) -> Unit,
    onApply: (Map<String, Any>) -> Unit
) {
    // 定义速度状态变量，初始化为参数中的速度或默认值0.5f
    var speed by remember { mutableStateOf(params["speed"] as? Float ?: 0.5f) }

    // 定义拖尾长度状态变量，初始化为参数中的拖尾长度或默认值0.2f
    var tailLength by remember { mutableStateOf(params["tail_length"] as? Float ?: 0.2f) }
    //var tailLength by remember { mutableStateOf(params["tail_length"] as? Float ?: 0.7f) }

    // 创建垂直排列的Column容器
    Column(
        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
        verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
    ) {
        // 显示面板标题
        Text("彩虹跑马灯设置", style = MaterialTheme.typography.titleMedium)

        // 速度调节滑块
        Text("速度: ${"%.1f".format(speed)}") // 显示当前速度值
        Slider(
            value = speed, // 绑定速度值
            onValueChange = { speed = it }, // 滑块值变化时更新速度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 拖尾长度调节滑块
        Text("拖尾长度: $tailLength") // 显示当前拖尾长度值
        Slider(
            value = tailLength.toFloat(), // 绑定拖尾长度值
            onValueChange = { tailLength = it }, // 滑块值变化时更新拖尾长度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 应用按钮组件
        Button(
            onClick = {
                // 创建新的参数映射
                val newParams = mapOf(
                    "speed" to speed,
                    "tail_length" to tailLength
                )
                // 调用参数变化回调
                onParamsChange(newParams)
                // 调用应用设置回调
                onApply(newParams)
            },
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        ) {
            Text("应用设置") // 按钮文本
        }
    }
}

/**
 * 星星闪烁灯控制面板组件
 *
 * 提供星星闪烁灯模式的颜色、频率、速度和周期调节功能
 *
 * @param params 当前模式参数
 * @param onParamsChange 参数变化回调函数
 * @param onApply 应用设置回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun TwinkleControlPanel(
    params: Map<String, Any>,
    onParamsChange: (Map<String, Any>) -> Unit,
    onApply: (Map<String, Any>) -> Unit
) {
    // 定义颜色1状态变量，初始化为参数中的颜色1或默认黄色
    var color1 by remember {
        mutableStateOf(
            (params["color1"] as? String)?.let { Color(it.toColorInt()) }
                ?: Color.Yellow
        )
    }

    // 定义颜色2状态变量，初始化为参数中的颜色2或默认红色
    var color2 by remember {
        mutableStateOf(
            (params["color2"] as? String)?.let { Color(it.toColorInt()) }
                ?: Color.Red
        )
    }

    // 定义频率状态变量，初始化为参数中的频率或默认值0.5f
    var frequency by remember { mutableStateOf(params["frequency"] as? Float ?: 0.5f) }

    // 定义速度状态变量，初始化为参数中的速度或默认值0.5f
    var speed by remember { mutableStateOf(params["speed"] as? Float ?: 0.5f) }

    // 定义周期时间状态变量，初始化为参数中的周期时间或默认值2f
    var cycleTime by remember { mutableStateOf(params["cycle_time"] as? Float ?: 2f) }

    // 创建垂直排列的Column容器
    Column(
        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
        verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
    ) {
        // 显示面板标题
        Text("星星闪烁灯设置", style = MaterialTheme.typography.titleMedium)

        // 颜色1选择器组件
        SimpleColorPicker(
            selectedColor = color1, // 传递选中的颜色1
            onColorSelected = { color1 = it } // 颜色选择回调
        )

        // 添加垂直间距
        Spacer(modifier = Modifier.height(16.dp))

        // 颜色2选择器组件
        SimpleColorPicker(
            selectedColor = color2, // 传递选中的颜色2
            onColorSelected = { color2 = it } // 颜色选择回调
        )

        // 添加垂直间距
        Spacer(modifier = Modifier.height(16.dp))

        // 闪烁频率调节滑块
        Text("闪烁频率: ${"%.1f".format(frequency)}") // 显示当前频率值
        Slider(
            value = frequency, // 绑定频率值
            onValueChange = { frequency = it }, // 滑块值变化时更新频率
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 闪烁速度调节滑块
        Text("闪烁速度: ${"%.1f".format(speed)}") // 显示当前速度值
        Slider(
            value = speed, // 绑定速度值
            onValueChange = { speed = it }, // 滑块值变化时更新速度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 渐变周期调节滑块
        Text("渐变周期: ${"%.1f".format(cycleTime)}秒") // 显示当前周期时间值
        Slider(
            value = cycleTime, // 绑定周期时间值
            onValueChange = { cycleTime = it }, // 滑块值变化时更新周期时间
            valueRange = 0f..5f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 应用按钮组件
        Button(
            onClick = {
                // 创建新的参数映射
                val newParams = mapOf(
                    "color1" to String.format("#%06X", 0xFFFFFF and color1.toArgb()), // 格式化颜色1
                    "color2" to String.format("#%06X", 0xFFFFFF and color2.toArgb()), // 格式化颜色2
                    "frequency" to "%.2f".format(frequency).toFloat(), // 格式化频率
                    "speed" to "%.2f".format(speed).toFloat(), // 格式化速度
                    "cycle_time" to "%.2f".format(cycleTime).toFloat() // 格式化周期时间
                )
                // 调用参数变化回调
                onParamsChange(newParams)
                // 调用应用设置回调
                onApply(newParams)
            },
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        ) {
            Text("应用设置") // 按钮文本
        }
    }
}

/**
 * 波动灯控制面板组件
 *
 * 提供波动灯模式的颜色、波动速度、波形密度和亮度调节功能
 *
 * @param params 当前模式参数
 * @param onParamsChange 参数变化回调函数
 * @param onApply 应用设置回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun WaveControlPanel(
    params: Map<String, Any>,
    onParamsChange: (Map<String, Any>) -> Unit,
    onApply: (Map<String, Any>) -> Unit
) {
    // 定义颜色状态变量，初始化为参数中的颜色或默认蓝色
    var color by remember {
        mutableStateOf(
            (params["color"] as? String)?.let { Color(it.toColorInt()) }
                ?: Color.Blue
        )
    }

    // 定义波动速度状态变量，初始化为参数中的波动速度或默认值0.5f
    var waveSpeed by remember { mutableStateOf(params["wave_speed"] as? Float ?: 0.5f) }

    // 定义波形密度状态变量，初始化为参数中的波形密度或默认值3
    var waveDensity by remember { mutableStateOf(params["wave_density"] as? Int ?: 3) }

    // 定义亮度状态变量，初始化为参数中的亮度或默认值1f
    var brightness by remember { mutableStateOf(params["brightness"] as? Float ?: 1f) }

    // 创建垂直排列的Column容器
    Column(
        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
        verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
    ) {
        // 显示面板标题
        Text("波动灯设置", style = MaterialTheme.typography.titleMedium)

        // 颜色选择器组件
        SimpleColorPicker(
            selectedColor = color, // 传递选中的颜色
            onColorSelected = { color = it } // 颜色选择回调
        )

        // 波动速度调节滑块
        Text("波动速度: ${"%.1f".format(waveSpeed)}") // 显示当前波动速度值
        Slider(
            value = waveSpeed, // 绑定波动速度值
            onValueChange = { waveSpeed = it }, // 滑块值变化时更新波动速度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 波形密度调节滑块
        Text("波形密度: $waveDensity") // 显示当前波形密度值
        Slider(
            value = waveDensity.toFloat(), // 绑定波形密度值
            onValueChange = { waveDensity = it.toInt() }, // 滑块值变化时更新波形密度
            valueRange = 0f..5f, // 设置值范围
            steps = 4, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 亮度调节滑块
        Text("亮度: ${"%.0f".format(brightness * 100)}%") // 显示当前亮度百分比
        Slider(
            value = brightness, // 绑定亮度值
            onValueChange = { brightness = it }, // 滑块值变化时更新亮度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 应用按钮组件
        Button(
            onClick = {
                // 创建新的参数映射
                val newParams = mapOf(
                    "color" to color, // 颜色参数
                    "wave_speed" to waveSpeed, // 波动速度参数
                    "wave_density" to waveDensity, // 波形密度参数
                    "brightness" to brightness // 亮度参数
                )
                // 调用参数变化回调
                onParamsChange(newParams)
                // 调用应用设置回调
                onApply(newParams)
            },
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        ) {
            Text("应用设置") // 按钮文本
        }
    }
}

/**
 * 彩虹流动灯控制面板组件
 *
 * 提供彩虹流动灯模式的流动速度、拖尾长度和亮度调节功能
 *
 * @param params 当前模式参数
 * @param onParamsChange 参数变化回调函数
 * @param onApply 应用设置回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun FlowingRainbowControlPanel(
    params: Map<String, Any>,
    onParamsChange: (Map<String, Any>) -> Unit,
    onApply: (Map<String, Any>) -> Unit
) {
    // 定义速度状态变量，初始化为参数中的速度或默认值0.5f
    var speed by remember { mutableStateOf(params["speed"] as? Float ?: 0.5f) }

    // 定义拖尾长度状态变量，初始化为参数中的拖尾长度或默认值0.2f
    var tailLength by remember { mutableStateOf(params["tail_length"] as? Float ?: 0.2f) }

    // 定义亮度状态变量，初始化为参数中的亮度或默认值1f
    var brightness by remember { mutableStateOf(params["brightness"] as? Float ?: 1f) }

    // 创建垂直排列的Column容器
    Column(
        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
        verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
    ) {
        // 显示面板标题
        Text("彩虹流动灯设置", style = MaterialTheme.typography.titleMedium)

        // 流动速度调节滑块
        Text("流动速度: ${"%.1f".format(speed)}") // 显示当前流动速度值
        Slider(
            value = speed, // 绑定流动速度值
            onValueChange = { speed = it }, // 滑块值变化时更新流动速度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 拖尾长度调节滑块
        Text("拖尾长度: $tailLength") // 显示当前拖尾长度值
        Slider(
            value = tailLength.toFloat(), // 绑定拖尾长度值
            onValueChange = { tailLength = it }, // 滑块值变化时更新拖尾长度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 亮度调节滑块
        Text("亮度: ${"%.0f".format(brightness * 100)}%") // 显示当前亮度百分比
        Slider(
            value = brightness, // 绑定亮度值
            onValueChange = { brightness = it }, // 滑块值变化时更新亮度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 应用按钮组件
        Button(
            onClick = {
                // 创建新的参数映射
                val newParams = mapOf(
                    "speed" to speed, // 速度参数
                    "tail_length" to tailLength, // 拖尾长度参数
                    "brightness" to brightness // 亮度参数
                )
                // 调用参数变化回调
                onParamsChange(newParams)
                // 调用应用设置回调
                onApply(newParams)
            },
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        ) {
            Text("应用设置") // 按钮文本
        }
    }
}

/**
 * 彩虹呼吸流动灯控制面板组件
 *
 * 提供彩虹呼吸流动灯模式的呼吸周期、动画帧数和旋转速度调节功能
 *
 * @param params 当前模式参数
 * @param onParamsChange 参数变化回调函数
 * @param onApply 应用设置回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun BreathingRainbowControlPanel(
    params: Map<String, Any>,
    onParamsChange: (Map<String, Any>) -> Unit,
    onApply: (Map<String, Any>) -> Unit
) {
    // 定义周期持续时间状态变量，初始化为参数中的周期持续时间或默认值5f
    var cycleDuration by remember { mutableStateOf(params["cycle_duration"] as? Float ?: 5f) }

    // 定义步数状态变量，初始化为参数中的步数或默认值60
    var steps by remember { mutableStateOf(params["steps"] as? Int ?: 60) }

    // 定义旋转速度状态变量，初始化为参数中的旋转速度或默认值2.5f
    var spinSpeed by remember { mutableStateOf(params["spin_speed"] as? Float ?: 2.5f) }

    // 创建垂直排列的Column容器
    Column(
        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
        verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
    ) {
        // 显示面板标题
        Text("彩虹呼吸流动灯设置", style = MaterialTheme.typography.titleMedium)

        // 呼吸周期调节滑块
        Text("呼吸周期: ${"%.1f".format(cycleDuration)}秒") // 显示当前周期持续时间值
        Slider(
            value = cycleDuration, // 绑定周期持续时间值
            onValueChange = { cycleDuration = it }, // 滑块值变化时更新周期持续时间
            valueRange = 0f..10f, // 设置值范围
            steps = 19, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 动画帧数调节滑块
        Text("动画帧数: $steps") // 显示当前步数值
        Slider(
            value = steps.toFloat(), // 绑定步数值
            onValueChange = { steps = it.toInt() }, // 滑块值变化时更新步数
            valueRange = 0f..120f, // 设置值范围
            steps = 11, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 旋转速度调节滑块
        Text("旋转速度: ${"%.1f".format(spinSpeed)}") // 显示当前旋转速度值
        Slider(
            value = spinSpeed, // 绑定旋转速度值
            onValueChange = { spinSpeed = it }, // 滑块值变化时更新旋转速度
            valueRange = 0f..5f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 应用按钮组件
        Button(
            onClick = {
                // 创建新的参数映射
                val newParams = mapOf(
                    "cycle_duration" to cycleDuration, // 周期持续时间参数
                    "steps" to steps, // 步数参数
                    "spin_speed" to spinSpeed // 旋转速度参数
                )
                // 调用参数变化回调
                onParamsChange(newParams)
                // 调用应用设置回调
                onApply(newParams)
            },
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        ) {
            Text("应用设置") // 按钮文本
        }
    }
}

/**
 * 水波纹灯控制面板组件
 *
 * 提供水波纹灯模式的颜色、波动速度和波长调节功能
 *
 * @param params 当前模式参数
 * @param onParamsChange 参数变化回调函数
 * @param onApply 应用设置回调函数
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun RippleControlPanel(
    params: Map<String, Any>,
    onParamsChange: (Map<String, Any>) -> Unit,
    onApply: (Map<String, Any>) -> Unit
) {
    // 定义颜色状态变量，初始化为参数中的颜色或默认青色
    var color by remember {
        mutableStateOf(
            (params["color"] as? String)?.let { Color(it.toColorInt()) }
                ?: Color.Cyan
        )
    }

    // 定义速度状态变量，初始化为参数中的速度或默认值0.5f
    var speed by remember { mutableStateOf(params["speed"] as? Float ?: 0.5f) }

    // 定义波长状态变量，初始化为参数中的波长或默认值5
    var waveLength by remember { mutableStateOf(params["wave_length"] as? Int ?: 5) }

    // 创建垂直排列的Column容器
    Column(
        modifier = Modifier.fillMaxWidth(), // 填充最大宽度
        verticalArrangement = Arrangement.spacedBy(16.dp) // 设置垂直间距
    ) {
        // 显示面板标题
        Text("水波纹灯设置", style = MaterialTheme.typography.titleMedium)

        // 颜色选择器组件
        SimpleColorPicker(
            selectedColor = color, // 传递选中的颜色
            onColorSelected = { color = it } // 颜色选择回调
        )

        // 波动速度调节滑块
        Text("波动速度: ${"%.1f".format(speed)}") // 显示当前波动速度值
        Slider(
            value = speed, // 绑定波动速度值
            onValueChange = { speed = it }, // 滑块值变化时更新波动速度
            valueRange = 0f..1f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 波长调节滑块
        Text("波长: $waveLength") // 显示当前波长值
        Slider(
            value = waveLength.toFloat(), // 绑定波长值
            onValueChange = { waveLength = it.toInt() }, // 滑块值变化时更新波长
            valueRange = 0f..10f, // 设置值范围
            steps = 9, // 设置步数
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        )

        // 应用按钮组件
        Button(
            onClick = {
                // 创建新的参数映射
                val newParams = mapOf(
                    "color" to color, // 颜色参数
                    "speed" to speed, // 速度参数
                    "wave_length" to waveLength // 波长参数
                )
                // 调用参数变化回调
                onParamsChange(newParams)
                // 调用应用设置回调
                onApply(newParams)
            },
            modifier = Modifier.fillMaxWidth() // 填充最大宽度
        ) {
            Text("应用设置") // 按钮文本
        }
    }
}

/**
 * 简化颜色选择器组件
 *
 * 提供一组预定义颜色供用户选择
 *
 * @param selectedColor 当前选中的颜色
 * @param onColorSelected 颜色选择回调函数
 * @param modifier 修饰符
 *
 * @author Your Name
 * @since 1.0.0
 */
@Composable
fun SimpleColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // 定义预定义颜色列表
    val colors = listOf(
        Color.Red, Color.Green, Color.Blue,
        Color.Yellow, Color.Cyan, Color.Magenta,
        Color.White, Color.Black
    )

    // 创建垂直排列的Column容器
    Column(modifier = modifier) {
        // 显示选择器标题
        Text("选择颜色", style = MaterialTheme.typography.labelMedium)

        // 添加垂直间距
        Spacer(modifier = Modifier.height(8.dp))

        // 创建水平滚动的LazyRow容器显示颜色选项
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 遍历颜色列表创建颜色选项
            items(colors) { color ->
                // 创建颜色显示框
                Box(
                    modifier = Modifier
                        .size(40.dp) // 设置大小
                        .clip(CircleShape) // 裁剪为圆形
                        .background(color) // 设置背景色
                        .border(
                            width = if (color == selectedColor) 3.dp else 1.dp, // 根据选中状态设置边框宽度
                            color = if (color == selectedColor) MaterialTheme.colorScheme.primary // 选中时边框颜色
                            else MaterialTheme.colorScheme.outline, // 未选中时边框颜色
                            shape = CircleShape // 圆形边框
                        )
                        .clickable { onColorSelected(color) } // 点击时调用选择回调
                )
            }
        }
    }
}
