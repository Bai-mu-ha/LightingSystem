import sys
import os
import board
import neopixel
import time
import math
import random
import threading

# --- 自动申请sudo权限 ---
if os.geteuid() != 0:
    print("正在自动请求sudo权限...")
    os.execvp("sudo", ["sudo", sys.executable] + sys.argv)

# LED 数量
num_pixels = 60

# 初始化灯带
pixels = neopixel.NeoPixel(board.D18, num_pixels, brightness=0.5, auto_write=False)

def hex_to_rgb(hex_color):
    """将 #FFFFFF 转为 (r,g,b) 元组"""
    hex_color = hex_color.lstrip('#')
    return tuple(int(hex_color[i:i+2], 16) for i in (0, 2, 4))

def interpolate(color1, color2, factor):
    """线性插值函数：颜色之间渐变"""
    return tuple(int(color1[i] + (color2[i] - color1[i]) * factor) for i in range(3))

def starTwinklingLight(stop_event, color1='#FFFFFF', color2='#FFFF00', frequency=0.1, speed=0.1, cycle_time=3):
    """星星闪烁渐变效果
    frequency: 0-1 值越大闪烁越频繁
    speed: 闪烁速度
    cycle_time: 控制颜色渐变周期的秒数
    """
    print(f"启动闪烁模式 颜色1:{color1} 颜色2:{color2} 频率:{frequency:.2f} 速度:{speed:.2f}")

    rgb_color1 = hex_to_rgb(color1)
    rgb_color2 = hex_to_rgb(color2)

    start_time = time.time()  # ➔ 这里补上定义
    try:
        while not stop_event.is_set():
            current_time = time.time() - start_time
            cycle_progress = (current_time % cycle_time) / cycle_time

            for i in range(num_pixels):
                if random.random() < frequency:
                    base_color = interpolate(rgb_color1, rgb_color2, cycle_progress)
                    pixel_offset = math.sin(i/10 + current_time*speed) * 0.5 + 0.5
                    final_color = tuple(int(c * pixel_offset) for c in base_color)
                    pixels[i] = final_color
                else:
                    pixels[i] = (0, 0, 0)

            pixels.show()
            time.sleep(0.05)

    except KeyboardInterrupt:
        print("\n中断，关闭灯光")
    finally:
        # 确保退出时灯带熄灭
        pixels.fill((0, 0, 0))
        pixels.show()

# 启动模式的线程
def start_light_mode(mode_function, *args):
    stop_event = threading.Event()
    mode_thread = threading.Thread(target=mode_function, args=(stop_event, *args))
    mode_thread.start()
    return stop_event, mode_thread

if __name__ == "__main__":
    stop_event, mode_thread = start_light_mode(starTwinklingLight)

    time.sleep(10)  # 等10秒

    stop_event.set()
    mode_thread.join()
    print("渐变闪烁效果模式已停止")
