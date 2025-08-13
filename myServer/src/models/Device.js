const mongoose = require('mongoose');

/**
 * 设备状态日志模型模式定义
 * 记录设备状态变化的历史信息
 */
const statusLogSchema = new mongoose.Schema({
    device: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Device',
        required: true
    },
    status: {
        type: String,
        enum: ['online', 'offline'],  // 移除bound状态
        required: true
    },
    isBound: Boolean,  // 新增绑定状态记录
    mode: String,
    modeParams: mongoose.Schema.Types.Mixed  // 新增模式参数记录
}, { timestamps: true });

/**
 * 设备状态日志模型
 * @typedef {Object} DeviceStatusLog
 * @property {mongoose.Types.ObjectId} device - 关联的设备ID
 * @property {string} status - 设备状态 ('online' | 'offline')
 * @property {boolean} isBound - 设备绑定状态
 * @property {string} mode - 设备模式
 * @property {Object} modeParams - 模式参数
 * @property {Date} createdAt - 创建时间
 * @property {Date} updatedAt - 更新时间
 */
module.exports = mongoose.model('DeviceStatusLog', statusLogSchema);
