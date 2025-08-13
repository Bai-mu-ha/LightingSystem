const mongoose = require('mongoose');

/**
 * 用户设备关联模型模式定义
 * 建立用户与设备之间的多对多关系
 */
const userDeviceSchema = new mongoose.Schema({
    userId: {
        type: mongoose.Schema.Types.ObjectId,  // 用户ID保持ObjectId类型
        ref: 'User',
        required: true
    },
    deviceId: {
        type: String,  // 改为String类型以存储MAC地址
        required: true
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

// 添加虚拟字段关联Device模型（解决populate问题）
/**
 * 设备虚拟字段
 * 通过deviceId关联到Device模型，实现设备信息的自动填充
 */
userDeviceSchema.virtual('device', {
    ref: 'Device',
    localField: 'deviceId',
    foreignField: 'deviceId',
    justOne: true
});

// 启用虚拟字段的JSON转换
userDeviceSchema.set('toJSON', { virtuals: true });

/**
 * 用户设备关联模型
 * @typedef {Object} UserDevice
 * @property {mongoose.Types.ObjectId} userId - 用户ID
 * @property {string} deviceId - 设备ID(MAC地址)
 * @property {Date} createdAt - 关联创建时间
 * @property {Device} device - 关联的设备信息（虚拟字段）
 */
module.exports = mongoose.model('UserDevice', userDeviceSchema);
