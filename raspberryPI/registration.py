import requests

class DeviceRegistration:
    def __init__(self, api_base):
        self.api_base = api_base.replace("wss://", "https://")

    def check_registered(self, device_id):
        """检查设备是否已注册（正确解析服务器响应）"""
        try:
            resp = requests.post(
                f"{self.api_base}/api/device/check-registered",
                json={'deviceId': device_id},
                timeout=5
            )
            resp.raise_for_status()  # 检查HTTP错误
            resp_data = resp.json()
            print(f"[DEBUG] 注册检查响应: {resp.status_code}, 内容: {resp.text}")
            return resp_data.get('data', {}).get('isRegistered', False)
        except Exception as e:
            print(f"[ERROR] 注册检查失败: {str(e)}")
            return False

    def register_device(self, device_id, ip):
        """注册新设备（增加详细日志和错误处理）"""
        try:
            print(f"[DEBUG] 尝试注册设备: MAC={device_id}, IP={ip}")
            resp = requests.post(
                f"{self.api_base}/api/device/piregister",
                json={'mac': device_id, 'ip': ip},
                timeout=5
            )
            resp.raise_for_status()
            print(f"[DEBUG] 注册响应: {resp.status_code}, 内容: {resp.text}")
            
            # 验证注册是否真正成功
            resp_data = resp.json()
            return resp_data.get('code', 0) == 200 and 'data' in resp_data
        except Exception as e:
            print(f"[ERROR] 注册失败: {str(e)}")
            return False
