let DeviceService;
setImmediate(() => {
    DeviceService = require('./deviceService');
});

/**
 * WebSocket服务类
 * 管理设备WebSocket连接，处理设备与服务器之间的实时通信
 */
class WsService {
    /**
     * 创建WebSocket服务实例
     */
    constructor() {
        /** @type {Map<string, WebSocket>} 设备连接映射表 */
        this.clients = new Map();
        this.startHeartbeatCheck();
    }

    /**
     * 处理设备连接
     * @param {WebSocket} ws - WebSocket连接对象
     * @param {string} deviceId - 设备ID(MAC地址)
     * @returns {void}
     */
    handleConnection(ws, deviceId) {
        this.clients.set(deviceId, ws);
        console.log(`已连接上树莓派 ${deviceId}`);

        // 心跳检测
        ws.isAlive = true;
        ws.on('pong', () => ws.isAlive = true);

        // 消息处理
        ws.on('message', async (data) => {
            try {
                ws.isAlive = true;
                const msg = JSON.parse(data.toString());
                console.log(`[MSG] 收到设备 ${deviceId} 消息:`, msg);

                // 新增模式更新确认处理
                if (msg.type === 'MODE_UPDATE_ACK') {
                    console.log(`设备 ${deviceId} 确认模式更新成功`);
                    return;
                }

                if (msg.type === 'INITIAL_STATUS') {
                    await DeviceService.registerPiDevice(
                        deviceId,
                        msg.data.ip,
                        msg.data.mode
                    );
                } else if (msg.type === 'HEARTBEAT') {
                    //console.log(`接收到树莓派 ${deviceId} 的心跳请求`);
                    console.log(`[HEARTBEAT] 处理心跳`, msg.data);
                    await DeviceService.handleHeartbeat(deviceId, msg.data?.ip);
                }
            } catch (error) {
                console.error(`处理消息失败:`, error);
            }
        });

        // 清理连接
        ws.on('close', () => this.handleDisconnect(deviceId));
        ws.on('error', (err) => this.handleError(deviceId, err));
    }

    /**
     * 处理设备断开连接
     * @param {string} deviceId - 设备ID
     * @returns {void}
     */
    handleDisconnect(deviceId) {
        this.clients.delete(deviceId);
        console.log(`设备断开: ${deviceId}`);
        DeviceService.updateDevice(deviceId, {
            status: 'offline',
            lastSeen: new Date()  // 增加最后离线时间标记
        }).catch(e => console.error(`状态更新失败:`, e));
    }

    /**
     * 处理设备连接错误
     * @param {string} deviceId - 设备ID
     * @param {Error} error - 错误对象
     * @returns {void}
     */
    handleError(deviceId, error) {
        console.error(`设备 ${deviceId} 错误:`, error);
        this.handleDisconnect(deviceId);
    }

    /**
     * 启动心跳检测
     * @param {number} [interval=10000] - 检测间隔(毫秒)
     * @returns {void}
     */
    startHeartbeatCheck(interval = 10000) {
        setInterval(() => {
            this.clients.forEach((ws, deviceId) => {
                if (!ws.isAlive) return ws.terminate();
                ws.isAlive = false;
                ws.ping();
            });
        }, interval);
    }

    /**
     * 发送命令并等待确认
     * @async
     * @param {string} deviceId - 设备ID
     * @param {Object} command - 命令对象
     * @param {string} command.type - 命令类型
     * @param {Object} command.data - 命令数据
     * @param {number} [timeout=5000] - 超时时间(毫秒)
     * @returns {Promise<any>} 命令执行结果
     * @throws {Error} 设备离线或超时时抛出异常
     */
    async sendCommandWithAck(deviceId, command, timeout = 5000) {
        console.log(`[DEBUG] 准备向设备 ${deviceId} 发送命令:`, JSON.stringify(command));
        const ws = this.clients.get(deviceId);
        if (!ws) {
            console.error(`[ERROR] 设备 ${deviceId} 离线，无法发送命令`);
            throw new Error('DEVICE_OFFLINE');
        }

        return new Promise((resolve, reject) => {
            //console.log(`[DEBUG] 设置 ${timeout}ms 超时监听ACK响应`);
            const timer = setTimeout(() => {
                console.error(`[TIMEOUT] 设备 ${deviceId} 未在 ${timeout}ms 内响应`);
                reject(new Error('TIMEOUT'));
            }, timeout);

            const listener = (data) => {
                console.log(`[DEBUG] 收到设备 ${deviceId} 的响应:`, data.toString());
                const msg = JSON.parse(data);
                if (msg.type === `${command.type}_ACK`) {
                    console.log(`[SUCCESS] 设备 ${deviceId} 处理成功:`, msg.data);
                    clearTimeout(timer);
                    ws.removeListener('message', listener);
                    resolve(msg.data);
                }
            };

            ws.on('message', listener);
            ws.send(JSON.stringify(command));
            console.log(`[DEBUG] 命令已发送至设备 ${deviceId}`);
        });
    }
}

// 导出单例实例
module.exports = new WsService();
