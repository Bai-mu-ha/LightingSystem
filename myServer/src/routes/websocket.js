const WebSocket = require('ws');
const wsService = require('../services/wsService');

/**
 * WebSocket路由配置模块
 * 负责处理设备WebSocket连接的建立和管理
 * @param {http.Server} server - HTTP服务器实例
 * @returns {WebSocket.Server} WebSocket服务器实例
 */
module.exports = (server) => {
    /**
     * 创建WebSocket服务器实例
     * 监听/ws路径的连接请求
     */
    const wss = new WebSocket.Server({
        server,
        path: '/ws'
    });

    /**
     * WebSocket连接事件处理
     * 验证设备ID并委托给wsService处理连接
     */
    wss.on('connection', (ws, req) => {
        try {
            // 从URL查询参数中提取设备ID
            const deviceId = new URL(req.url, `http://${req.headers.host}`)
                .searchParams.get('deviceId');

            if (!deviceId) {
                throw new Error('缺少设备ID');
            }

            // 委托给wsService处理连接逻辑
            wsService.handleConnection(ws, deviceId);
        } catch (error) {
            console.error('WebSocket连接错误:', error.message);
            ws.close(1008, error.message);
        }
    });

    return wss;
};
