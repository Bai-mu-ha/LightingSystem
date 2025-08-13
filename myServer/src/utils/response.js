/**
 * 统一API响应格式工具
 * 用于创建标准化的API响应对象，确保响应格式的一致性
 * @param {number} code - HTTP状态码
 * @param {string} message - 响应消息
 * @param {any} [data=null] - 响应数据
 * @returns {Object} 标准化响应对象
 * @returns {number} return.code - HTTP状态码
 * @returns {string} return.message - 响应消息
 * @returns {any} return.data - 响应数据
 */
const formatResponse = (code, message, data = null) => ({
    code,
    message,
    data: data !== undefined ? data : null  // 明确处理undefined情况
});

module.exports = { formatResponse };
