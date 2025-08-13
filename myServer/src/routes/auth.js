const express = require('express');
const { body } = require('express-validator');
const AuthService = require('../services/authService');
const { formatResponse } = require('../utils/response');

const router = express.Router();

/**
 * 邮箱验证中间件
 * 验证邮箱格式并标准化
 */
const emailValidator = body('email').isEmail().normalizeEmail();

/**
 * 验证码验证中间件
 * 验证6位验证码格式
 */
const codeValidator = body('code').isLength({ min: 6, max: 6 });

/**
 * 密码验证中间件
 * 验证密码长度至少6位
 */
const passwordValidator = body('password').isLength({ min: 6 });

/**
 * 账号检查路由
 * @route POST /api/auth/check-account
 * @group Auth - 认证相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.email - 邮箱地址
 * @param {Object} res - 响应对象
 * @returns {Object} 账号是否存在状态
 */
router.post('/check-account', [emailValidator], async (req, res) => {
    try {
        console.log('\n=== 账号检查请求 ===');
        console.log('请求参数:', { email: req.body.email });
        const exists = await AuthService.checkAccountExists(req.body.email);
        const response = formatResponse(200, exists ? '账号已存在' : '账号不存在', exists);
        console.log('返回数据:', response);
        res.json(response);
    } catch (error) {
        console.error('账号检查失败:', error.message);
        res.status(400).json(formatResponse(400, error.message));
    }
});

/**
 * 发送验证码路由
 * @route POST /api/auth/send-code
 * @group Auth - 认证相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.email - 邮箱地址
 * @param {string} req.body.type - 验证码类型(register|password_reset)
 * @param {Object} res - 响应对象
 * @returns {Object} 发送结果
 */
router.post('/send-code', [emailValidator], async (req, res) => {
    try {
        console.log('\n=== 发送验证码请求 ===');
        const type = req.body.type || 'register';
        console.log('请求参数:', { email: req.body.email, type });
        await AuthService.sendVerificationCode(req.body.email, type);
        const response = formatResponse(200, '验证码已发送至邮箱');
        console.log('返回数据:', response);
        res.json(response);
    } catch (error) {
        console.error('发送验证码失败:', error.message);
        res.status(500).json(formatResponse(500, error.message));
    }
});

/**
 * 验证验证码路由
 * @route POST /api/auth/verify-code
 * @group Auth - 认证相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.email - 邮箱地址
 * @param {string} req.body.code - 6位验证码
 * @param {Object} res - 响应对象
 * @returns {Object} 验证码有效性
 */
router.post('/verify-code', [emailValidator, codeValidator], async (req, res) => {
    try {
        console.log('\n=== 验证验证码请求 ===');
        console.log('请求参数:', { email: req.body.email, code: req.body.code });
        const isValid = await AuthService.verifyCode(req.body.email, req.body.code);
        const response = formatResponse(200, isValid ? '验证码有效' : '验证码无效', isValid);
        console.log('返回数据:', response);
        res.json(response);
    } catch (error) {
        console.error('验证验证码失败:', error.message);
        res.status(400).json(formatResponse(400, error.message));
    }
});

/**
 * 发送邮箱变更验证码路由
 * @route POST /api/auth/send-email-change-code
 * @group Auth - 认证相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.email - 邮箱地址
 * @param {Object} res - 响应对象
 * @returns {Object} 发送结果
 */
router.post('/send-email-change-code', [emailValidator], async (req, res) => {
    try {
        console.log('\n=== 发送邮箱变更验证码请求 ===');
        console.log('请求参数:', { email: req.body.email });
        await AuthService.sendEmailChangeCode(req.body.email);
        const response = formatResponse(200, '验证码已发送至邮箱');
        console.log('返回数据:', response);
        res.json(response);
    } catch (error) {
        console.error('发送验证码失败:', error.message);
        res.status(500).json(formatResponse(500, error.message));
    }
});

/**
 * 用户注册路由
 * @route POST /api/auth/register
 * @group Auth - 认证相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.email - 邮箱地址
 * @param {string} req.body.code - 6位验证码
 * @param {string} req.body.password - 密码
 * @param {Object} res - 响应对象
 * @returns {Object} 注册结果
 */
router.post('/register', [
    emailValidator,
    codeValidator,
    passwordValidator
], async (req, res) => {
    try {
        console.log('接收到用户注册请求，请求体：', req.body); // 打印请求数据
        const result = await AuthService.register(
            req.body.email,
            req.body.code,
            req.body.password
        );
        console.log('返回的数据：', result); // 打印返回数据
        res.status(result.code).json(result);
    } catch (error) {
        console.error('注册失败：', error.message); // 打印错误信息
        res.status(500).json(formatResponse(500, error.message));
    }
});

/**
 * 用户登录路由
 * @route POST /api/auth/login
 * @group Auth - 认证相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.email - 邮箱地址
 * @param {string} req.body.password - 密码
 * @param {Object} res - 响应对象
 * @returns {Object} 登录结果和用户信息
 */
router.post('/login',
    [
        body('email').isEmail(),
        body('password').isLength({ min: 6 })
    ],
    async (req, res) => {
        try {
            //console.log('接收到用户登录请求，请求体：', req.body); // 打印请求体
            const user = await AuthService.login(req.body.email, req.body.password);
            const responseData = {
                token: user.token,
                userId: user.id,
                username: user.username,
                email: user.email,
                phone: user.phone,
                avatarUrl: user.avatar
            };
            //console.log('返回的数据：', responseData); // 打印返回的数据
            res.json(formatResponse(200, '登录成功', responseData));
        } catch (error) {
            console.error('登录失败：', error.message); // 打印错误信息
            res.status(401).json(formatResponse(401, error.message));
        }
    }
);

/**
 * 重置密码路由
 * @route POST /api/auth/reset-password
 * @group Auth - 认证相关接口
 * @param {Object} req - 请求对象
 * @param {Object} req.body - 请求体
 * @param {string} req.body.email - 邮箱地址
 * @param {string} req.body.code - 6位验证码
 * @param {string} req.body.newPassword - 新密码
 * @param {Object} res - 响应对象
 * @returns {Object} 重置结果
 */
router.post('/reset-password', [
    body('email').isEmail(),
    body('code').isLength({ min: 6, max: 6 }),
    body('newPassword').isLength({ min: 6 })
], async (req, res) => {
    try {
        await AuthService.resetPassword(
            req.body.email,
            req.body.code,
            req.body.newPassword
        );
        res.json(formatResponse(200, '密码重置成功'));
    } catch (error) {
        // 根据错误消息返回对应状态码
        const statusCode = error.message.includes('验证码') ||
        error.message.includes('密码') ||
        error.message.includes('账号') ? 409 : 400;
        res.status(statusCode).json(formatResponse(statusCode, error.message));
    }
});

module.exports = router;
