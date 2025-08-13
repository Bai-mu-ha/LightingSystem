import socket
import uuid
import time
import threading
import struct

class DeviceDiscovery:
    def __init__(self, device_name="智能灯光"):
        self.device_name = device_name
        self._running = False
        self._thread = None

    def get_mac_address(self):
        mac = uuid.getnode()
        return ':'.join(("%012X" % mac)[i:i+2] for i in range(0, 12, 2))

    def _respond_to_discovery(self):
        multicast_group = '224.0.0.114'
        server_port = 8888
        
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.settimeout(0.2)
            sock.bind(('', server_port))
            
            group = socket.inet_aton(multicast_group)
            mreq = struct.pack('4sL', group, socket.INADDR_ANY)
            sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)
            
            print("树莓派设备发现服务已启动...")
            while self._running:
                try:
                    data, address = sock.recvfrom(1024)
                    if data.decode() == "DISCOVER_DEVICES_REQUEST":
                        response = f"DISCOVER_DEVICES_RESPONSE|{self.device_name}|{self.get_mac_address()}"
                        sock.sendto(response.encode(), address)
                        print(f"已响应设备发现请求来自: {address}")
                except socket.timeout:
                    continue
                except Exception as e:
                    print(f"设备发现服务错误: {e}")
                    break

    def start(self):
        if not self._running:
            self._running = True
            self._thread = threading.Thread(target=self._respond_to_discovery, daemon=True)
            self._thread.start()

    def stop(self):
        self._running = False
        if self._thread:
            self._thread.join(timeout=1)
