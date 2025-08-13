// 引入所需模块
const https = require('https');
const fs = require('fs');
const mongoose = require('mongoose');
const { exec } = require('child_process'); // 用于执行系统命令

// 引入 Express 应用实例
const app = require('./app');

// 定义端口号和 SSL 证书目录
const PORT = process.env.PORT || 443;  // 默认使用 HTTPS 端口 443
const SSL_DIR = process.env.SSL_DIR || '../uphengbai77.xyz';  // SSL 证书目录

// 启动 cloudflared 隧道进程
const tunnelProcess = exec(
    `"D:\\code\\cloudflared\\cloudflared.exe" tunnel --config "D:\\code\\Java\\myServer\\cloudflared\\config.yml" run light_service`,
    { cwd: 'D:\\code\\Java\\myServer' } // 设置工作目录
);

// 监听 tunnelProcess 的标准输出
tunnelProcess.stdout.on('data', (data) => {
    console.log(`[cloudflared] ${data}`);
});

// 监听 tunnelProcess 的错误输出
tunnelProcess.stderr.on('data', (data) => {
    const log = data.toString().trim();

    if (log.includes(' ERR ') || log.includes(' FATAL')) {
        console.error(`[cloudflared ERROR] ${log}`);
    } else if (log.includes(' WRN ')) {
        console.warn(`[cloudflared WARNING] ${log}`);
    } else if (log.includes(' INF ')) {
        console.log(`[cloudflared INFO] ${log}`);
    } else {
        console.log(`[cloudflared OUTPUT] ${log}`);
    }
});

// 添加进程退出处理
tunnelProcess.on('exit', (code) => {
    if (code !== 0) {
        console.error(`[cloudflared] 进程异常退出，代码 ${code}`);
    } else {
        console.log('[cloudflared] 进程正常退出');
    }
});

// HTTPS 证书配置
const options = {
    key: fs.readFileSync(`${SSL_DIR}/uphengbai77.xyz.key`),
    cert: fs.readFileSync(`${SSL_DIR}/uphengbai77.xyz.pem`),
    ca: fs.readFileSync(`${SSL_DIR}/cert.pem`)  // 添加CA证书
};

// 连接 MongoDB 数据库
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/myapp', )
    .then(() => {
        // 创建 HTTPS 服务器
        const server = https.createServer(options, app);

        // 初始化 WebSocket 路由
        require('./routes/websocket')(server);

        // 启动服务器并监听指定端口
        server.listen(PORT, () => console.log(`服务运行在 https://localhost:${PORT}`));
    })
    .catch(err => {
        // 处理数据库连接失败的情况
        console.error('MongoDB 连接失败:', err);
        process.exit(1);  // 连接失败时退出程序
    });
