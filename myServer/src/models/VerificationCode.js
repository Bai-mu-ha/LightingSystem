const mongoose = require('mongoose');

/**
 * 验证码模型模式定义
 * 用于存储邮箱验证码信息，包括注册和密码重置两种类型
 */
const verificationCodeSchema = new mongoose.Schema({
    email: {
        type: String,
        required: true,
        unique: true
    },
    code: {
        type: String,
        required: true
    },
    type: {
        type: String,
        enum: ['register', 'password_reset'],
        default: 'register'
    },
    expiresAt: {
        type: Date,
        required: true
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

// 自动删除过期验证码（每小时清理一次）
verificationCodeSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

/**
 * 验证码模型
 * @typedef {Object} VerificationCode
 * @property {string} email - 用户邮箱地址
 * @property {string} code - 6位验证码
 * @property {string} type - 验证码类型 ('register' | 'password_reset')
 * @property {Date} expiresAt - 过期时间
 * @property {Date} createdAt - 创建时间
 */
module.exports = mongoose.model('VerificationCode', verificationCodeSchema);
