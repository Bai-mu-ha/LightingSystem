package com.example.myapp.data

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName
import androidx.core.graphics.toColorInt

/**
 * 设备数据类，表示智能设备的信息和状态
 *
 * @property macAddress 设备MAC地址
 * @property name 设备名称
 * @property status 设备状态
 * @property ip 设备IP地址
 * @property currentMode 当前模式
 * @property isOnline 是否在线
 * @property modeParams 模式参数
 *
 * @author Your Name
 * @since 1.0.0
 */
data class Device(
    @SerializedName("deviceId") // 匹配服务器字段
    val macAddress: String,
    val name: String,
    val status: String,
    val ip: String = "",
    @SerializedName("mode")
    val currentMode: String = "solid",
    @SerializedName("isOnline")
    val isOnline: Boolean = false,
    @SerializedName("modeParams")
    val modeParams: Map<String, Any> = emptyMap()
) {
    /**
     * 获取设备当前的颜色参数
     *
     * @return 当前颜色，如果未设置则返回白色
     */
    fun getColor(): Color {
        return (modeParams["color"] as? String)?.let {
            Color(it.toColorInt())
        } ?: Color.White
    }

    /**
     * 获取设备的第一种颜色参数
     *
     * @return 第一种颜色，如果未设置则返回黄色
     */
    fun getColor1(): Color {
        return (modeParams["color1"] as? String)?.let {
            Color(it.toColorInt())
        } ?: Color.Yellow
    }

    /**
     * 获取设备的第二种颜色参数
     *
     * @return 第二种颜色，如果未设置则返回红色
     */
    fun getColor2(): Color {
        return (modeParams["color2"] as? String)?.let {
            Color(it.toColorInt())
        } ?: Color.Red
    }

    /**
     * 获取设备的亮度参数
     *
     * @return 亮度值，范围0.0-1.0，默认为1.0
     */
    fun getBrightness(): Float {
        return (modeParams["brightness"] as? Number)?.toFloat() ?: 1.0f
    }

    /**
     * 创建一个新的设备实例，更新模式和参数
     *
     * @param mode 新的模式
     * @param params 新的模式参数
     * @return 更新后的设备实例
     */
    fun copyWithMode(mode: String, params: Map<String, Any>): Device {
        return this.copy(
            currentMode = mode,
            modeParams = params
        )
    }

    /**
     * 灯光模式枚举，定义了设备支持的各种灯光效果
     *
     * @property modeName 模式名称
     * @property hasParams 是否有参数
     */
    enum class LightMode(
        val modeName: String,
        val hasParams: Boolean
    ) {
        /** 常亮模式 */
        SOLID("常亮", false),

        /** 彩虹跑马灯模式 */
        RAINBOW("彩虹跑马灯", true),

        /** 星星闪烁模式 */
        TWINKLE("星星闪烁", true),

        /** 波动灯模式 */
        WAVE("波动灯", true),

        /** 彩虹流动模式 */
        FLOWING_RAINBOW("彩虹流动", true),

        /** 彩虹呼吸流动模式 */
        BREATHING_RAINBOW("彩虹呼吸流动", true),

        /** 水波纹模式 */
        RIPPLE("水波纹", true)
    }
}
