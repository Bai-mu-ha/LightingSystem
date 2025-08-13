import sys
import os
import board
import neopixel
import time
import threading
import colorsys

# --- 自动申请sudo权限 ---
if os.geteuid() != 0:
    print("正在自动请求sudo权限...")
    os.execvp("sudo", ["sudo", sys.executable] + sys.argv)

# LED 数量
NUM_PIXELS = 60

# 初始化灯带
pixels = neopixel.NeoPixel(board.D18, NUM_PIXELS, brightness=1.0, auto_write=False)

def hex_to_rgb(hex_color, brightness=1.0):
    """将 #FFFFFF 格式转为 (r,g,b) 元组，并应用亮度"""
    hex_color = hex_color.lstrip('#')
    r = int(hex_color[0:2], 16) * brightness
    g = int(hex_color[2:4], 16) * brightness
    b = int(hex_color[4:6], 16) * brightness
    return (int(r), int(g), int(b))

def constantColor(stop_event, color='#FFFFFF', brightness=1.0):
    """支持十六进制颜色和亮度控制的常亮模式"""
    rgb_color = hex_to_rgb(color, brightness)
    print(f"启动常亮模式 颜色:{color} 亮度:{brightness} => RGB:{rgb_color}")

    try:
        pixels.fill(rgb_color)
        pixels.show()

        while not stop_event.is_set():
            time.sleep(1)

    except KeyboardInterrupt:
        pixels.fill((0, 0, 0))
        pixels.show()

def light(color='#FFFFFF', brightness=1.0):  # 确保顺序正确
    """启动常亮模式的主入口函数"""
    stop_event, mode_thread = start_light_mode(constantColor, color=color, brightness=brightness)

    try:
        while True:  # 保持主线程运行
            time.sleep(1)
    except KeyboardInterrupt:
        stop_event.set()
        mode_thread.join()
        pixels.fill((0, 0, 0))
        pixels.show()

def start_light_mode(mode_function, color='#FFFFFF', brightness=1.0):
    stop_event = threading.Event()
    # 确保mode_function是可调用的函数
    assert callable(mode_function), "mode_function must be a callable function"
    mode_thread = threading.Thread(
        target=mode_function,
        args=(stop_event, color, brightness)  # 只传递一次color和brightness
    )
    mode_thread.start()
    return stop_event, mode_thread

if __name__ == "__main__":
    # 调用light函数，启动常亮模式
    light(color='#00FF00', brightness=0.8)  # 示例：绿色常亮模式
    
    # 你可以在此处切换到其他模式
    # 例如：light(anotherModeFunction) 
