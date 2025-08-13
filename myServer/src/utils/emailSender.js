const nodemailer = require('nodemailer');
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

// 生产级邮件配置
const transporter = nodemailer.createTransport({
    host: process.env.EMAIL_HOST,
    port: process.env.EMAIL_PORT,
    secure: true, // TLS
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS
    },
    tls: {
        rejectUnauthorized: false
    },
    pool: true, // 启用连接池
    maxConnections: 5,
    maxMessages: 100
});

/**
 * 真实发送验证码邮件（强制发送，无开发模式）
 * @param {string} to - 收件邮箱
 * @param {string} code - 6位验证码
 * @param {string} [type='register'] - 邮件类型，'register'表示注册验证，其他值表示密码重置
 * @throws {Error} 发送失败时抛出异常
 * @returns {Promise<Object>} 邮件发送结果信息
 */
const sendVerificationEmail = async (to, code, type = 'register') => {
    const mailOptions = {
        from: `"智能家居系统" <${process.env.EMAIL_USER}>`,
        to,
        subject: type === 'register' ? '【注册验证】您的验证码' : '【密码重置】验证码',
        html: `
            <div style="font-family: Arial, sans-serif; max-width: 600px;">
                <h2 style="color: #1890ff;">${type === 'register' ? '账号注册' : '密码重置'}</h2>
                <p>您的验证码为：<strong style="font-size: 18px;">${code}</strong></p>
                <p style="color: #ff4d4f;">有效期5分钟，请勿泄露</p>
                <hr style="border-color: #f0f0f0;">
                <p style="font-size: 12px; color: #999;">系统自动发送，请勿回复</p>
            </div>
        `
    };

    // 强制发送并等待结果
    console.log('当前SMTP配置:', {
        host: process.env.EMAIL_HOST, // 应输出 smtp.zoho.com
        port: process.env.EMAIL_PORT,
        user: process.env.EMAIL_USER
    });
    const info = await transporter.sendMail(mailOptions);
    console.log(`[邮件发送] 消息ID: ${info.messageId} 收件人: ${to}`);
    return info;
};

module.exports = { sendVerificationEmail };
