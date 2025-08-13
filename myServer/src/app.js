// 引入所需模块和依赖
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const path = require('path');
const fs = require('fs'); // 文件系统模块

// 引入路由模块
const authRouter = require('./routes/auth');
const userRouter = require('./routes/userRoutes');
const deviceRouter = require('./routes/device');
const commandRoutes = require('./routes/commandRoutes');

// 引入工具模块
const DeviceStatusMonitor = require('./utils/deviceStatusMonitor');

// 创建 Express 应用实例
const app = express();

// 使用 body-parser 中间件解析请求体
// app.use(bodyParser.json()); // 解析 application/json 格式的请求体
// app.use(bodyParser.urlencoded({ extended: true })); // 解析 URL 编码的表单数据

// 使用 CORS 中间件处理跨域请求
app.use(cors());

// 使用 express.json() 中间件解析 JSON 格式的请求体
app.use(express.json());

// 配置静态文件服务
const staticDir = path.join(__dirname, '../uploads');

// 提供 /static 路由访问上传的文件
app.use('/static', express.static(staticDir, {
    // 设置响应头，指定正确的 MIME 类型
    setHeaders: (res, filePath) => {
        const mimeTypes = { '.jpg': 'image/jpeg', '.png': 'image/png' };
        const ext = path.extname(filePath);
        if (mimeTypes[ext]) res.setHeader('Content-Type', mimeTypes[ext]);
    }
}));

// 调试路由：检查文件是否存在
app.get('/debug-file', (req, res) => {
    const physicalPath = path.join(staticDir, 'avatars/default.jpg');
    console.log('调试路径:', physicalPath);

    // 检查文件是否存在
    if (!fs.existsSync(physicalPath)) {
        const errorMsg = `文件不存在于: ${physicalPath}`;
        console.error(errorMsg);
        return res.status(500).send(errorMsg);
    }

    // 发送文件
    res.sendFile(physicalPath);
});

// 启动设备状态监控器
DeviceStatusMonitor.start();

// API 路由配置
app.use('/api/auth', authRouter);      // 认证相关路由
app.use('/api/user', userRouter);      // 用户相关路由
app.use('/api/device', deviceRouter);  // 设备相关路由
app.use('/api/commands', commandRoutes); // 命令相关路由

// 健康检查端点：检查服务器状态
app.get('/health', (req, res) => {
    res.json({
        status: 'UP',  // 服务状态
        staticDir: staticDir,  // 静态目录路径
        defaultAvatar: fs.existsSync(path.join(staticDir, 'avatars/default.jpg'))  // 检查默认头像是否存在
    });
});

// 首页路由：返回主页面
app.get('/', (req, res) => {
    res.sendFile(path.join(staticDir, 'pages/mainpage.html'));
});

// 导出应用实例供其他模块使用
module.exports = app;
