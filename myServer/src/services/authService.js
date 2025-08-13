require('dotenv').config();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const User = require('../models/User');
const VerificationCode = require('../models/VerificationCode'); // 添加这行
const path = require('path');
require('dotenv').config({ path: path.join(__dirname, '../.env') });
const { sendVerificationEmail } = require('../utils/emailSender');

/**
 * 生成6位随机验证码
 * @returns {string} 6位数字验证码
 */
const generateCode = () => Math.floor(100000 + Math.random() * 900000).toString();

/**
 * 认证服务类
 * 处理用户认证相关的业务逻辑，包括注册、登录、密码重置等
 */
class AuthService {

    /**
     * 检查账号是否存在
     * @static
     * @async
     * @param {string} email - 用户邮箱
     * @returns {Promise<boolean>} 账号是否存在
     * @throws {Error} 邮箱格式无效时抛出异常
     */
    static async checkAccountExists(email) {
        if (!/^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/.test(email)) {
            throw new Error('邮箱格式无效');
        }

        const user = await User.findOne({ email });
        return !!user; // 返回布尔值
    }

    /**
     * 发送邮件验证码（真实发送）
     * @static
     * @async
     * @param {string} email - 目标邮箱
     * @param {string} [type='register'] - 验证码类型 ('register'|'password_reset'|'email_change')
     * @returns {Promise<void>}
     * @throws {Error} 邮件发送失败时抛出异常
     */
    static async sendVerificationCode(email, type = 'register') {
        // 0. 邮箱格式校验
        if (!/^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/.test(email)) {
            throw new Error('邮箱格式不正确');
        }

        // 1. 生成并保存验证码
        const code = generateCode();
        const expiresAt = Date.now() + 300000; // 5分钟有效期

        await VerificationCode.findOneAndUpdate(
            { email, type },
            { code, expiresAt },
            { upsert: true }
        );

        // 2. 真实发送邮件
        try {
            await sendVerificationEmail(email, code, type);
        } catch (error) {
            console.error('邮件发送失败:', {
                error: error.response || error.message,
                stack: error.stack
            });
            throw new Error('验证码发送失败，请稍后重试');
        }
    }

    /**
     * 发送邮箱变更验证码
     * @static
     * @async
     * @param {string} email - 新邮箱地址
     * @returns {Promise<void>}
     * @throws {Error} 邮箱格式无效或已被注册时抛出异常
     */
    static async sendEmailChangeCode(email) {
        // 0. 邮箱格式校验
        if (!/^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/.test(email)) {
            throw new Error('邮箱格式不正确');
        }

        // 1. 检查新邮箱是否已被注册
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            throw new Error('该邮箱已被其他账号使用');
        }

