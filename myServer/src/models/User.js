const mongoose = require('mongoose');

/**
 * 用户模型模式定义
 * 存储用户基本信息和认证信息
 */
const UserSchema = new mongoose.Schema({
    username: {
        type: String,
        trim: true,
        minlength: 2,
        maxlength: 20,
        default: () => `用户${Math.random().toString(36).substring(2, 8)}`
    },
    email: {
        type: String,
        required: true,
        unique: true,
        match: /^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/
    },
    password: {
        type: String,
        required: true,
        select: false // 默认不返回密码字段
    },
    deviceToken: { type: String }, // 用于移动端推送
    lastLogin: { type: Date },     // 最后登录时间
    phone: {
        type: String,
        required: false, // 改为非必填
        default: '',
        match: /^[0-9]{10,15}$/
    },
    avatar: {
        type: String,
        default: 'avatars/default.jpg'
    }
}, {
    timestamps: true,
    toJSON: {
        virtuals: true,
        transform: (doc, ret) => {
            delete ret.password;
            return ret;
        }
    }
});

/**
 * 用户模型
 * @typedef {Object} User
 * @property {string} username - 用户名
 * @property {string} email - 邮箱地址
 * @property {string} password - 加密后的密码
 * @property {string} deviceToken - 移动端推送令牌
 * @property {Date} lastLogin - 最后登录时间
 * @property {string} phone - 手机号码
 * @property {string} avatar - 头像路径
 * @property {Date} createdAt - 创建时间
 * @property {Date} updatedAt - 更新时间
 */
module.exports = mongoose.model('User', UserSchema);
