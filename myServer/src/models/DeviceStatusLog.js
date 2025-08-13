const mongoose = require('mongoose');

const statusLogSchema = new mongoose.Schema({
    device: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Device',
        required: true
    },
    status: {
        type: String,
        enum: ['online', 'offline'],  // 移除bound状态
        required: true
    },
    isBound: Boolean,  // 新增绑定状态记录
    mode: String,
    modeParams: mongoose.Schema.Types.Mixed  // 新增模式参数记录
}, { timestamps: true });

module.exports = mongoose.model('DeviceStatusLog', statusLogSchema);