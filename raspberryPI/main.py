from network import DeviceNetwork
from registration import DeviceRegistration
from discoveryDevice import DeviceDiscovery
from binding import DeviceBinding
from Light import light
import time
import signal
import sys
import threading  # 新增导入
import board  # 新增导入（用于关灯操作）
import neopixel  # 新增导入（用于关灯操作）

class SmartLightDevice:
    def __init__(self, server_url):
        self.server_url = server_url
        self.network = DeviceNetwork(server_url)
        self.registration = DeviceRegistration(server_url)
        self.binding = DeviceBinding(server_url)
        self.discovery = DeviceDiscovery()
        self.device_id = self.network.device_id
        self._setup_signal_handlers()

        self.light_thread = threading.Thread(
            target=light,
            kwargs={'color': '#FFFFFF', 'brightness': 1.0},
            daemon=True
        )
        self.light_thread.start()
        print(f"[INIT] 设备初始化完成，ID: {self.device_id}")

    def _setup_signal_handlers(self):
        """设置信号处理程序"""
        signal.signal(signal.SIGINT, self._handle_exit)
        signal.signal(signal.SIGTERM, self._handle_exit)

    def _handle_exit(self, signum, frame):
        """处理退出信号"""
        print(f"\n[SHUTDOWN] 收到终止信号({signum})，执行清理...")
        self._turn_off_light()
        self.discovery.stop()
        self.network.close()

        # 新增：停止常亮模式线程
        if hasattr(self, 'light_thread') and self.light_thread.is_alive():
            self.light_thread.join(timeout=1)  # 等待线程结束

        print("[SHUTDOWN] 清理完成，退出程序")
        sys.exit(0)

    def _turn_off_light(self):
        """发送关灯命令"""
        try:
            print("[CLEANUP] 正在关闭灯光...")
            # 这里需要根据你的实际硬件接口实现关灯逻辑
#            self.network.light.turn_off()
            # 示例：通过HTTP请求发送关闭命令
#            requests.post(
#                f"{self.server_url}/api/device/command",
#                json={
#                    'deviceId': self.device_id,
#                    'command': 'turn_off'
#                },
#                timeout=3
#            )
            print("[CLEANUP] 灯光已关闭")
        except Exception as e:
            print(f"[ERROR] 关闭灯光失败: {str(e)}")

    def start(self):
        print("[START] 启动设备流程...")

        try:
            # 1. 检查注册状态
            if not self._check_registered():
                print("[REG] 设备未注册，尝试自动注册...")
                if not self._register_device():
                    print("[ERROR] 注册失败，终止启动")
                    return False
            print("[REG] 设备注册验证通过")

            # 2. 检查绑定状态
            if not self._check_binding():
                print("[BIND] 设备未绑定，启动发现服务...")
                self.discovery.start()
                while not self._check_binding():
                    print("[BIND] 等待绑定完成...")
                    time.sleep(5)
                self.discovery.stop()
                print("[BIND] 设备绑定成功")

            # 3. 主连接循环
            print("[MAIN] 进入主连接循环")
            self._main_loop()
            
        except Exception as e:
            print(f"[FATAL] 发生未捕获异常: {str(e)}")
            self._handle_exit(signal.SIGTERM, None)
            return False
            
        return True

    def _check_registered(self):
        print("[CHECK] 检查注册状态...")
        registered = self.registration.check_registered(self.device_id)
        print(f"[CHECK] 注册状态结果: {registered}")
        return registered

    def _register_device(self):
        ip = self.network._get_local_ip()
        print(f"[REG] 获取本地IP: {ip}")
        return self.registration.register_device(self.device_id, ip)

    def _check_binding(self):
        print("[BIND] 检查绑定状态...")
        bound = self.binding.check_binding(self.device_id)
        print(f"[BIND] 绑定状态结果: {bound}")
        return bound

    def _main_loop(self):
        while True:
            if not self.network.connect():
                print("[NET] 连接服务器失败，5秒后重试...")
                time.sleep(5)
                continue
                
            try:
                print("[NET] 连接成功，保持活跃状态...")
                while True:
                    time.sleep(1)
            except ConnectionError as e:
                self.network.close()
                print(f"[NET] 连接异常: {str(e)}，尝试重新连接...")

if __name__ == "__main__":
    print("=== 智能灯设备启动 ===")
    device = SmartLightDevice("wss://uphengbai77.xyz")
    if not device.start():
        print("!!! 设备启动失败 !!!")
