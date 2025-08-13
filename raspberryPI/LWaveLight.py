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

NUM_PIXELS = 60
pixels = neopixel.NeoPixel(board.D18, NUM_PIXELS, brightness=1.0, auto_write=False)

# 定义颜色
# color = (0, 255, 0)  # 绿色的RGB值

def hex_to_rgb(hex_color, brightness=1.0):
    """将 #FFFFFF 转为 (r,g,b) 元组并应用亮度"""
    hex_color = hex_color.lstrip('#')
    r = int(hex_color[0:2], 16) * brightness
    g = int(hex_color[2:4], 16) * brightness
    b = int(hex_color[4:6], 16) * brightness
    return (int(r), int(g), int(b))

# 炫酷绿色波动效果
def WaveLight(stop_event, color='#FFFFFF', wave_speed=0.5, wave_density=3, brightness=1.0):
    """波动灯光效果
    wave_speed: 0-1 值越大波动越快
    wave_density: 1-5 值越大波峰越密集
    """

    rgb_color = hex_to_rgb(color, brightness)
    print(f"启动波动模式 颜色:{color} 速度:{wave_speed:.1f} 密度:{wave_density} 亮度:{brightness:.1f}")

    # 参数转换
    speed_factor = 0.2 * wave_speed  # 控制波动速度
    density_factor = wave_density * 2  # 控制波峰数量

    t = 0
    try:
        while not stop_event.is_set():  # 检查是否需要停止
            for i in range(NUM_PIXELS):
                # 使用正弦波创建波动效果
                wave = (math.sin((i/density_factor) + t) + 1) / 2
                pixels[i] = tuple(int(c * wave) for c in rgb_color)

            pixels.show()
            t += speed_factor
            time.sleep(0.03)
    except KeyboardInterrupt:
        print("\n退出中，关闭灯光...")
        pixels.fill((0, 0, 0))
        pixels.show()

# 启动模式的线程
def start_light_mode(mode_function):
    stop_event = threading.Event()  # 创建停止事件
    mode_thread = threading.Thread(target=mode_function, args=(stop_event,))
    mode_thread.start()
    return stop_event, mode_thread

if __name__ == "__main__":
    stop_event, mode_thread = start_light_mode(WaveLight)
    
    # 等待 10 秒钟模拟模式切换（你可以修改这个部分进行手动切换）
    time.sleep(10)

    # 停止当前模式
    stop_event.set()
    mode_thread.join()  # 等待当前模式线程退出
    print("模式已停止")
    
    # 你可以在此处切换到其他模式
    # 例如：start_light_mode(anotherModeFunction)
