const fs = require('fs');
const path = require('path');
const User = require('../models/User');

/**
 * 用户服务类
 * 提供用户相关的业务逻辑处理
 */
class UserService {
    /**
     * 更新用户头像
     * @static
     * @async
     * @param {mongoose.Types.ObjectId} userId - 用户ID
     * @param {Object} file - 上传的文件对象
     * @param {string} file.originalname - 原始文件名
     * @param {string} file.mimetype - 文件MIME类型
     * @param {string} file.path - 临时文件路径
     * @returns {Promise<Object>} 头像文件信息
     * @returns {string} return.relativePath - 相对路径
     * @returns {string} return.absolutePath - 绝对路径
     * @throws {Error} 文件类型不支持或文件操作失败时抛出异常
     */
    static async updateAvatar(userId, file) {
        if (!file) throw new Error('请选择头像文件');

        // 验证文件类型
        const allowedTypes = ['image/jpeg', 'image/png'];
        const allowedExts = ['.jpg', '.jpeg', '.png'];

        const fileExt = path.extname(file.originalname).toLowerCase();
        if (!allowedTypes.includes(file.mimetype) || !allowedExts.includes(fileExt)) {
            throw new Error(`仅支持 ${allowedTypes.join('/')} 格式`);
        }

        // 建议添加目录存在检查
        const tmpDir = path.join(__dirname, '../../uploads/avatars');
        if (!fs.existsSync(tmpDir)) {
            fs.mkdirSync(tmpDir, { recursive: true });
        }

        // 创建用户目录
        const userDir = path.join(__dirname, '../../uploads/avatars', userId.toString());
        if (!fs.existsSync(userDir)) {
            fs.mkdirSync(userDir, { recursive: true });
        }

        // 生成唯一文件名
        const ext = path.extname(file.originalname).toLowerCase();
        const filename = `avatar_${Date.now()}${ext}`;
        const filePath = path.join(userDir, filename);
        console.log('实际保存路径:', filePath);

        // 保存新头像
        await fs.promises.rename(file.path, filePath);

        // 在服务器终端打印日志
        console.log('\n=== 头像上传成功 ===');
        console.log('用户ID:', userId);
        console.log('存储路径:', path.relative(process.cwd(), filePath));

        return {
            relativePath: `avatars/${userId}/${filename}`,
            absolutePath: filePath
        };
    }
}

module.exports = UserService;
