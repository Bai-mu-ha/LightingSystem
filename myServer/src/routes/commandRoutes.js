const router = require('express').Router();
const WsService = require('../services/wsService');

/**
 * 设备颜色更新路由
 * @route POST /api/command/color
 * @group Command - 设备控制命令接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.deviceId - 设备ID
 * @param {string} req.body.color - 颜色值
 * @param {Object} res - 响应对象
 * @returns {Object} 命令执行结果
 */
router.post('/color', async (req, res) => {
    try {
        const device = await DeviceController.handleColorUpdate(
            req.body.deviceId,
            req.body.color
        );

        await WsService.sendCommandWithAck(device.deviceId, {
            type: 'COLOR_UPDATE',
            color: device.color
        });

        res.json({ success: true });
    } catch (error) {
        res.status(503).json({
            success: false,
            error: error.message
        });
    }
});

module.exports = router;
