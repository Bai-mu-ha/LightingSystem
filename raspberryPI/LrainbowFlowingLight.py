import sys
import os
import board
import neopixel
import time
import threading

# --- 自动申请sudo权限 ---
if os.geteuid() != 0:
    print("正在自动请求sudo权限...")
    os.execvp("sudo", ["sudo", sys.executable] + sys.argv)

NUM_PIXELS = 60
pixels = neopixel.NeoPixel(board.D18, NUM_PIXELS, brightness=1.0, auto_write=False)

# 生成彩虹色的一个函数
def wheel(pos):
    """输入0~255，输出彩虹颜色"""
    pos = pos % 256
    if pos < 85:
        return (255 - pos * 3, pos * 3, 0)
    elif pos < 170:
        pos -= 85
        return (0, 255 - pos * 3, pos * 3)
    else:
        pos -= 170
        return (pos * 3, 0, 255 - pos * 3)

# 彩虹流动效果
def rainbowFlowingLight(stop_event, speed=0.5, tail_length=9, brightness=1.0):
    """彩虹流动效果
    speed: 0-1 值越大流动越快
    tail_length: 拖尾长度 (1-20)
    brightness: 整体亮度 (0-1)
    """
    print(f"启动彩虹流动 speed={speed:.1f} tail={tail_length} brightness={brightness:.1f}")

    # 确保tail_length为整数
    tail_length = int(round(tail_length))

    # 参数转换
    delay_time = 0.05 * (1 - speed)  # speed=1最快(0s), speed=0最慢(0.05s)
    color_step = max(1, int(10 * speed))  # 控制颜色变化步长

    try:
        offset = 0
        while not stop_event.is_set():
            # 清空灯带
            pixels.fill((0, 0, 0))

            # 绘制拖尾
            for i in range(tail_length):
                pos = (offset - i) % NUM_PIXELS
                pixel_brightness = brightness * (1 - (i / tail_length))
                color = wheel((pos * 256 // NUM_PIXELS) & 255)
                pixels[pos] = tuple(int(c * pixel_brightness) for c in color)

            pixels.show()
            offset = (offset + color_step) % NUM_PIXELS  # 用color_step来增加偏移
            time.sleep(max(0.001, delay_time))

    except KeyboardInterrupt:
        print("\n退出中，关闭灯光...")
        pixels.fill((0, 0, 0))
        pixels.show()

# 启动模式的线程
def start_light_mode(mode_function, *args, **kwargs):
    stop_event = threading.Event()  # 创建停止事件
    mode_thread = threading.Thread(target=mode_function, args=(stop_event, *args), kwargs=kwargs)
    mode_thread.start()
    return stop_event, mode_thread

if __name__ == "__main__":
    stop_event, mode_thread = start_light_mode(rainbowFlowingLight, speed=0.5, tail_length=0.2, brightness=1.0)

    # 模拟等待 10 秒后切换模式（你可以根据需求修改时间）
    time.sleep(10)

    # 停止当前模式
    stop_event.set()
    mode_thread.join()  # 等待当前模式线程退出
    print("彩虹流动灯模式已停止")

    # 切换到另一个模式
    # 例如：start_light_mode(anotherModeFunction)
