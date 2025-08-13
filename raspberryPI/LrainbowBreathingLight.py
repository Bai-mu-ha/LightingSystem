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

# 生成彩虹色的一个函数
def wheel(pos):
    """输入0~255，输出彩虹颜色"""
    if pos < 85:
        return (255 - pos * 3, pos * 3, 0)
    elif pos < 170:
        pos -= 85
        return (0, 255 - pos * 3, pos * 3)
    else:
        pos -= 170
        return (pos * 3, 0, 255 - pos * 3)

# 彩虹流动+呼吸效果
def rainbowBreathingLight(stop_event, cycle_duration=3, steps=60, spin_speed=2):
    """彩虹流动+呼吸效果"""
    print("启动动态彩虹呼吸灯...")

    steps = steps + 100
    offset = 0  # 控制颜色流动偏移量

    try:
        while not stop_event.is_set():
            for i in range(steps):
                if stop_event.is_set():
                    break

                # 呼吸亮度曲线（0~1）
                brightness = (math.sin(math.pi * i / steps)) ** 2
                for j in range(NUM_PIXELS):
                    # 加上偏移产生旋转/流动效果
                    pos = (j * 256 // NUM_PIXELS + offset) % 256
                    color = wheel(pos)
                    # 应用亮度
                    r = min(255, max(0, int(color[0] * brightness)))
                    g = min(255, max(0, int(color[1] * brightness)))
                    b = min(255, max(0, int(color[2] * brightness)))
                    pixels[j] = (r, g, b)
                pixels.show()
                time.sleep(cycle_duration / steps)
                offset = (offset + spin_speed) % 256  # 增加偏移，实现颜色流动
    except Exception as e:
        print(f"[ERROR] 彩虹灯异常: {e}")
    except KeyboardInterrupt:
        print("\n退出中，关闭灯光...")
        pixels.fill((0, 0, 0))
        pixels.show()
    finally:
        pixels.fill((0, 0, 0))
        pixels.show()


# 启动模式的线程
def start_light_mode(mode_function, *args):
    stop_event = threading.Event()  # 创建停止事件
    mode_thread = threading.Thread(target=mode_function, args=(stop_event, *args))
    mode_thread.start()
    return stop_event, mode_thread

if __name__ == "__main__":
    stop_event, mode_thread = start_light_mode(rainbowBreathingLight, 4, 140, 2)

    # 模拟等待 10 秒后切换模式（你可以根据需求修改时间）
    time.sleep(10)

    # 停止当前模式
    stop_event.set()
    mode_thread.join()  # 等待当前模式线程退出
    print("彩虹流动+呼吸模式已停止")

    # 切换到另一个模式
    # 例如：start_light_mode(anotherModeFunction)
