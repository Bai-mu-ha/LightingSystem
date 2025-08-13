const Device = require('../models/Device');
const UserDevice = require('../models/UserDevice');
//const WsService = require('./wsService');
//const DeviceStatusLog = require('../models/DeviceStatusLog');

/**
 * 设备服务类
 * 处理设备相关的业务逻辑，包括设备注册、绑定、状态更新等
 */
class DeviceService {
    /**
     * 检查并绑定设备到用户
     * @static
     * @async
     * @param {mongoose.Types.ObjectId} userId - 用户ID
     * @param {string} deviceId - 设备ID(MAC地址)
     * @returns {Promise<Object>} 绑定结果
     * @returns {boolean} return.isNew - 是否为新绑定
     * @returns {string} return.deviceId - 设备ID
     * @returns {mongoose.Types.ObjectId} return.userId - 用户ID
     * @throws {Error} 设备未注册或已被其他用户绑定时抛出异常
     */
    static async checkAndBindDevice(userId, deviceId) {
        const device = await Device.findOne({ deviceId });
        if (!device) throw new Error('设备未注册');

        const existingBinding = await UserDevice.findOne({ deviceId });
        if (existingBinding && !existingBinding.userId.equals(userId)) {
            throw new Error('设备已被其他用户绑定');
        }

        // 同步更新设备绑定状态
        await Device.updateOne(
            { deviceId },
            { isBound: true }
        );

        const result = await UserDevice.findOneAndUpdate(
            { deviceId },
            { userId },
            { upsert: true, new: true }
        );

        return {
            isNew: !existingBinding,
            deviceId,
            userId
        };
    }

    /**
     * 树莓派设备注册
     * @static
     * @async
     * @param {string} mac - 设备MAC地址
     * @param {string} ip - 设备IP地址
     * @returns {Promise<mongoose.Document>} 设备文档
     * @throws {Error} MAC地址格式无效时抛出异常
     */
    static async registerPiDevice(mac, ip) {
        if (!this._validateMac(mac)) throw new Error('无效的MAC地址格式');

        let device = await Device.findOne({ deviceId: mac });
        if (device) {
            return this._updateDevice(device, {
                ip:ip,
                status: 'online',
                mode: 'solid',
                modeParams: {
                    color: '#FFFFFF',
                    brightness: 1.0
                }
            });
        } else {
            return Device.create({
                deviceId: mac,
                ip:ip,
                status: 'online',
                mode: 'solid',
                modeParams: {
                    color: '#FFFFFF',
                    brightness: 1.0
                }
            });
        }
    }

    /**
     * 更新设备信息（供手机端调用）
     * @static
     * @async
     * @param {string} deviceId - 设备ID
     * @param {Object} updates - 更新数据
     * @param {string} [updates.mode] - 设备模式
     * @param {Object} [updates.params] - 模式参数
     * @param {string} [updates.status] - 设备状态
     * @returns {Promise<mongoose.Document>} 更新后的设备文档
     * @throws {Error} 设备未找到或命令发送失败时抛出异常
     */
    static async updateDevice(deviceId, updates) {
        console.log('[DEBUG] 服务层接收参数:', JSON.stringify(updates));

        // 特殊处理状态更新（不依赖模式参数）
        if ('status' in updates) {
            return this._updateDevice(
                { deviceId },
                { status: updates.status }
            );
        }

        // 模式更新需要转发到设备
        if (updates.mode) {
            const command = {
                type: 'MODE_UPDATE',
                data: {
                    mode: updates.mode,
                    params: updates.params || {}
                }
            };

            const WsService = require('./wsService');
            try {
                await WsService.sendCommandWithAck(deviceId, command);
            } catch (error) {
                console.error(`[ERROR] 设备 ${deviceId} 模式更新失败:`, error);
                throw error;
            }
        }

        const newModeParams = {};

        // 只保留当前模式需要的参数
        switch(updates.mode) {
            case 'solid':
                newModeParams.color = updates.params?.color || '#FFFFFF';
                newModeParams.brightness = updates.params?.brightness || 1.0;
                break;

            case 'rainbow':
                newModeParams.speed = updates.params?.speed || 0.5;
                newModeParams.tail_length = Math.round((updates.params?.tail_length || 0.2) * 20 + 5);
                break;

            case 'twinkle':
                newModeParams.color1 = updates.params?.color1 || '#FFFFFF';
                newModeParams.color2 = updates.params?.color2 || '#FFFF00';
                newModeParams.frequency = updates.params?.frequency || 0.5;
                newModeParams.speed = updates.params?.speed || 0.5;
                newModeParams.cycle_time = updates.params?.cycle_time || 2;
                break;

            case 'wave':
                newModeParams.color = updates.params?.color || '#FFFFFF';
                newModeParams.wave_speed = updates.params?.wave_speed || 0.5;
                newModeParams.wave_density = Math.round(updates.params?.wave_density || 3);
                newModeParams.brightness = updates.params?.brightness || 1.0;
                break;

            case 'flowing_rainbow':
                newModeParams.speed = updates.params?.speed || 0.5;
                newModeParams.tail_length = Math.round((updates.params?.tail_length || 0.2) * 20 + 5);
                newModeParams.brightness = updates.params?.brightness || 1.0;
                break;

            case 'breathing_rainbow':
                newModeParams.cycle_duration = updates.params?.cycle_duration || 3;
                newModeParams.steps = Math.round(updates.params?.steps || 60);
                newModeParams.spin_speed = updates.params?.spin_speed || 2;
                break;

            case 'ripple':
                newModeParams.color = updates.params?.color || '#FFFFFF';
                newModeParams.speed = updates.params?.speed || 0.5;
                newModeParams.wave_length = Math.round(updates.params?.wave_length || 5);
                break;

            default:
                break;
        }

        console.log('[DEBUG] 处理后参数:', JSON.stringify(newModeParams, null, 2));

        const existingDevice = await Device.findOne({ deviceId });
        if (!existingDevice) throw new Error('设备未找到');

        // 转换为普通对象并更新
        const deviceObj = existingDevice.toObject();
        const updatedDevice = {
            ...deviceObj,
            mode: updates.mode,
            modeParams: newModeParams
        };

        await Device.replaceOne(
            { deviceId },
            updatedDevice
        );

        // console.log('[DEBUG] 更新后设备状态:', JSON.stringify(updatedDevice?.modeParams, null, 2));
        //
        // // 记录状态变更
        // if (updates.mode || newModeParams.modeParams) {
        //     await DeviceStatusLog.create({
        //         device: updatedDevice._id,
        //         status: updatedDevice.status,
        //         mode: updatedDevice.mode,
        //         modeParams: updatedDevice.modeParams
        //     });
        // }

        return Device.findOne({ deviceId });
    }