        // 2. 发送验证码（使用特殊类型标识）
        return this.sendVerificationCode(email, 'email_change');
    }

    /**
     * 用户注册（改进版）
     * @static
     * @async
     * @param {string} email - 用户邮箱
     * @param {string} code - 验证码
     * @param {string} password - 用户密码
     * @returns {Promise<Object>} 注册结果
     * @returns {number} return.code - 状态码
     * @returns {string} return.message - 响应消息
     * @returns {Object|null} return.data - 响应数据
     */
    static async register(email, code, password) {
        try {
            console.log('\n[注册流程] 开始处理');
            console.log('输入参数:', { email, code, password: password ? '******' : '未提供' });

            // 0. 基础校验
            if (!/^\w+([.-]?\w+)*@\w+([.-]?\w+)*(\.\w{2,3})+$/.test(email)) {
                console.log('[验证失败] 邮箱格式无效');
                return { code: 400, message: '邮箱格式无效', data: null };
            }

            // 1. 检查用户是否存在
            console.log('[数据库查询] 检查用户是否存在...');
            const existingUser = await User.findOne({ email });
            console.log('用户查询结果:', existingUser ? '已存在' : '不存在');

            if (existingUser) {
                console.log('[拒绝注册] 邮箱已注册');
                return { code: 409, message: '该邮箱已注册', data: null };
            }

            // 2. 验证码校验
            console.log('[数据库查询] 验证验证码...');
            const codeRecord = await VerificationCode.findOne({ email });
            console.log('验证码记录:', codeRecord ?
                `code: ${codeRecord.code}, expiresAt: ${new Date(codeRecord.expiresAt).toLocaleString()}` :
                '未找到'
            );

            if (!codeRecord || codeRecord.code !== code) {
                console.log('[验证失败] 验证码不匹配');
                return { code: 400, message: '验证码不正确', data: null };
            }
            if (codeRecord.expiresAt < Date.now()) {
                console.log('[验证失败] 验证码已过期');
                return { code: 400, message: '验证码已过期', data: null };
            }

            // 3. 创建用户
            console.log('[处理中] 创建用户...');
            const hashedPassword = await bcrypt.hash(password, 10);
            const newUser = await User.create({ email, password: hashedPassword });
            console.log('用户创建成功, ID:', newUser._id);

            // 4. 清除验证码
            await VerificationCode.deleteOne({ email });
            console.log('验证码已清除');

            // 5. 返回结果
            const response = {
                code: 200,
                message: '注册成功',
                data: { userId: newUser._id, token: this._generateToken(newUser._id) }
            };
            //console.log('[注册完成] 返回响应:', response);
            return response;

        } catch (error) {
            console.error('[注册错误]', error.stack);
            return {
                code: 500,
                message: error.message || '注册失败',
                data: null
            };
        }
    }

    /**
     * 生成JWT令牌
     * @static
     * @param {mongoose.Types.ObjectId} userId - 用户ID
     * @returns {string} JWT令牌
     * @throws {Error} JWT密钥未配置时抛出异常
     */
    static _generateToken(userId) {
        // 临时使用固定密钥（仅限开发环境）
        const tempSecret = process.env.NODE_ENV === 'production'
            ? process.env.JWT_SECRET
            : 'development_temp_secret_123!';

        if (!tempSecret) {
            throw new Error('JWT密钥未配置');
        }

        console.warn('⚠️ 正在使用临时JWT密钥，生产环境必须配置JWT_SECRET');

        return jwt.sign(
            { userId },
            tempSecret,
            { expiresIn: '7d' }
        );
    }

    /**
     * 用户登录
     * @static
     * @async
     * @param {string} email - 用户邮箱
     * @param {string} password - 用户密码
     * @returns {Promise<Object>} 登录结果和用户信息
     * @returns {mongoose.Types.ObjectId} return.id - 用户ID
     * @returns {string} return.username - 用户名
     * @returns {string} return.email - 用户邮箱
     * @returns {string} return.phone - 用户手机号
     * @returns {string} return.avatar - 头像路径
     * @returns {string} return.token - JWT令牌
     * @throws {Error} 用户不存在或密码错误时抛出异常
     */
    static async login(email, password) {
        const user = await User.findOne({ email }).select('+password');
        if (!user) throw new Error('邮箱未注册');

        // 调试日志
        //console.log('数据库密码:', user.password);
        //console.log('输入密码:', password);

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            throw new Error('密码错误');
        }

        // 生成JWT令牌
        const token = jwt.sign(
            { userId: user._id },
            process.env.JWT_SECRET || 'development_secret',
            { expiresIn: '7d' }
        );

        return {
            id: user._id,
            username: user.username,
            email: user.email,
            phone: user.phone,
            avatar: user.avatar ? `static/${user.avatar}` : null,
            token
        };
    }

    /**
     * 发送密码重置验证码
     * @static
     * @async
     * @param {string} email - 用户邮箱
     * @returns {Promise<void>}
     */
    static async sendPasswordResetCode(email) {
        return this.sendVerificationCode(email, 'password_reset');
    }

    /**
     * 验证验证码有效性
     * @static
     * @async
     * @param {string} email - 用户邮箱
     * @param {string} code - 验证码
     * @returns {Promise<boolean>} 验证码是否有效
     */
    static async verifyCode(email, code) {
        console.log('\n=== 验证码服务层验证 ===');
        console.log('输入参数:', { email, code });
        let record = await VerificationCode.findOne({ email });

        console.log('验证码记录:', record ? {
            code: record.code,
            expiresAt: new Date(record.expiresAt).toLocaleString(),
            isExpired: record.expiresAt < Date.now()
        } : '无记录');

        if (!record || record.code !== code) {
            console.log('验证失败: 记录不存在或验证码不匹配');
            return false;
        }
        if (record.expiresAt < Date.now()) {
            console.log('验证失败: 验证码已过期');
            await VerificationCode.deleteOne({ _id: record._id });
            return false;
        }
        console.log('验证成功');
        return true;
    }

    /**
     * 重置用户密码
     * @static
     * @async
     * @param {string} email - 用户邮箱
     * @param {string} code - 验证码
     * @param {string} newPassword - 新密码
     * @returns {Promise<boolean>} 重置是否成功
     * @throws {Error} 账号不存在、验证码无效或密码相关错误时抛出异常
     */
    static async resetPassword(email, code, newPassword) {
        const existingUser = await User.findOne({ email });
        if (!existingUser) {
            throw new Error('账号不存在'); // 改为 throw
        }

        try {
            // 1. 验证验证码
            const isValid = await this.verifyCode(email, code, 'reset');
            if (!isValid) {
                throw new Error('验证码无效或已过期'); // 改为 throw
            }

            // 2. 获取用户
            const user = await User.findOne({ email }).select('+password');
            if (!user) {
                throw new Error('用户不存在'); // 改为 throw
            }

            // 3. 检查新旧密码是否相同
            if (!newPassword) {
                throw new Error('新密码不能为空'); // 改为 throw
            }

            const isSame = await bcrypt.compare(newPassword, user.password);
            if (isSame) {
                throw new Error('新密码不能与旧密码相同'); // 改为 throw
            }

            // 4. 更新密码并清理验证码
            user.password = await bcrypt.hash(newPassword, 10);
            await user.save();
            await VerificationCode.deleteOne({ email, type: 'password_reset' });

            return true;
        } catch (error) {
            console.error('密码重置过程中出错:', error);
            throw error; // 继续抛出错误
        }
    }
}

module.exports = AuthService;
