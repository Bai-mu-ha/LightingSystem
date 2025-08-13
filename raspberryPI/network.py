import json
import socket
import uuid
import time
import threading
from websockets.sync.client import connect
import os
import sys
import board
import neopixel

# 将文件路径添加到系统路径中
sys.path.append('/home/cxk/code')

# 引入模式代码
from LrainbowMarquee import rainbowMarquee
from LstarTwinklingLight import starTwinklingLight
from LWaveLight import WaveLight
from LrainbowFlowingLight import rainbowFlowingLight
from LrainbowBreathingLight import rainbowBreathingLight
from LwaterRippleLamp import waterRippleLamp
from Light import light  # 常亮模式

class DeviceNetwork:
    def __init__(self, server_url):
        self.server_url = server_url
        self.ws = None
        self._running = False
        self.device_id = self._get_mac_address()
        self.current_mode_thread = None
        self.stop_event = threading.Event()
        self.current_mode = None

    def _get_mac_address(self):
        mac = uuid.getnode()
        return ':'.join(("%012X" % mac)[i:i+2] for i in range(0, 12, 2))

    def connect(self):
        try:
            self.ws = connect(f"{self.server_url}/ws?deviceId={self.device_id}")
            self._running = True

            threading.Thread(target=self._heartbeat_loop, daemon=True).start()
            threading.Thread(target=self._message_listener, daemon=True).start()

            self.send_initial_status({
                'mode': 'solid',
                'ip': self._get_local_ip()
            })

            return True
        except Exception as e:
            print(f"[ERROR] 连接服务器失败: {e}")
            return False

    def _message_listener(self):
        while self._running:
            try:
                message = self.ws.recv()
                self.on_message(message)
            except Exception as e:
                print(f"[ERROR] 消息接收异常: {e}")
                self._running = False

    def _heartbeat_loop(self):
        while self._running:
            try:
                self.send_heartbeat()
                time.sleep(30)
            except Exception as e:
                print(f"[ERROR] 心跳发送异常: {e}")
                self._running = False

    def send_heartbeat(self):
        ip = self._get_local_ip()
        self._send_message({
            'type': 'HEARTBEAT',
            'data': {'ip': ip}
        })

    def send_initial_status(self, mode_params):
        self._send_message({
            'type': 'INITIAL_STATUS',
            'data': mode_params
        })

    def _send_message(self, message):
        if self.ws:
            print(f"[DEBUG] 准备发送消息: {message}")
            try:
                self.ws.send(json.dumps(message))
                print("[DEBUG] 消息发送成功")
            except Exception as e:
                print(f"[ERROR] 消息发送失败: {e}")

    def on_message(self, message):
        print(f"[DEBUG] 收到服务器消息: {message}")
        try:
            msg = json.loads(message)
            if msg.get('type') == 'MODE_UPDATE':
                mode = msg.get('data', {}).get('mode')
                params = msg.get('data', {}).get('params', {})

                if mode:
                    self._stop_current_mode()

                    self.current_mode = mode
                    self.stop_event.clear()

                    self.current_mode_thread = threading.Thread(
                        target=self._run_mode,
                        args=(mode, params),
                        daemon=True
                    )
                    self.current_mode_thread.start()

                    self._send_message({
                        'type': 'MODE_UPDATE_ACK',
                        'data': {'success': True}
                    })
                else:
                    print("[ERROR] 未指定模式")
            else:
                print(f"[ERROR] 不支持的消息类型: {msg.get('type')}")
        except Exception as e:
            print(f"[ERROR] 消息处理异常: {e}")

    def _run_mode(self, mode, params={}):
        try:
            if mode == 'rainbow':
                print("[COMMAND] 启动彩虹跑马模式")
                tail_length = params.get('tail_length', 9) * 20 + 5  # 换算 tail_length
                rainbowMarquee(
                    self.stop_event,
                    speed=params.get('speed', 0.5),
                    tail_length=tail_length
                )
            elif mode == 'twinkle':
                print("[COMMAND] 启动星星闪烁模式")
                starTwinklingLight(
                    self.stop_event,
                    color1=params.get('color1', '#FFFFFF'),
                    color2=params.get('color2', '#FFFF00'),
                    frequency=1 - params.get('frequency', 0.1),
                    speed=params.get('speed', 0.1),
                    cycle_time=params.get('cycle_time', 3)
                )
            elif mode == 'wave':
                print("[COMMAND] 启动波动模式")
                WaveLight(
                    self.stop_event,
                    color=params.get('color', '#FFFFFF'),
                    wave_speed=params.get('wave_speed', 0.5),
                    wave_density=params.get('wave_density', 3),
                    brightness=params.get('brightness', 1.0)
                )
            elif mode == 'flowing_rainbow':
                print("[COMMAND] 启动彩虹流动模式")
                tail_length = params.get('tail_length', 9) * 600 + 5  # 换算 tail_length
                rainbowFlowingLight(
                    self.stop_event,
                    speed=params.get('speed', 0.5),
                    tail_length=tail_length,
                    brightness=params.get('brightness', 1.0)
                )
            elif mode == 'breathing_rainbow':
                print("[COMMAND] 启动彩虹呼吸效果")
                rainbowBreathingLight(
                    self.stop_event,
                    cycle_duration=params.get('cycle_duration', 3),
                    steps=params.get('steps', 60),
                    spin_speed=params.get('spin_speed', 2)
                )
            elif mode == 'ripple':
                print("[COMMAND] 启动水波纹效果")
                waterRippleLamp(
                    self.stop_event,
                    color=params.get('color', '#FFFFFF'),
                    speed=params.get('speed', 0.5),
                    wave_length=params.get('wave_length', 5)
                )
            elif mode == 'solid':
                print("[COMMAND] 启动常亮模式")
                # 只传递color和brightness，避免重复传递brightness
                light(
                    color=params.get('color', '#FFFFFF'),
                    brightness=params.get('brightness', 1.0)
                )
            else:
                print(f"[ERROR] 未知模式: {mode}")
        except Exception as e:
            print(f"[ERROR] 模式执行异常: {e}")

    def _stop_current_mode(self):
        if self.current_mode_thread and self.current_mode_thread.is_alive():
            print("[DEBUG] 停止当前模式线程")
            self.stop_event.set()
            self.current_mode_thread.join(timeout=2)
            print("[DEBUG] 当前模式线程已停止")

        try:
            pixels = neopixel.NeoPixel(board.D18, 60)
            pixels.fill((0, 0, 0))
            pixels.show()
            print("[DEBUG] 灯带已清空")
        except Exception as e:
            print(f"[ERROR] 灯带清空失败: {e}")

    def _get_local_ip(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect(('8.8.8.8', 80))
            return s.getsockname()[0]
        finally:
            s.close()

    def close(self):
        self._running = False
        self._stop_current_mode()
        if self.ws:
            self.ws.close()
