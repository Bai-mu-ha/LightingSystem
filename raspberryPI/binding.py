import requests
import time

class DeviceBinding:
    def __init__(self, api_base):
        self.api_base = api_base.replace("wss://", "https://")


    def check_binding(self, device_id):
        """检查设备绑定状态"""
        try:
            resp = requests.post(
                f"{self.api_base}/api/device/check-registered",
                json={'deviceId': device_id},
                timeout = 3
            )
            print(f"绑定检查响应: {resp.status_code}, {resp.text}")

            if resp.status_code == 200:
                return resp.json().get('data', {}).get('isBound', False)
            return False
        except Exception as e:
            print(f"绑定检查失败: {e}")
            return False
