const Device = require('../models/Device');
const cron = require('node-cron');

/**
 * 设备状态监控器类
 * 定期检查设备在线状态，将超时未响应的设备标记为离线
 */
class DeviceStatusMonitor {
    /**
     * 启动设备状态监控定时任务
     * 每分钟执行一次检查，将超过1分钟未活动的设备设为离线状态
     * @static
     * @returns {void}
     */
    static start() {
        // 每分钟检查一次设备状态
        cron.schedule('* * * * *', async () => {
            const MinutesAgo = new Date(Date.now() - 60 * 1000);

            //console.log(`[${new Date().toISOString()}] 检测到${result.modifiedCount}台设备离线`);
            const result = await Device.updateMany(
                {
                    lastSeen: { $lt: MinutesAgo },
                    status: { $ne: 'offline' }
                },
                {
                    status: 'offline',
                    $unset: { ip: 1 }
                }
            );

            if (result.modifiedCount > 0) {
                console.log(`[${new Date().toISOString()}] 检测到${result.modifiedCount}台设备离线`);
            }
        });
    }
}

module.exports = DeviceStatusMonitor;
