package com.example.myapp.feature.model

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.Device
import com.example.myapp.service.DeviceRepository
import com.example.myapp.service.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import javax.inject.Inject

/**
 * 设备ViewModel类，负责处理设备相关的业务逻辑
 *
 * 管理设备发现、绑定、控制等操作，以及维护设备状态
 *
 * @param repository 设备数据仓库，用于访问设备相关数据
 * @param userService 用户服务，用于访问用户相关功能
 * @param context 应用上下文
 *
 * @author Your Name
 * @since 1.0.0
 */
@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val userService: UserService,
    private val context: Context
) : ViewModel() {
    /**
     * 绑定设备的状态流，包含当前用户绑定的所有设备列表
     */
    val boundDevices: StateFlow<List<Device>> = repository.boundDevices

    /**
     * 设备发现相关状态
     * 存储通过局域网发现的设备列表
     */
    private val _devices = MutableStateFlow<List<Device>>(emptyList())

    /**
     * 对外暴露的设备列表状态流
     */
    val devices: StateFlow<List<Device>> = _devices

    /**
     * 设备发现状态的私有可变状态流
     */
    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)

    /**
     * 对外暴露的设备发现状态流
     */
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState

    /**
     * 设备绑定状态的私有可变状态流
     */
    private val _bindingState = MutableStateFlow<BindingState>(BindingState.Idle)

    /**
     * 对外暴露的设备绑定状态流
     */
    val bindingState: StateFlow<BindingState> = _bindingState

    /**
     * 设备更新状态的私有可变状态流
     */
    private val _deviceUpdates = MutableStateFlow<Device?>(null)

    /**
     * 对外暴露的设备更新状态流
     */
    val deviceUpdates: StateFlow<Device?> = _deviceUpdates

    /**
     * 执行本地网络扫描，发现局域网中的设备
     *
     * @return 发现的设备列表
     */
    private suspend fun performLocalNetworkScan(): List<Device> {
        return try {
            // 调用UDP发现方法查找设备
            discoverDevicesViaUDP()
        } catch (e: Exception) {
            // 扫描失败时打印错误信息并返回空列表
            println("Local network scan failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * 通过UDP协议发现局域网设备
     *
     * 使用组播方式发送设备发现请求，并接收设备响应
     *
     * @return 发现的设备列表
     */
    // 在DeviceViewModel中添加
    private suspend fun discoverDevicesViaUDP(): List<Device> {
        println("DEBUG: [1] 开始UDP发现流程") // 添加此行
        return withContext(Dispatchers.IO) {
            try {
                // 创建组播套接字监听8888端口
                MulticastSocket(8888).use { socket ->
                    // 加入组播组224.0.0.114
                    socket.joinGroup(InetAddress.getByName("224.0.0.114"))

                    // 发送组播请求包，请求设备发现
                    socket.send(DatagramPacket(
                        "DISCOVER_DEVICES_REQUEST".toByteArray(),
                        "DISCOVER_DEVICES_REQUEST".length,
                        InetAddress.getByName("224.0.0.114"),
                        8888
                    ))

                    // 接收响应（带超时）
                    val devices = mutableListOf<Device>()
                    val buffer = ByteArray(1024)
                    val startTime = System.currentTimeMillis()

                    // 在5秒内持续接收设备响应
                    while (System.currentTimeMillis() - startTime < 5000) {
                        try {
                            socket.soTimeout = 1000 // 设置1秒超时
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet) // 接收数据包

                            val response = String(packet.data, 0, packet.length)
                            // 检查是否为设备发现响应
                            if (response.startsWith("DISCOVER_DEVICES_RESPONSE")) {
                                val parts = response.split("|")
                                // 解析设备信息并添加到列表
                                devices.add(Device(
                                    name = parts.getOrElse(1) { "树莓派" },
                                    macAddress = parts.getOrElse(2) { "" }.uppercase(),
                                    ip = packet.address.hostAddress ?: "",
                                    status = "online"
                                ))
                            }
                        } catch (e: SocketTimeoutException) {
                            // 超时继续循环
                        }
                    }
                    return@withContext devices
                }
            } catch (e: Exception) {
                // 发生异常时返回空列表
                emptyList()
            }
        }
    }

    /**
     * 初始化加载设备（仅在登录后调用）
     *
     * 从服务器加载当前用户绑定的设备列表
     */
    fun loadBoundDevices() {
        viewModelScope.launch {
            try {
                // 调用仓库方法加载设备
                repository.loadDevices()
            } catch (e: Exception) {
                // 简单错误处理
            }
        }
    }

    /**
     * 发现局域网设备
     *
     * 启动设备发现流程，通过UDP组播方式查找局域网中的设备
     */
    fun discoverDevices() {
        viewModelScope.launch {
            _discoveryState.value = DiscoveryState.Loading // 设置发现状态为加载中
            try {
                // 执行本地网络扫描
                val devices = performLocalNetworkScan()
                _devices.value = devices // 更新发现的设备列表
                _discoveryState.value = DiscoveryState.Success // 设置发现状态为成功
            } catch (e: Exception) {
                // 发生异常时设置错误状态
                _discoveryState.value = DiscoveryState.Error("设备发现失败: ${e.message}")
            }
        }
    }

    /**
     * 重置设备发现状态到初始状态
     */
    fun resetDiscoveryState() {
        _discoveryState.value = DiscoveryState.Idle
    }

    /**
     * 绑定设备到当前用户
     *
     * 将发现的设备绑定到当前登录的用户账户
     *
     * @param device 要绑定的设备对象
     */
    fun connectToDevice(device: Device) {
        viewModelScope.launch {
            _bindingState.value = BindingState.Loading // 设置绑定状态为加载中
            try {
                // 调用仓库方法绑定设备
                val success = repository.bindDevice(device)
                if (success) {
                    _bindingState.value = BindingState.Success // 绑定成功
                } else {
                    // 服务器返回200但message为"设备已绑定"
                    _bindingState.value = BindingState.Error("该设备已绑定该账号，请不要再次绑定")
                }
            } catch (e: Exception) {
                // 发生异常时设置错误状态
                _bindingState.value = BindingState.Error(e.message ?: "绑定错误")
            }
        }
    }

    /**
     * 根据MAC地址获取设备
     *
     * 从本地缓存中查找指定MAC地址的设备
     *
     * @param mac 设备MAC地址
     * @return 找到的设备对象，未找到则返回null
     */
    //根据MAC地址获取设备
    fun getDeviceByMac(mac: String): Device? {
        return repository.getDeviceByMac(mac)
    }

    /**
     * 重置绑定状态到初始状态
     */
    //重置绑定状态到初始状态
    fun resetBindingState() {
        _bindingState.value = BindingState.Idle
    }

    /**
     * 获取指定模式的默认参数
     *
     * 根据设备模式返回对应的默认参数配置
     *
     * @param mode 设备模式
     * @return 该模式的默认参数映射
     */
    fun getDefaultParamsForMode(mode: String): Map<String, Any> {
        return when (mode) {
            "solid" -> mapOf(
                "color" to "#FFFFFF",
                "brightness" to 1f
            )
            "rainbow" -> mapOf(
                "speed" to 0.5f,
                "tail_length" to 0.2f
            )
            "twinkle" -> mapOf(
                "color1" to Color.Yellow,
                "color2" to Color.Red,
                "frequency" to 0.5f,
                "speed" to 0.5f,
                "cycle_time" to 2f
            )
            // 其他模式的默认参数...
            else -> emptyMap()
        }
    }

    /**
     * 更改设备模式
     *
     * 更新指定设备的工作模式和相关参数
     *
     * @param macAddress 设备MAC地址
     * @param mode 要设置的模式
     * @param params 模式参数
     */
    fun changeMode(macAddress: String, mode: String, params: Map<String, Any> = emptyMap()) {
        viewModelScope.launch {
            try {
                // 处理颜色参数，确保转换为16进制字符串
                val processedParams = params.mapValues { (key, value) ->
                    when {
                        (key == "color1" || key == "color2" || key == "color") && value is Color -> {
                            // 将Color转换为16进制字符串
                            String.format("#%06X", 0xFFFFFF and value.toArgb())
                        }
                        value is Float -> {
                            // 保留两位小数
                            "%.2f".format(value).toFloat()
                        }
                        else -> value
                    }
                }

                // 只上传当前模式需要的参数
                val filteredParams = when (mode) {
                    "solid" -> processedParams.filterKeys { it == "color" || it == "brightness" }
                    "rainbow" -> processedParams.filterKeys { it == "speed" || it == "tail_length" }
                    "twinkle" -> processedParams.filterKeys { it in listOf("color1", "color2", "frequency", "speed", "cycle_time") }
                    "wave" -> processedParams.filterKeys { it in listOf("color", "wave_speed", "wave_density", "brightness") }
                    "flowing_rainbow" -> processedParams.filterKeys { it in listOf("speed", "tail_length", "brightness") }
                    "breathing_rainbow" -> processedParams.filterKeys { it in listOf("cycle_duration", "steps", "spin_speed") }
                    "ripple" -> processedParams.filterKeys { it in listOf("color", "speed", "wave_length") }
                    else -> processedParams
                }

                // 1. 更新服务器
                repository.updateDeviceMode(macAddress, mode, filteredParams)

                // 2. 更新本地状态
                repository.getDeviceByMac(macAddress)?.let { device ->
                    _deviceUpdates.value = device.copyWithMode(mode, filteredParams)
                }
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }

    /**
     * 设备发现状态密封类
     *
     * 定义设备发现过程中的各种状态
     */
    sealed class DiscoveryState {
        /** 空闲状态 */
        object Idle : DiscoveryState()

        /** 加载中状态 */
        object Loading : DiscoveryState()

        /** 成功状态 */
        object Success : DiscoveryState()

        /** 错误状态，包含错误信息 */
        data class Error(val message: String) : DiscoveryState()
    }

    /**
     * 设备绑定状态密封类
     *
     * 定义设备绑定过程中的各种状态
     */
    sealed class BindingState {
        /** 空闲状态 */
        object Idle : BindingState()

        /** 加载中状态 */
        object Loading : BindingState()

        /** 成功状态 */
        object Success : BindingState()

        /** 错误状态，包含错误信息 */
        data class Error(val message: String) : BindingState()
    }
}
