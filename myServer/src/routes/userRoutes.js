const express = require('express');
const multer = require('multer');
const { body } = require('express-validator');
const User = require('../models/User');
const UserService = require('../services/userService');
const { checkAuth } = require('../services/checkAuth');
const VerificationCode = require("../models/VerificationCode");
const bcrypt = require('bcryptjs');

const router = express.Router();
const upload = multer({
    dest: '../uploads/',
    limits: { fileSize: 5 * 1024 * 1024 }
});

/**
 * 统一API响应格式化函数
 * @param {number} code - HTTP状态码
 * @param {string} message - 响应消息
 * @param {any} [data=null] - 响应数据
 * @returns {Object} 标准化响应对象
 */
const formatResponse = (code, message, data = null) => ({
    code,
    message,
    data
});

/**
 * 更新用户名路由
 * @route PATCH /api/user/me/username
 * @group User - 用户相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.value - 新用户名
 * @param {Object} res - 响应对象
 * @returns {Object} 更新后的用户信息
 */
router.patch('/me/username', checkAuth, express.json(), async (req, res) => {
    try {
        const { value: username } = req.body;
        if (!username || username.trim().length < 2) {
            return res.status(400).json(formatResponse(400, '用户名至少2个字符'));
        }

        const user = await User.findByIdAndUpdate(
            req.user.id,
            { username: username.trim() },
            { new: true }
        ).select('-password');

        res.json(formatResponse(200, '更新成功', {
            token: req.headers.authorization?.split(' ')[1],
            userId: user._id,
            username: user.username
        }));
    } catch (error) {
        res.status(500).json(formatResponse(500, error.message));
    }
});

/**
 * 更新用户头像路由
 * @route PATCH /api/user/me/avatar
 * @group User - 用户相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.file - 上传的头像文件
 * @param {Object} res - 响应对象
 * @returns {Object} 更新后的头像信息
 */
router.patch('/me/avatar', checkAuth, upload.single('avatar'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json(formatResponse(400, '请选择头像文件'));
        }

        const avatarResult = await UserService.updateAvatar(req.user.id, req.file);
        const user = await User.findByIdAndUpdate(
            req.user.id,
            { avatar: avatarResult.relativePath },
            { new: true }
        ).select('-password');

        res.json(formatResponse(200, '头像更新成功', {
            token: req.headers.authorization?.split(' ')[1],
            userId: user._id,
            avatarUrl: `${req.protocol}://${req.get('host')}/static/${user.avatar}`
        }));
    } catch (error) {
        res.status(400).json(formatResponse(400, error.message));
    }
});

/**
 * 更新用户邮箱路由
 * @route PATCH /api/user/email
 * @group User - 用户相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.value - 新邮箱地址
 * @param {Object} res - 响应对象
 * @returns {Object} 更新后的邮箱信息
 */
router.patch('/email', checkAuth, express.json(), async (req, res) => {
    try {
        const { value: newEmail } = req.body;
        if (!/^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/.test(newEmail)) {
            return res.status(400).json(formatResponse(400, '邮箱格式无效'));
        }

        const exists = await User.exists({ email: newEmail });
        if (exists) return res.status(409).json(formatResponse(409, '该邮箱已被注册'));

        const user = await User.findByIdAndUpdate(
            req.user.id,
            { email: newEmail },
            { new: true }
        ).select('-password');

        res.json(formatResponse(200, '邮箱更新成功', {
            token: req.headers.authorization?.split(' ')[1],
            userId: user._id,
            email: user.email
        }));
    } catch (error) {
        res.status(500).json(formatResponse(500, error.message));
    }
});

/**
 * 验证用户密码路由
 * @route POST /api/user/verify-password
 * @group User - 用户相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.value - 待验证的密码
 * @param {Object} res - 响应对象
 * @returns {Object} 密码验证结果
 */
router.post('/verify-password', checkAuth, express.json(), async (req, res) => {
    try {
        const { value: password } = req.body;
        const user = await User.findById(req.user.id).select('+password');

        const isMatch = await bcrypt.compare(password, user.password);
        res.json(formatResponse(200, '验证成功', isMatch));
    } catch (error) {
        res.status(500).json(formatResponse(500, error.message));
    }
});

/**
 * 更新用户密码路由
 * @route PATCH /api/user/password
 * @group User - 用户相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.value - 新密码
 * @param {Object} res - 响应对象
 * @returns {Object} 密码更新结果
 */
router.patch('/password', checkAuth, express.json(), async (req, res) => {
    try {
        const { value: newPassword } = req.body;
        const hashedPassword = await bcrypt.hash(newPassword, 10);

        await User.findByIdAndUpdate(req.user.id, { password: hashedPassword });
        res.json(formatResponse(200, '密码修改成功'));
    } catch (error) {
        res.status(500).json(formatResponse(500, error.message));
    }
});

module.exports = router;
