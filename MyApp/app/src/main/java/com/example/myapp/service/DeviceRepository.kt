package com.example.myapp.service

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.myapp.data.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.net.SocketTimeoutException

/**
 * 设备数据仓库类，负责处理设备相关的数据操作
 *
 * 管理设备列表、设备绑定、设备控制等核心功能
 *
 * @param apiService API服务接口，用于与后端通信
 * @param userService 用户服务，用于访问用户相关功能
 *
 * @author Your Name
 * @since 1.0.0
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val apiService: ApiService,
    private val userService: UserService
) {
    companion object {
        private const val TAG = "DeviceRepository"
    }

    /**
     * 内存缓存
     * 存储当前用户绑定的设备列表
     */
    private val _boundDevices = MutableStateFlow<List<Device>>(emptyList())

    /**
     * 对外暴露的绑定设备状态流
     */
    val boundDevices: StateFlow<List<Device>> = _boundDevices

    /**
     * 从服务器加载并缓存设备列表
     *
     * 获取当前用户绑定的所有设备信息
     *
     * @param maxRetry 最大重试次数
     * @return 加载是否成功
     */
    // 从服务器加载并缓存
    suspend fun loadDevices(maxRetry: Int = 2): Boolean {
        var retryCount = 0
        while (retryCount <= maxRetry) {
            try {
                val userId = userService.getCurrentUserId() ?: return false
                val token = userService.getToken() ?: return false

                Log.d(TAG, "DEBUG:开始从服务器加载设备列表(尝试${retryCount + 1})")
                // 设置15秒超时获取设备列表
                val response = withTimeoutOrNull(15_000) { // 15秒超时
                    apiService.getBoundDevices("Bearer $token", userId)
                } ?: throw SocketTimeoutException("请求超时")

                if (!response.isSuccessful) {
                    throw Exception("API请求失败: ${response.code()}")
                }

                // 将服务器响应转换为本地设备对象并更新缓存
                _boundDevices.value = response.body()?.data?.map { serverDevice ->
                    Device(
                        macAddress = serverDevice.deviceId,
                        name = serverDevice.name ?: "未命名设备",
                        status = if (serverDevice.isOnline) "online" else "offline"
                    )
                } ?: emptyList()

                Log.d(TAG, "DEBUG:设备列表加载完成，数量: ${_boundDevices.value.size}")
                return true
            } catch (e: Exception) {
                retryCount++
                Log.e(TAG, "加载设备失败(尝试$retryCount)", e)
                if (retryCount > maxRetry) {
                    return false
                }
                delay(2000L * retryCount) // 指数退避
            }
        }
        return false
    }

    /**
     * 绑定新设备
     *
     * 将发现的设备绑定到当前用户账户
     *
     * @param device 要绑定的设备对象
     * @return 绑定是否成功
     * @throws Exception 操作失败时抛出异常
     */
    //绑定新设备（先API调用，成功后更新本地）
    suspend fun bindDevice(device: Device): Boolean {
        val userId = userService.getCurrentUserId() ?: return false
        val token = userService.getToken() ?: return false

        // 1. 检查设备是否已在服务器注册
        val checkResponse = apiService.checkDeviceRegistered(
            CheckDeviceRequest(device.macAddress)
        ).body()

        if (checkResponse?.data?.isRegistered != true) {
            throw Exception("设备未在服务器注册")
        }

        // 2. 绑定设备到用户
        val response = apiService.bindDevice(
            "Bearer $token",
            BindDeviceRequest(userId, device.macAddress)
        )

        if (response.isSuccessful) {
            val body = response.body()
            when {
                body == null -> {
                    println("🔴 绑定响应体为空")
                    return false
                }
                body.message?.contains("已绑定") == true -> {
                    println("🟡 设备已绑定: ${body.message}")
                    return false // 特殊返回false表示已绑定
                }
                else -> {
                    // 3. 更新本地缓存
                    _boundDevices.value = _boundDevices.value + device
                    return true
                }
            }
        }
        return false
    }

    /**
     * 添加设备到缓存
     *
     * 将设备对象添加到本地缓存列表
     *
     * @param device 要添加的设备对象
     */
    // 添加设备到缓存
    fun addDevice(device: Device) {
        _boundDevices.value = _boundDevices.value + device
    }

    /**
     * 获取指定MAC地址的设备
     *
     * 从本地缓存中查找指定MAC地址的设备
     *
     * @param mac 设备MAC地址
     * @return 找到的设备对象，未找到则返回null
     */
    //获取指定MAC地址的设备
    fun getDeviceByMac(mac: String): Device? {
        return _boundDevices.value.firstOrNull { it.macAddress == mac }
    }

    /**
     * 更新设备模式
     *
     * 更新指定设备的工作模式和相关参数
     *
     * @param macAddress 设备MAC地址
     * @param mode 要设置的模式
     * @param params 模式参数
     * @throws Exception 操作失败时抛出异常
     */
    suspend fun updateDeviceMode(
        macAddress: String,
        mode: String,
        params: Map<String, Any> = emptyMap()
    ) {
        try {
            // 只上传当前模式需要的参数
            val filteredParams = when (mode) {
                "solid" -> params.filterKeys { it == "color" || it == "brightness" }
                "rainbow" -> params.filterKeys { it == "speed" || it == "tail_length" }
                "twinkle" -> params.filterKeys { it in listOf("color1", "color2", "frequency", "speed", "cycle_time") }
                "wave" -> params.filterKeys { it in listOf("color", "wave_speed", "wave_density", "brightness") }
                "flowing_rainbow" -> params.filterKeys { it in listOf("speed", "tail_length", "brightness") }
                "breathing_rainbow" -> params.filterKeys { it in listOf("cycle_duration", "steps", "spin_speed") }
                "ripple" -> params.filterKeys { it in listOf("color", "speed", "wave_length") }
                else -> params
            }

            // 调用API更新设备模式
            val response = apiService.updateDeviceMode(
                deviceId = macAddress,
                request = ModeRequest(mode, filteredParams)
            )

            // 更新本地缓存
            _boundDevices.value = _boundDevices.value.map {
                if (it.macAddress == macAddress) {
                    it.copy(currentMode = mode, modeParams = filteredParams)
                } else {
                    it
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新设备模式失败", e)
            throw e
        }
    }

    /**
     * 更新设备模式参数
     *
     * 更新指定设备的模式参数
     *
     * @param macAddress 设备MAC地址
     * @param params 要更新的参数
     * @throws Exception 操作失败时抛出异常
     */
    suspend fun updateDeviceModeParams(macAddress: String, params: Map<String, Any>) {
        try {
            // 1. 获取当前设备
            val device = getDeviceByMac(macAddress) ?: throw Exception("设备不存在")

            // 2. 合并参数
            val currentParams = device.modeParams.toMutableMap()
            params.forEach { (key, value) ->
                currentParams[key] = value
            }

            // 3. 更新服务器
            apiService.updateDeviceMode(
                deviceId = macAddress,
                request = ModeRequest(device.currentMode, currentParams)
            )

            // 4. 更新本地缓存
            _boundDevices.value = _boundDevices.value.map {
                if (it.macAddress == macAddress) {
                    it.copy(modeParams = currentParams)
                } else {
                    it
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新设备参数失败", e)
            throw e
        }
    }
}
