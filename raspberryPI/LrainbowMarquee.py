import sys
import os
import board
import neopixel
import time
import colorsys
import threading

# --- 自动申请sudo权限 ---
if os.geteuid() != 0:
    print("正在自动请求sudo权限...")
    os.execvp("sudo", ["sudo", sys.executable] + sys.argv)

# LED 数量
NUM_PIXELS = 60

# 初始化灯带
pixels = neopixel.NeoPixel(board.D18, NUM_PIXELS, brightness=0.5, auto_write=False)

def hsv_to_rgb(h, s, v):
    """将 HSV 转为 RGB，输出为 0-255 的整数元组"""
    r, g, b = colorsys.hsv_to_rgb(h, s, v)
    return (int(r * 255), int(g * 255), int(b * 255))

def rainbowMarquee(stop_event, speed=0.5, tail_length=9):
    """彩虹拖尾跑马灯
    speed: 0-1 浮点数，越大变化越快
    tail_length: 拖尾长度
    """
    # 处理tail_length必须是整数，且至少为1
    tail_length = max(1, int(tail_length))
    print(f"启动彩虹拖尾跑马灯 speed={speed:.2f} tail_length={tail_length}")

    # 将0-1的速度值转换为实际的延迟时间（0.1-0.001秒）
    delay_time = 0.1 * (1 - speed) + 0.001

    hue_offset = 0  # 用于彩虹变化

    try:
        while not stop_event.is_set():
            # 从左到右
            for head in range(NUM_PIXELS + tail_length):
                if stop_event.is_set():
                    break
                pixels.fill((0, 0, 0))  # 清空
                for i in range(tail_length):
                    pos = head - i
                    if 0 <= pos < NUM_PIXELS:
                        brightness = 1.0 - (i / tail_length)  # 越靠后越暗
                        hue = ((pos + hue_offset) % 360) / 360.0  # 彩虹色循环
                        color = hsv_to_rgb(hue, 1.0, brightness)
                        pixels[pos] = color
                pixels.show()
                time.sleep(delay_time)
                hue_offset = (hue_offset + 2) % 360  # 彩虹色慢慢滚动

            # 从右到左
            for head in reversed(range(NUM_PIXELS + tail_length)):
                if stop_event.is_set():
                    break
                pixels.fill((0, 0, 0))
                for i in range(tail_length):
                    pos = head - i
                    if 0 <= pos < NUM_PIXELS:
                        brightness = 1.0 - (i / tail_length)
                        hue = ((pos + hue_offset) % 360) / 360.0
                        color = hsv_to_rgb(hue, 1.0, brightness)
                        pixels[pos] = color
                pixels.show()
                time.sleep(delay_time)
                hue_offset = (hue_offset + 2) % 360

    except KeyboardInterrupt:
        print("\n中断，关闭灯光")
        pixels.fill((0, 0, 0))
        pixels.show()
    finally:
        # 退出时确保灯带熄灭
        pixels.fill((0, 0, 0))
        pixels.show()

# 启动模式的线程
def start_light_mode(mode_function, *args):
    stop_event = threading.Event()  # 创建停止事件
    mode_thread = threading.Thread(target=mode_function, args=(stop_event, *args))
    mode_thread.start()
    return stop_event, mode_thread

if __name__ == "__main__":
    stop_event, mode_thread = start_light_mode(rainbowMarquee)

    # 模拟等待 10 秒后切换模式（你可以根据需求修改时间）
    time.sleep(10)

    # 停止当前模式
    stop_event.set()
    mode_thread.join()  # 等待当前模式线程退出
    print("彩虹拖尾跑马灯模式已停止")
