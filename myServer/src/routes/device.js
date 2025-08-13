const express = require('express');
const router = express.Router();
const DeviceService = require('../services/deviceService');
const WsService = require('../services/wsService');
const mongoose = require('mongoose');
const Device = require('../models/Device');
const UserDevice = require('../models/UserDevice')

const debugTag = '[DeviceRouter]';

/**
 * 异步错误处理中间件包装器
 * 统一处理异步路由中的错误
 * @param {Function} fn - 异步路由处理函数
 * @returns {Function} 包装后的处理函数
 */
const asyncHandler = (fn) => (req, res, next) =>
    Promise.resolve(fn(req, res, next)).catch(next);

/**
 * 检查设备是否已注册路由
 * @route POST /api/device/check-registered
 * @group Device - 设备相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.deviceId - 设备ID(MAC地址)
 * @param {Object} res - 响应对象
 * @returns {Object} 设备注册和绑定状态
 */
router.post('/check-registered', asyncHandler(async (req, res) => {
    const { deviceId } = req.body; // 从请求体中获取
    console.log("check-registered-deviceId:", deviceId)

    const device = await Device.findOne({ deviceId });

    //console.log("check-registered-device:", device)
    res.json({
        code: 200,
        data: {
            isRegistered: !!device,
            isBound: device?.isBound || false  // 直接使用Device模型的isBound字段
        },
        message: "success"
    });
}));

/**
 * 树莓派设备注册路由
 * @route POST /api/device/piregister
 * @group Device - 设备相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.mac - 设备MAC地址
 * @param {string} req.body.ip - 设备IP地址
 * @param {Object} res - 响应对象
 * @returns {Object} 设备注册结果
 */
router.post('/piregister', asyncHandler(async (req, res) => {
    const { mac, ip } = req.body;
    res.json({
        code: 200,
        data: await DeviceService.registerPiDevice(mac, ip)
    });
}));

/**
 * 绑定设备到用户路由
 * @route POST /api/device/bind
 * @group Device - 设备相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.userId - 用户ID
 * @param {string} req.body.deviceId - 设备ID(MAC地址)
 * @param {Object} res - 响应对象
 * @returns {Object} 绑定结果
 */
router.post('/bind', asyncHandler(async (req, res) => {
    const { userId, deviceId } = req.body;
    console.log("[DeviceRouter] 绑定请求接收", { userId, deviceId });

    const result = await DeviceService.checkAndBindDevice(userId, deviceId);

    console.log("[DeviceRouter] 绑定结果返回", {
        deviceId: result.deviceId,
        isNew: result.isNew,
        isBound: true  // 确保返回绑定状态
    });

    res.json({
        code: 200,
        data: {  // 确保data是对象
            isBound: true,
            isNew: result.isNew
        },
        message: result.isNew ? '绑定成功' : '设备已绑定'
    });
}));

/**
 * 获取用户设备列表路由
 * @route GET /api/device/list
 * @group Device - 设备相关接口
 * @param {Object} req - 请求对象
 * @param {string} req.query.userId - 用户ID
 * @param {Object} res - 响应对象
 * @returns {Object} 用户设备列表
 */
router.get('/list', asyncHandler(async (req, res) => {
    const { userId } = req.query;
    //console.log("[DEBUG] 接收到的 userId:", userId);

    const devices = await UserDevice.find({ userId })
        .populate({
            path: 'device',
            select: '-_id deviceId name status mode color lastSeen'
        });

    // 打印原始查询结果
    //console.log("[DEBUG] 数据库原始数据:", JSON.stringify(devices, null, 2));

    // 构造响应数据
    const responseData = devices.map(d => ({
        ...d.device.toObject(),
        isOnline: d.device.status === 'online'
    }));

    // 打印最终返回结构
    // console.log("[DEBUG] 返回移动端的数据结构:", {
    //     code: 200,
    //     data: responseData
    // });

    res.json({
        code: 200,
        data: responseData
    });
}));

/**
 * 更新设备状态路由
 * @route PATCH /api/device/:deviceId
 * @group Device - 设备相关接口
 * @param {Object} req - 请求对象
 * @param {string} req.params.deviceId - 设备ID
 * @param {Object} req.body - 更新的数据
 * @param {Object} res - 响应对象
 * @returns {Object} 更新后的设备信息
 */
router.patch('/:deviceId', asyncHandler(async (req, res) => {
    const device = await DeviceService.updateDevice(
        req.params.deviceId,
        req.body
    );

    res.json({
        code: 200,
        data: device
    });
}));

/**
 * 更新设备模式路由
 * @route PATCH /api/device/:deviceId/mode
 * @group Device - 设备相关接口
 * @param {Object} req - 请求对象
 * @param {string} req.params.deviceId - 设备ID
 * @param {Object} req.body - 模式更新数据
 * @param {string} req.body.mode - 模式名称
 * @param {Object} req.body.params - 模式参数
 * @param {Object} res - 响应对象
 * @returns {Object} 更新后的设备信息
 */
router.patch('/:deviceId/mode', asyncHandler(async (req, res) => {
    //console.log(`${debugTag} 原始请求体:`, JSON.stringify(req.body, null, 2));
    //console.log(`${debugTag} 请求头:`, req.headers['content-type']);

    if (!req.body.mode) {
        const errorMsg = 'mode字段不能为空';
        console.error(`${debugTag} 参数验证失败`, errorMsg);
        return res.status(400).json({ code: 400, message: errorMsg });
    }

    try {
        // 统一参数格式
        const updates = {
            mode: req.body.mode,
            params: req.body.params || req.body.modeParams || {}
        };
        //console.log(`${debugTag} 处理后参数:`, JSON.stringify(updates, null, 2));

        const device = await DeviceService.updateDevice(req.params.deviceId, updates);

        console.log(`${debugTag} 数据库更新结果:`, {
            deviceId: device.deviceId,
            newMode: device.mode,
            modeParams: device.modeParams
        });

        res.json({
            code: 200,
            data: device
        });
    } catch (error) {
        console.error(`${debugTag} 模式修改失败`, {
            error: error.message,
            stack: error.stack,
            rawBody: req.rawBody // 需要body-parser配置
        });
        res.status(500).json({
            code: 500,
            message: '服务器内部错误'
        });
    }
}));

module.exports = router;
