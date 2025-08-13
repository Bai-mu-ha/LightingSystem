# -*- coding: utf-8 -*-
import sys
import os
import board
import neopixel
import time
import math
import threading

# --- 自动申请sudo权限 ---
if os.geteuid() != 0:
    print("正在自动请求sudo权限...")
    os.execvp("sudo", ["sudo", sys.executable] + sys.argv)

# LED 数量
NUM_PIXELS = 60

# 初始化灯带
pixels = neopixel.NeoPixel(board.D18, NUM_PIXELS, brightness=1.0, auto_write=False)

def hex_to_rgb(hex_color, brightness=1.0):
    """将 #FFFFFF 转为 (r,g,b) 元组并应用亮度"""
    hex_color = hex_color.lstrip('#')
    r = int(hex_color[0:2], 16) * brightness
    g = int(hex_color[2:4], 16) * brightness
    b = int(hex_color[4:6], 16) * brightness
    return (int(r), int(g), int(b))

def waterRippleLamp(stop_event, color='#FFFFFF', speed=0.5, wave_length=5):
    """水波纹效果
    speed: 0-1 值越大波纹扩散越快
    wave_length: 1-10 波纹波长（值越大波纹越宽）
    """
    rgb_color = hex_to_rgb(color)
    print(f"启动水波纹 颜色:{color} 速度:{speed:.1f} 波长:{wave_length}")

    # 参数转换
    speed_factor = 0.5 * speed  # 控制波纹扩散速度
    wave_factor = 10 / wave_length  # 控制波纹宽度

    center = NUM_PIXELS // 2
    phase = 0

    try:
        while not stop_event.is_set():
            for i in range(NUM_PIXELS):
                distance = abs(i - center)
                # 每个像素根据距中心的距离有一个延迟，相当于“跳动波向外扩展”
                brightness = (math.sin((phase - distance/wave_factor) * math.pi) + 1) / 2
                brightness = max(0, min(1, brightness))  # 钳制在0-1范围

                pixels[i] = tuple(int(c * brightness) for c in rgb_color)

            pixels.show()
            phase += speed_factor
            time.sleep(0.05)

    except KeyboardInterrupt:
        print("\n中断，关闭灯光")
        pixels.fill((0, 0, 0))
        pixels.show()

# 启动模式的线程
def start_light_mode(mode_function, *args):
    stop_event = threading.Event()  # 创建停止事件
    mode_thread = threading.Thread(target=mode_function, args=(stop_event, *args))
    mode_thread.start()
    return stop_event, mode_thread

if __name__ == "__main__":
    stop_event, mode_thread = start_light_mode(waterRippleLamp, color=(255, 100, 255), speed=0.08, wave_length=7)

    # 模拟等待 10 秒后切换模式（你可以根据需求修改时间）
    time.sleep(10)

    # 停止当前模式
    stop_event.set()
    mode_thread.join()  # 等待当前模式线程退出
    print("音符跳动灯效果模式已停止")

    # 切换到另一个模式
    # 例如：start_light_mode(anotherModeFunction)
