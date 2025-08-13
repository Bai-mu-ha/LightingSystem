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
color = (0, 255, 0)  # 绿色的RGB值

# 炫酷绿色波动效果
def greenWaveLight(stop_event):
    print("模式一：炫酷绿色波动（Ctrl+C 可退出）")
    t = 0
    try:
        while not stop_event.is_set():  # 检查是否需要停止
            for i in range(NUM_PIXELS):
                brightness = (math.sin((i / 4.0) + t) + 1) / 2
                g = int(brightness * 255)
                pixels[i] = (color[0], g, color[2])
            pixels.show()
            t += 0.15
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
    stop_event, mode_thread = start_light_mode(greenWaveLight)
    
    # 等待 10 秒钟模拟模式切换（你可以修改这个部分进行手动切换）
    time.sleep(10)

    # 停止当前模式
    stop_event.set()
    mode_thread.join()  # 等待当前模式线程退出
    print("模式已停止")
    
    # 你可以在此处切换到其他模式
    # 例如：start_light_mode(anotherModeFunction)