    /**
     * 获取设备完整状态
     * @static
     * @async
     * @param {string} deviceId - 设备ID
     * @returns {Promise<Object>} 设备状态信息
     * @throws {Error} 设备未注册时抛出异常
     */
    static async getDeviceStatus(deviceId) {
        console.log('deviceId:', deviceId);
        const device = await Device.findOne({ deviceId })
            .select('-_id -__v -createdAt -updatedAt');

        if (!device) throw new Error('设备未注册');

        return {
            ...device.toObject(),
            isOnline: device.status === 'online'
        };
    }

    /**
     * 处理设备心跳
     * @static
     * @async
     * @param {string} deviceId - 设备ID
     * @param {string} ip - 设备IP地址
     * @returns {Promise<mongoose.Document>} 更新后的设备文档
     */
    static async handleHeartbeat(deviceId, ip) {
        const now = new Date();
        const MinutesAgo = new Date(now.getTime() - 60 * 1000);

        const device = await Device.findOne({ deviceId });

        // 如果设备存在且超过20秒未活跃，标记为需要更新状态
        if (device) {
            const lastSeen = device.lastSeen || now;
            if (lastSeen < MinutesAgo) {
                console.log(`[HEARTBEAT] 设备 ${deviceId} 恢复在线`);
            }
        }

        return this._updateDevice(
            { deviceId },
            {
                ip,
                status: 'online',
                lastSeen: now
            },
            {
                upsert: true,
                new: true,
                setDefaultsOnInsert: true  // 确保插入时应用默认值
            }
        );
    }

    /**
     * 更新设备信息（私有方法）
     * @static
     * @async
     * @private
     * @param {Object} query - 查询条件
     * @param {Object} updates - 更新数据
     * @param {Object} [options={}] - 更新选项
     * @returns {Promise<mongoose.Document>} 更新后的设备文档
     */
    static async _updateDevice(query, updates, options = {}) {
        return Device.findOneAndUpdate(
            query,
            updates,
            { new: true, ...options }
        );
    }

    /**
     * 验证MAC地址格式
     * @static
     * @private
     * @param {string} mac - MAC地址
     * @returns {boolean} 格式是否有效
     */
    static _validateMac(mac) {
        return /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/.test(mac);
    }

    /**
     * 发送命令到设备
     * @static
     * @async
     * @param {string} deviceId - 设备ID
     * @param {Object} command - 命令对象
     * @returns {Promise<mongoose.Document>} 更新后的设备文档
     */
    static async sendCommand(deviceId, command) {
        const device = await this.updateDevice(deviceId, command);
        const WsService = require('./wsService');
        await WsService.sendCommandWithAck(deviceId, command);
        return device;
    }
}

module.exports = DeviceService;
