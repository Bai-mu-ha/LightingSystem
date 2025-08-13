const jwt = require('jsonwebtoken');
const User = require('../models/User');

/**
 * 身份验证中间件
 * 验证请求中的JWT令牌并附加用户信息到请求对象
 * @async
 * @param {express.Request} req - Express请求对象
 * @param {express.Response} res - Express响应对象
 * @param {express.NextFunction} next - Express下一步函数
 * @returns {void}
 */
const checkAuth = async (req, res, next) => {
    try {
        // 从Header获取token
        const token = req.headers.authorization?.split(' ')[1];
        if (!token) {
            return res.status(401).json({ error: '未提供认证令牌' });
        }

        // 验证token
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'development_secret');
        const user = await User.findById(decoded.userId);

        if (!user) {
            return res.status(401).json({ error: '用户不存在' });
        }

        // 将用户信息附加到请求对象
        req.user = {
            id: user._id,
            email: user.email
        };
        next();
    } catch (error) {
        res.status(401).json({ error: '认证失败', details: error.message });
    }
};

module.exports = { checkAuth };
